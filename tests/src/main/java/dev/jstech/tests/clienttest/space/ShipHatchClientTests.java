/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.crew.ShipHatches;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipFlightModel;
import dev.jstech.space.ship.ShipGround;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A small ship's airlock door used from outside, on a player's game, as a player uses any block: standing on the land
 * by a ship nobody owns, set down beside its dock, looking at the door and using it, they are taken in, in survival as
 * in creative. (A ship with a crew asks for a place aboard first; its game test covers that.)
 */
public final class ShipHatchClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 70;
    /* How far south of its dock the ship is set down on the grass, and how high over it it is let go, in blocks. */
    private static final double BESIDE = 36;
    private static final double OVER_THE_GRASS = 4;
    /* How far the ship may move in a tick and still be at rest, in blocks. */
    private static final double STILL = 1.0e-4;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* How far out of the door the player stands, in blocks; how long they are given to land and to aim, in ticks. */
    private static final int OUTSIDE = 2;
    private static final int LAND = 30;

    private ShipHatchClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void hatch_usedFromOutsideTakesThePlayerInInSurvivalAndCreative(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] door = new BlockPos[1];
        final Direction[] out = new Direction[1];
        final StructurePose[] stood = new StructurePose[1];
        final ShipDesign design = new ShipDesign("JS-05", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            if (ship[0] != null) {
                MovingStructures.takeAway(ship[0]);
            }
            if (built[0] != null) {
                clear(ctx, built[0]);
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(design);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving")
                // Its airlock door found where the ship stands in its dock, a place in its yard wherever it goes; then
                // the ship taken over the grass beside the dock, a little up, and left at rest: it sinks onto its gear.
                .thenServer(0, level -> {
                    final PlanSpot spot = plan[0].plan().spots().stream().filter(each -> each.block().equals(
                            ShipKitBlocks.AIRLOCK_DOOR.getId())).findFirst().orElseThrow();
                    door[0] = BlockPos.containing(MovingStructures.toYard(ship[0], Vec3.atCenterOf(plan[0].where(
                            spot))));
                    out[0] = ShipHatches.outward(ship[0], door[0]).orElseThrow();
                    final StructurePose now = ship[0].pose();
                    ship[0].teleport(now.movedTo(now.x(), now.y() + OVER_THE_GRASS, now.z() + BESIDE));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(
                            ShipFlightModel.State.RESTING));
                })
                .thenWaitUntilServer(level -> {
                    final boolean still = stood[0] != null && ship[0].pose().distanceTo(stood[0]) < STILL
                            && ShipGround.meet(level, ship[0], ship[0].frame()).touching();
                    stood[0] = ship[0].pose();
                    return still;
                }, ARRIVE, "the ship to come down onto the grass and rest", level -> ship[0].pose().toString())
                .thenWaitUntil(() -> drawn(ship[0]), ARRIVE, "the ship to be drawn on the player's game");
        enter(ctx, ship, door, out, GameType.SURVIVAL);
        enter(ctx, ship, door, out, GameType.CREATIVE);
    }

    /* The player, in {@code mode}, standing on the land out of the door, uses it as the game uses a block. */
    private static void enter(final ClientTestContext ctx, final MovingStructure[] ship, final BlockPos[] door,
                              final Direction[] out, final GameType mode) {
        ctx.thenServer(0, level -> {
            ctx.serverPlayer().setGameMode(mode);
            ctx.serverPlayer().getAbilities().flying = false;
            ctx.serverPlayer().onUpdateAbilities();
            final Vec3 there = MovingStructures.inWorld(level, Vec3.atBottomCenterOf(door[0].relative(out[0],
                    OUTSIDE)));
            ctx.serverPlayer().teleportTo(level, there.x, there.y, there.z, 0F, 0F);
        }).then(LAND, () -> {
            // Facing the door: back along its way out, at its middle.
            final Vec3 eye = ctx.player().getEyePosition();
            final Vec3 target = ctx.server().submit(() -> MovingStructures.inWorld(ctx.serverLevel(),
                    Vec3.atCenterOf(door[0]))).join();
            final Vec3 along = target.subtract(eye);
            final float yaw = (float) Math.toDegrees(Math.atan2(-along.x, along.z));
            final float pitch = (float) Math.toDegrees(-Math.atan2(along.y, along.horizontalDistance()));
            ctx.player().setYRot(yaw);
            ctx.player().yRotO = yaw;
            ctx.player().setXRot(pitch);
            ctx.player().xRotO = pitch;
        }).then(2, () -> {
            ctx.assertTrue(ctx.mc().hitResult instanceof BlockHitResult aimed && FormedPartBlock.originalAt(
                    ctx.mc().level, aimed.getBlockPos(), ctx.mc().level.getBlockState(aimed.getBlockPos()))
                    .is(ShipKitBlocks.AIRLOCK_DOOR.get()), "in " + mode + ", the player aims at the door: "
                    + ctx.mc().hitResult);
            final BlockHitResult hit = (BlockHitResult) ctx.mc().hitResult;
            ctx.mc().gameMode.useItemOn(ctx.player(), InteractionHand.MAIN_HAND, hit);
        }).thenWaitUntilServer(level -> side(ship[0], ctx, door[0], out[0]) < 0, SETTLE * 3,
                "in " + mode + ", using the door from outside to take the player in",
                level -> "they stand " + side(ship[0], ctx, door[0], out[0]) + " out of it")
                .thenScreenshot(0, "ship-hatch-" + mode.getName());
    }

    /* How far out of {@code ship}'s airlock door the player stands, in its yard: above nought outside. */
    private static double side(final MovingStructure ship, final ClientTestContext ctx, final BlockPos door,
                               final Direction out) {
        final Vec3 at = MovingStructures.toYard(ship, ctx.serverPlayer().position());
        return (at.x - door.getX() - 0.5) * out.getStepX() + (at.z - door.getZ() - 0.5) * out.getStepZ();
    }

    /* Whether the ship has reached the player's game and been built there. */
    private static boolean drawn(final MovingStructure ship) {
        final ClientStructure seen = ship == null ? null : ClientMovingStructures.byPlot(ship.plot());
        return seen != null && seen.ready() && seen.meshes().built() > 0 && !seen.meshes().waiting();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = Y_BOTTOM; y <= Y_TOP; y++) {
                    final Block block = y == Y_BOTTOM ? Blocks.BEDROCK : y < Y_GRASS ? Blocks.DIRT
                            : y == Y_GRASS ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    level.setBlock(ctx.abs(new BlockPos(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
