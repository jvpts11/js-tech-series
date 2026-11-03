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
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPieceUses;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.client.KeyMapping;
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
 * A small ship's pilot seat, on a player's game: aimed at from outside, through its canopy, using it sits no one;
 * from inside the cockpit it is sat in, aimed at by its back too, which stands in the block over it; and its pilot
 * getting up stands inside the ship, never out of the hull.
 */
public final class ShipSeatClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    private static final double EYE = 1.62;
    private static final String COCKPIT = "small_orbiter_cockpit";
    /*
     * Where the player looks from, in the cockpit's model: over the canopy, outside; and standing behind the seat; and
     * at what: the seat's back. The ship's inside the pilot gets up in, from the cockpit's console to the end of the
     * airlock behind it: across, up and along it, in the cockpit's model, within the hull, over the floor and under
     * the roof.
     */
    private static final Vec3 OVER_THE_CANOPY = new Vec3(0.3, 3.6, 3.6);
    private static final Vec3 BEHIND_THE_SEAT = new Vec3(0.5, 0.95 + EYE, 4.85);
    private static final Vec3 SEAT_BACK = new Vec3(-0.1, 1.9, 4.4);
    private static final double ROOM_X = 1.6;
    private static final double FLOOR = 0.3;
    private static final double ROOF = 2.9;
    private static final double ROOM_Z0 = 2.6;
    private static final double ROOM_Z1 = 8.0;

    private ShipSeatClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void seat_isSatInFromTheCockpitNotThroughItsCanopyAndGotUpFromIntoIt(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] seat = new BlockPos[1];
        final KeyMapping crouch = ctx.mc().options.keyShift;
        final ShipDesign design = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"), part("wheels")));
        ctx.afterTest(() -> {
            crouch.setDown(false);
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                ctx.serverPlayer().getAbilities().flying = false;
                ctx.serverPlayer().setGameMode(GameType.CREATIVE);
                if (ship[0] != null) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                }
            });
        });
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
                .thenWaitUntil(() -> drawn(ship[0]), ARRIVE, "the ship to be drawn on the player's game")
                .thenServer(0, level -> seat[0] = BlockPos.containing(MovingStructures.toYard(ship[0],
                        Vec3.atCenterOf(plan[0].where(plan[0].plan().spots().stream().filter(each -> each.block()
                                .equals(ShipKitBlocks.PILOT_SEAT.getId())).findFirst().orElseThrow())))));
        // Over the canopy, outside, the seat in the crosshair as if seen through the glass: using it sits no one.
        look(ctx, plan, OVER_THE_CANOPY, SEAT_BACK);
        ctx.then(SETTLE, () -> ctx.mc().gameMode.useItemOn(ctx.player(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(seat[0]), Direction.UP, seat[0], false)))
                .thenServer(SETTLE, level -> ctx.assertTrue(!ctx.serverPlayer().isPassenger(), "used through its"
                        + " canopy from outside, the seat sits no one: " + ctx.serverPlayer().getVehicle()));
        // Standing behind it in the cockpit, aiming at it: using it sits the player.
        look(ctx, plan, BEHIND_THE_SEAT, SEAT_BACK);
        // Its back stands up into the block over the seat: aimed at there, it is the seat that is used.
        ctx.then(SETTLE, () -> {
            ctx.assertTrue(ctx.mc().hitResult instanceof BlockHitResult hit && (hit.getBlockPos().equals(seat[0])
                    || FormedPieceUses.worksBy(ctx.mc().level, hit.getBlockPos(), hit.getLocation())
                    .filter(seat[0]::equals).isPresent()), "from the cockpit, the player aims at the seat: "
                    + ctx.mc().hitResult);
            ctx.mc().gameMode.useItemOn(ctx.player(), InteractionHand.MAIN_HAND, (BlockHitResult) ctx.mc().hitResult);
        }).thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity, ARRIVE,
                "from the cockpit, the player to sit in the seat")
                // Getting up, crouching: they stand in the cockpit, on its deck, under its roof.
                .then(SETTLE, () -> crouch.setDown(true))
                .thenWaitUntil(() -> !(ctx.player().getVehicle() instanceof StructureSeatEntity), ARRIVE,
                        "the pilot to get up")
                .then(SETTLE, () -> crouch.setDown(false))
                .thenServer(SETTLE, level -> {
                    final Vec3 at = inModel(plan[0], ship[0], ctx.serverPlayer().position());
                    ctx.assertTrue(Math.abs(at.x) < ROOM_X && at.y > FLOOR && at.y < ROOF && at.z > ROOM_Z0
                            && at.z < ROOM_Z1, "got up, the pilot stands inside the ship, in the cockpit's model at "
                            + at);
                });
    }

    /* The player, flying, their eyes at {@code eye} of the cockpit's model, looking at {@code target} of it. */
    private static void look(final ClientTestContext ctx, final PlannedBuild[] plan, final Vec3 eye,
                             final Vec3 target) {
        ctx.thenServer(0, level -> {
            final Vec3 from = inWorld(plan[0], eye);
            final Vec3 along = inWorld(plan[0], target).subtract(from);
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            ctx.serverPlayer().getAbilities().flying = true;
            ctx.serverPlayer().onUpdateAbilities();
            ctx.serverPlayer().teleportTo(level, from.x, from.y - EYE, from.z,
                    (float) Math.toDegrees(Math.atan2(-along.x, along.z)),
                    (float) Math.toDegrees(-Math.atan2(along.y, along.horizontalDistance())));
        });
    }

    /* Where the point {@code at} of the cockpit's model is in the world, as the ship stood let go. */
    private static Vec3 inWorld(final PlannedBuild plan, final Vec3 at) {
        final PlanSpot spot = cockpitSpot(plan);
        final FormedPlace place = spot.place().orElseThrow();
        final double[] turned = new double[3];
        CellSpace.toWorld(new double[] {at.x - place.x(), at.y - place.y(), at.z - place.z()}, place.turn(), turned);
        final BlockPos block = plan.where(spot);
        return new Vec3(block.getX() + turned[0], block.getY() + turned[1], block.getZ() + turned[2]);
    }

    /* Where {@code world} is in the cockpit's model, aboard {@code ship} as it stands now. */
    private static Vec3 inModel(final PlannedBuild plan, final MovingStructure ship, final Vec3 world) {
        final PlanSpot spot = cockpitSpot(plan);
        final FormedPlace place = spot.place().orElseThrow();
        final BlockPos block = BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(plan.where(spot))));
        final Vec3 yard = MovingStructures.toYard(ship, world);
        final double[] own = new double[3];
        CellSpace.toModel(new double[] {yard.x - block.getX(), yard.y - block.getY(), yard.z - block.getZ()},
                place.turn(), own);
        return new Vec3(own[0] + place.x(), own[1] + place.y(), own[2] + place.z());
    }

    /* A block of the plan standing for a block of the cockpit's model. */
    private static PlanSpot cockpitSpot(final PlannedBuild plan) {
        return plan.plan().spots().stream().filter(spot -> spot.place().isPresent() && spot.place().get().model()
                .getPath().endsWith("/" + COCKPIT)).findFirst().orElseThrow();
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
