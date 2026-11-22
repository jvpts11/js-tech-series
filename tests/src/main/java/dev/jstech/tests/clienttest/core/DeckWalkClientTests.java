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
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.region.Box;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Walking in a moving structure whose floor pulls towards itself, on the player's own game: a floor with a step of
 * slabs at its far end, pitched half way up, rolled on its side and turned upside down, and in each the player stands
 * on it, walks along it up onto the step, jumps and comes down on it again, the server taking where they stand.
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
                    .thenAssert(SETTLE, () -> Math.abs(yard(ctx).y - (seen().bounds().minY() + 1)) < ON,
                            "the player stays on the floor " + turn.name() + ", its floor down: " + where(ctx))
                    .then(0, () -> ctx.mc().options.keyUp.setDown(true))
                    .then(WALK, () -> ctx.mc().options.keyUp.setDown(false))
                    .thenAssert(SETTLE, () -> yard(ctx).z < seen().bounds().maxZ() + 0.5 - WALKED
                                    && Math.abs(yard(ctx).y - (seen().bounds().minY() + 1 + SLAB)) < ON,
                            "the player walked along the floor " + turn.name() + " and up onto its step: "
                                    + where(ctx))
                    .thenServer(0, level -> server.set(ctx.serverPlayer().position()))
                    .thenAssert(0, () -> server.get().distanceTo(ctx.player().position()) < AGREE,
                            "the server has the player where their game has them: " + server.get() + " against "
                                    + ctx.player().position())
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

    /* A turn of the floor: its name, the axis it is turned about and how far, in radians. */
    private record Turn(String name, double ax, double ay, double az, double angle) {
    }
}
