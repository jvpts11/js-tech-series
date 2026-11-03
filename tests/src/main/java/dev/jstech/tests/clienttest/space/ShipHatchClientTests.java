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
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.space.crew.ShipDoorRules;
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
import java.util.Optional;
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
 * A small ship's airlock, boarded from the land on a player's game, as a player does it: by a ship nobody owns, set
 * down on the grass beside its dock, its inner doors shut, the player in survival aims at its outer door and uses it;
 * the door opens and the boarding stair comes down; they walk up the stair and through the door into the airlock. (A
 * ship with a crew asks for a place aboard first; its game test covers that.)
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
    /* How far out of the door the player stands to use it and to start up the stair, in blocks. */
    private static final double OUTSIDE = 3.5;
    private static final int LAND = 30;
    /* How long the player walks before they are in, in ticks, at most; how far within the door is in. */
    private static final int WALK = 200;
    private static final double IN = 0.6;

    private ShipHatchClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void hatch_opensFromOutsideItsStairComesDownAndThePlayerWalksIn(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerLevel[] built = new ServerLevel[1];
        final Vec3[] door = new Vec3[1];
        final FormedDoors.Door[] hatch = new FormedDoors.Door[1];
        final Direction[] out = new Direction[1];
        final StructurePose[] stood = new StructurePose[1];
        final ShipDesign design = new ShipDesign("JS-05", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush")), List.of(part("twin_bells"), part("swept_wings"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.mc().options.keyUp.setDown(false);
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
                // Its airlock's outer door found where the ship stands in its dock, the middle of its two blocks, a
                // place in its yard wherever it goes; its inner doors shut; then the ship taken over the grass beside
                // the dock, a little up, and left at rest: it sinks onto its gear.
                .thenServer(0, level -> {
                    final List<BlockPos> blocks = plan[0].plan().spots().stream().filter(each -> each.block().equals(
                            ShipKitBlocks.AIRLOCK_DOOR.getId())).map(each -> BlockPos.containing(MovingStructures
                            .toYard(ship[0], Vec3.atCenterOf(plan[0].where(each))))).toList();
                    door[0] = Vec3.atCenterOf(blocks.getFirst()).add(Vec3.atCenterOf(blocks.getLast())).scale(0.5);
                    hatch[0] = outerDoor(level, blocks.getFirst());
                    ShipDoorRules.otherSide(level, hatch[0]).forEach(inner -> FormedDoors.set(level, inner, true));
                    out[0] = ShipHatches.outward(ship[0], blocks.getFirst()).orElseThrow();
                    final StructurePose now = ship[0].pose();
                    ship[0].teleport(now.movedTo(now.x(), now.y() + OVER_THE_GRASS, now.z() + BESIDE));
                    MovingStructures.move(ship[0], ((ShipFlight) ship[0].motion()).with(
                            ShipFlightModel.State.RESTING));
                })
                .thenWaitUntilServer(level -> {
                    final boolean still = stood[0] != null && ship[0].pose().distanceTo(stood[0]) < STILL
                            && ShipGround.meet(level, ship[0], ship[0].frame()).touching()
                            && ship[0].motion() instanceof ShipFlight flight && flight.grounded();
                    stood[0] = ship[0].pose();
                    return still;
                }, ARRIVE, "the ship to come down onto the grass and rest", level -> ship[0].pose().toString())
                .thenWaitUntil(() -> drawn(ship[0]), ARRIVE, "the ship to be drawn on the player's game");
        // On the grass out of the door, facing it, in survival: the door used as the game uses a block.
        ctx.thenServer(0, level -> {
            ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
            ctx.serverPlayer().getAbilities().flying = false;
            ctx.serverPlayer().onUpdateAbilities();
            final Vec3 there = MovingStructures.inWorld(level, door[0].relative(out[0], OUTSIDE));
            ctx.serverPlayer().teleportTo(level, there.x, ctx.abs(new BlockPos(0, Y_GRASS + 1, 0)).getY(), there.z,
                    0F, 0F);
        }).then(LAND, () -> face(ctx, door[0]))
                .then(2, () -> {
                    ctx.assertTrue(ctx.mc().hitResult instanceof BlockHitResult aimed && FormedPartBlock.originalAt(
                            ctx.mc().level, aimed.getBlockPos(), ctx.mc().level.getBlockState(aimed.getBlockPos()))
                            .is(ShipKitBlocks.AIRLOCK_DOOR.get()), "the player aims at the airlock's outer door: "
                            + ctx.mc().hitResult);
                    ctx.mc().gameMode.useItemOn(ctx.player(), InteractionHand.MAIN_HAND,
                            (BlockHitResult) ctx.mc().hitResult);
                })
                .thenWaitUntilServer(level -> !FormedDoors.shut(level, hatch[0]), SETTLE * 3,
                        "the outer door to open as it is used", level -> "shut")
                .thenScreenshot(SETTLE, "ship-hatch-stair-down")
                // Up the stair and through the door, walking at it.
                .then(0, () -> {
                    face(ctx, door[0]);
                    ctx.player().setXRot(0);
                    ctx.mc().options.keyUp.setDown(true);
                })
                .thenWaitUntilServer(level -> side(ship[0], ctx, door[0], out[0]) < -IN, WALK,
                        "the player to walk up the stair and through the door into the airlock",
                        level -> "they stand " + side(ship[0], ctx, door[0], out[0]) + " out of the door, at "
                                + MovingStructures.toYard(ship[0], ctx.serverPlayer().position()) + " in its yard, "
                                + "the door's middle at " + door[0])
                .then(0, () -> ctx.mc().options.keyUp.setDown(false))
                .thenScreenshot(SETTLE, "ship-hatch-walked-in");
    }

    /* The player looks at {@code door}, a place in the ship's yard, from where they stand. */
    private static void face(final ClientTestContext ctx, final Vec3 door) {
        final Vec3 eye = ctx.player().getEyePosition();
        final Vec3 target = ctx.server().submit(() -> MovingStructures.inWorld(ctx.serverLevel(), door)).join();
        final Vec3 along = target.subtract(eye);
        final float yaw = (float) Math.toDegrees(Math.atan2(-along.x, along.z));
        final float pitch = (float) Math.toDegrees(-Math.atan2(along.y, along.horizontalDistance()));
        ctx.player().setYRot(yaw);
        ctx.player().yRotO = yaw;
        ctx.player().setXRot(pitch);
        ctx.player().xRotO = pitch;
    }

    /* The airlock's outer door, of its block at {@code at}. */
    private static FormedDoors.Door outerDoor(final ServerLevel level, final BlockPos at) {
        for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(at)).places()) {
            final Optional<FormedDoors.Door> door = FormedDoors.named(level, at, place, ShipDoorRules.OUTER_DOOR);
            if (door.isPresent()) {
                return door.get();
            }
        }
        throw new IllegalStateException("no outer door at " + at);
    }

    /* How far out of {@code ship}'s airlock door the player stands, in its yard: above nought outside. */
    private static double side(final MovingStructure ship, final ClientTestContext ctx, final Vec3 door,
                               final Direction out) {
        final Vec3 at = MovingStructures.toYard(ship, ctx.serverPlayer().position());
        return (at.x - door.x) * out.getStepX() + (at.z - door.z) * out.getStepZ();
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
