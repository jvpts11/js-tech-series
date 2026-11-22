/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.client.moving.SeatLookClient;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.SeatLook;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.region.Box;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Walking in a moving structure whose floor pulls towards itself, on the player's own game: a floor with a step of
 * slabs at its far end, pitched half way up, rolled on its side and turned upside down, and in each the player stands
 * on it, walks along it up onto the step, jumps and comes down on it again, the server taking where they stand. On
 * the floor rolled on its side the view rolls with it, the look is turned and carried in its frame, and a lever on it
 * is pulled where it is seen.
 */
public final class DeckWalkClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /* The floor's corners, high enough over the ground to be turned any way; the step of slabs on its far end. */
    private static final BlockPos CORNER = new BlockPos(-1, 12, -11);
    private static final BlockPos OTHER_CORNER = new BlockPos(1, 13, -3);
    private static final int STEP_FROM_Z = -11;
    private static final int STEP_TO_Z = -8;
    private static final int WAIT = 200;
    private static final int SETTLE = 20;
    /*
     * How long forward is held, in ticks: from the floor's near end up onto the step, some six blocks at a walk, and
     * short of its far end at eight.
     */
    private static final int WALK = 30;
    /* How far along the floor the player must walk, onto the step, and how near its top they stand, in blocks. */
    private static final double WALKED = 4.8;
    private static final double ON = 0.05;
    private static final double SLAB = 0.5;
    /* How far apart the player's game and the server may have them, in blocks. */
    private static final double AGREE = 0.5;
    /* A lever on the floor, by its near end; how far the view must roll on a floor on its side, in degrees. */
    private static final BlockPos LEVER = new BlockPos(0, 13, -5);
    private static final float ROLLED = 45;
    /* How far the mouse turns the look across, in its steps; how near the floor's own plane the look keeps. */
    private static final double MOUSE_ACROSS = 200;
    private static final double ALONG = 0.02;
    /* How much further the floor is turned, in radians, while the look is carried with it. */
    private static final double FURTHER = 0.4;
    /* Pitched half way up about the east-west line, rolled on its side, and upside down. */
    private static final List<Turn> TURNS = List.of(new Turn("pitched", 1, 0, 0, Math.PI / 4),
            new Turn("rolled", 0, 0, 1, Math.PI / 2), new Turn("upside down", 0, 0, 1, Math.PI));

    private DeckWalkClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void deck_isWalkedOnHoweverItIsTurned(final ClientTestContext ctx) {
        final AtomicReference<MovingStructure> made = new AtomicReference<>();
        final AtomicReference<Vec3> server = new AtomicReference<>(Vec3.ZERO);
        final double[] highest = new double[1];
        ctx.afterTest(() -> {
            ctx.mc().options.keyUp.setDown(false);
            ctx.mc().options.keyJump.setDown(false);
            ctx.server().execute(() -> takeAll(ctx.serverLevel()));
        });
        ctx.thenServer(0, level -> {
            takeAll(level);
            for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CORNER), ctx.abs(OTHER_CORNER.below()))) {
                level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
            }
            for (int x = CORNER.getX(); x <= OTHER_CORNER.getX(); x++) {
                for (int z = STEP_FROM_Z; z <= STEP_TO_Z; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, OTHER_CORNER.getY(), z)),
                            Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
            }
            final MovingStructure floor = MovingStructures.assemble(level, ctx.abs(CORNER), ctx.abs(OTHER_CORNER))
                    .structure();
            floor.deckGravity(true);
            made.set(floor);
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> seen() != null && seen().ready() && seen().deckGravity(), WAIT,
                        "the floor to reach the player's game, pulling towards itself");
        for (final Turn turn : TURNS) {
            ctx.thenServer(0, level -> {
                final StructurePose pose = made.get().pose();
                made.get().teleport(StructurePose.turned(pose.x(), pose.y(), pose.z(), turn.ax(), turn.ay(), turn.az(),
                        turn.angle()));
            })
                    .thenWaitUntil(() -> seen() != null && seen().frame().pose().angleTo(made.get().pose()) < 1.0e-3,
                            WAIT, "the floor " + turn.name() + " on the player's game")
                    .thenServer(0, level -> {
                        // Sent by the server, on the floor's near end, looking along it: their game, setting them
                        // there itself, would be sent back as having moved too far at once.
                        final MovingStructure floor = made.get();
                        final Box box = floor.bounds();
                        final StructureFrame frame = floor.frame();
                        final double x = (box.minX() + box.maxX() + 1) / 2.0;
                        final double y = box.minY() + 1 + ON;
                        final double z = box.maxZ() + 0.5;
                        final Vec3 ahead = new Vec3(frame.turnX(0, 0, -1), frame.turnY(0, 0, -1),
                                frame.turnZ(0, 0, -1));
                        ctx.serverPlayer().teleportTo(level, frame.worldX(x, y, z), frame.worldY(x, y, z),
                                frame.worldZ(x, y, z), (float) Math.toDegrees(Math.atan2(-ahead.x, ahead.z)),
                                (float) Math.toDegrees(Math.asin(-ahead.y)));
                        ctx.serverPlayer().setDeltaMovement(Vec3.ZERO);
                    })
                    .thenWaitUntil(() -> StructureFooting.standingOn(ctx.player()) == seen() && ctx.player().onGround(),
                            WAIT, "the player to stand on the floor " + turn.name(), () -> where(ctx))
                    .then(SETTLE, () -> { })
                    .thenWaitUntil(() -> Math.abs(yard(ctx).y - (seen().bounds().minY() + 1)) < ON, 1,
                            "the player to stay on the floor " + turn.name() + ", its floor down", () -> where(ctx))
                    .then(0, () -> ctx.mc().options.keyUp.setDown(true))
                    .then(WALK, () -> ctx.mc().options.keyUp.setDown(false))
                    .then(SETTLE, () -> { })
                    .thenWaitUntil(() -> yard(ctx).z < seen().bounds().maxZ() + 0.5 - WALKED
                                    && Math.abs(yard(ctx).y - (seen().bounds().minY() + 1 + SLAB)) < ON, 1,
                            "the player to have walked along the floor " + turn.name() + " and up onto its step",
                            () -> where(ctx))
                    .thenServer(0, level -> server.set(ctx.serverPlayer().position()))
                    .thenWaitUntil(() -> server.get().distanceTo(ctx.player().position()) < AGREE, 1,
                            "the server to have the player where their game has them",
                            () -> server.get() + " against " + ctx.player().position())
                    .then(0, () -> {
                        highest[0] = yard(ctx).y;
                        ctx.mc().options.keyJump.setDown(true);
                    })
                    .then(2, () -> ctx.mc().options.keyJump.setDown(false))
                    .thenWaitUntil(() -> {
                        highest[0] = Math.max(highest[0], yard(ctx).y);
                        return highest[0] > seen().bounds().minY() + 1 + SLAB + 1;
                    }, SETTLE, "the player to jump off the step " + turn.name() + ", away from its floor",
                            () -> where(ctx))
                    .thenWaitUntil(() -> ctx.player().onGround()
                                    && Math.abs(yard(ctx).y - (seen().bounds().minY() + 1 + SLAB)) < ON, WAIT,
                            "the player to come down on the step " + turn.name() + " again", () -> where(ctx));
        }
    }

    /*
     * Rolled on its side, the floor's up stays at the top of the view; the mouse turns the look about the floor's up,
     * along it; the floor turning further carries the look round with it; a lever on the floor, looked at, is pulled;
     * the player stands, not crawls, on both sides; and seen from behind, they are drawn standing on the floor.
     */
    @ClientTest(timeoutTicks = 1500)
    public static void look_isKeptInTheDecksFrameAndAimsWhereItIsSeen(final ClientTestContext ctx) {
        final AtomicReference<MovingStructure> made = new AtomicReference<>();
        final AtomicReference<BlockPos> lever = new AtomicReference<>();
        final float[] viewRoll = new float[1];
        final Vec3[] kept = new Vec3[1];
        final AtomicReference<Pose> serverPose = new AtomicReference<>();
        final DrawnUp drawn = new DrawnUp();
        final Vec3[] drawnUp = drawn.up;
        final Consumer<ViewportEvent.ComputeCameraAngles> angles = event -> viewRoll[0] = event.getRoll();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ViewportEvent.ComputeCameraAngles.class, angles);
        NeoForge.EVENT_BUS.register(drawn);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(angles);
            NeoForge.EVENT_BUS.unregister(drawn);
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.mc().options.keyUse.setDown(false);
            ctx.server().execute(() -> takeAll(ctx.serverLevel()));
        });
        ctx.thenServer(0, level -> {
            takeAll(level);
            for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CORNER), ctx.abs(OTHER_CORNER.below()))) {
                level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
            }
            level.setBlockAndUpdate(ctx.abs(LEVER), Blocks.LEVER.defaultBlockState()
                    .setValue(LeverBlock.FACE, AttachFace.FLOOR));
            final MovingStructure floor = MovingStructures.assemble(level, ctx.abs(CORNER), ctx.abs(OTHER_CORNER))
                    .structure();
            floor.deckGravity(true);
            final StructurePose pose = floor.pose();
            floor.teleport(StructurePose.turned(pose.x(), pose.y(), pose.z(), 0, 0, 1, Math.PI / 2));
            made.set(floor);
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> seen() != null && seen().ready() && seen().frame().pose()
                        .angleTo(made.get().pose()) < 1.0e-3, WAIT, "the floor, rolled, to reach the player's game")
                .thenServer(0, level -> {
                    final Box box = made.get().bounds();
                    final StructureFrame frame = made.get().frame();
                    final double x = (box.minX() + box.maxX() + 1) / 2.0;
                    final double y = box.minY() + 1 + ON;
                    final double z = box.maxZ() + 0.5;
                    ctx.serverPlayer().teleportTo(level, frame.worldX(x, y, z), frame.worldY(x, y, z),
                            frame.worldZ(x, y, z), 0, 0);
                    // Its blocks in its plot of the yard by now: the lever is found among them.
                    for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                            box.maxY(), box.maxZ())) {
                        if (level.getBlockState(at).is(Blocks.LEVER)) {
                            lever.set(at.immutable());
                        }
                    }
                })
                .thenWaitUntil(() -> StructureFooting.standingOn(ctx.player()) == seen() && ctx.player().onGround(),
                        WAIT, "the player to stand on the rolled floor", () -> where(ctx))
                .then(0, () -> {
                    // Looking along the floor, away from its near end, in its own frame.
                    final StructureFrame frame = seen().frame();
                    final Vec3 ahead = new Vec3(frame.turnX(0, 0, -1), frame.turnY(0, 0, -1), frame.turnZ(0, 0, -1));
                    ctx.player().setYRot((float) Math.toDegrees(Math.atan2(-ahead.x, ahead.z)));
                    ctx.player().setXRot((float) Math.toDegrees(Math.asin(-ahead.y)));
                })
                .then(2, () -> { })
                .thenWaitUntil(() -> Math.abs(viewRoll[0] - SeatLook.roll(seen().frame(), ctx.player().getYRot(),
                                ctx.player().getXRot())) < 1 && Math.abs(viewRoll[0]) > ROLLED, 1,
                        "the view to roll with the floor, its up at the top", () -> "rolled " + viewRoll[0])
                .then(0, () -> SeatLookClient.turn(ctx.player(), MOUSE_ACROSS, 0))
                .then(1, () -> { })
                .thenWaitUntil(() -> Math.abs(local(ctx).y) < ALONG, 1,
                        "the mouse to turn the look about the floor's up, along the floor", () -> "" + local(ctx))
                .then(0, () -> kept[0] = local(ctx))
                .thenServer(0, level -> {
                    final StructurePose pose = made.get().pose();
                    made.get().teleport(StructurePose.turned(pose.x(), pose.y(), pose.z(), 0, 0, 1,
                            Math.PI / 2 + FURTHER));
                })
                .thenWaitUntil(() -> seen().frame().pose().angleTo(made.get().pose()) < 1.0e-3, WAIT,
                        "the floor to turn further on the player's game")
                .then(SETTLE, () -> { })
                .thenWaitUntil(() -> local(ctx).distanceTo(kept[0]) < ALONG, 1,
                        "the look to be carried round with the floor, the same in its frame",
                        () -> local(ctx) + " against " + kept[0])
                .thenWaitUntil(() -> {
                    final StructureFrame frame = seen().frame();
                    final BlockPos at = lever.get();
                    ctx.lookAt(new Vec3(frame.worldX(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5),
                            frame.worldY(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5),
                            frame.worldZ(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5)));
                    return Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                            && hit.getBlockPos().equals(lever.get());
                }, SETTLE, "the aim to rest on the lever on the floor", () -> "aimed at "
                        + (Minecraft.getInstance().hitResult instanceof BlockHitResult hit ? hit.getBlockPos() + " "
                        + Minecraft.getInstance().level.getBlockState(hit.getBlockPos()) + " at " + hit.getLocation()
                        : Minecraft.getInstance().hitResult) + ", the lever at " + lever.get() + ", the eye on the"
                        + " floor at " + yard(ctx).add(local(ctx).scale(0)) + ", looking " + local(ctx))
                .then(0, () -> ctx.mc().options.keyUse.setDown(true))
                .then(1, () -> ctx.mc().options.keyUse.setDown(false))
                .thenWaitUntilServer(level -> level.getBlockState(lever.get()).getValue(LeverBlock.POWERED), SETTLE,
                        "the lever looked at on the rolled floor to be pulled",
                        level -> "the lever " + level.getBlockState(lever.get()))
                .thenServer(0, level -> serverPose.set(ctx.serverPlayer().getPose()))
                .thenWaitUntil(() -> ctx.player().getPose() == Pose.STANDING && serverPose.get() == Pose.STANDING, 1,
                        "the player to stand on the rolled floor on both sides, not crawl",
                        () -> "their game has them " + ctx.player().getPose() + ", the server " + serverPose.get())
                .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_BACK))
                .thenWaitUntil(() -> drawnUp[0] != null && drawnUp[0].distanceTo(floorUp()) < ALONG, SETTLE,
                        "the player to be drawn standing on the rolled floor, its up theirs",
                        () -> "drawn up " + drawnUp[0] + " against the floor's " + floorUp());
    }

    /* The floor's up, in the world. */
    private static Vec3 floorUp() {
        final StructureFrame frame = seen().frame();
        return new Vec3(frame.turnX(0, 1, 0), frame.turnY(0, 1, 0), frame.turnZ(0, 1, 0));
    }

    /* Where the player looks, in the floor's own frame. */
    private static Vec3 local(final ClientTestContext ctx) {
        final StructureFrame frame = seen().frame();
        final Vec3 look = Vec3.directionFromRotation(ctx.player().getXRot(), ctx.player().getYRot());
        return new Vec3(frame.unturnX(look.x, look.y, look.z), frame.unturnY(look.x, look.y, look.z),
                frame.unturnZ(look.x, look.y, look.z));
    }

    /* The floor on the player's game. */
    private static ClientStructure seen() {
        return ClientMovingStructures.all().stream().findFirst().orElse(null);
    }

    /* Where the player stands on the floor, in its yard. */
    private static Vec3 yard(final ClientTestContext ctx) {
        final StructureFrame frame = seen().frame();
        final Vec3 at = ctx.player().position();
        return new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z), frame.yardZ(at.x, at.y, at.z));
    }

    /* Where the player is, on the floor and in the world, for a failure to say. */
    private static String where(final ClientTestContext ctx) {
        return seen() == null ? "no floor" : "on the floor at " + yard(ctx) + " of its box " + seen().bounds()
                + ", in the world at " + ctx.player().position() + ", on the ground " + ctx.player().onGround()
                + ", standing on " + StructureFooting.standingOn(ctx.player());
    }

    private static void takeAll(final ServerLevel level) {
        for (final IStructureBody left : List.copyOf(LevelMovingStructures.of(level).all())) {
            if (left instanceof MovingStructure structure) {
                MovingStructures.takeAway(structure);
            }
        }
    }

    /* The up of the player's own model as it is drawn, after the Core's own turn of them, which comes before this. */
    public static final class DrawnUp {

        private final Vec3[] up = new Vec3[1];

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void drawn(final RenderLivingEvent.Pre<?, ?> event) {
            if (event.getEntity() == Minecraft.getInstance().player) {
                final Vector3f turned = event.getPoseStack().last().normal().transform(new Vector3f(0, 1, 0));
                up[0] = new Vec3(turned.x(), turned.y(), turned.z());
            }
        }
    }

    /* A turn of the floor: its name, the axis it is turned about and how far, in radians. */
    private record Turn(String name, double ax, double ay, double az, double angle) {
    }
}
