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
import dev.jstech.core.client.multiblock.FormedMeshes;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a player aiming about a small ship's cabin sees outlined, aboard the ship let go from its dock: aimed at the
 * pilot's seat, its console or the airlock's suit locker, the whole piece is outlined, past the block aimed at; aimed
 * at the hull, the plate of the hull in that block, within it.
 */
public final class ShipOutlineClientTests {

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
    /* How long the player's aim is given to settle, in ticks. */
    private static final int LOOK = 10;
    private static final double EYE = 1.62;
    /*
     * How far past its block a whole piece's outline reaches, at least, and how far a plate's may, in blocks: the
     * hull's skin stands a little proud of the blocks' grid, and its faces there belong to the nearest block.
     */
    private static final double PAST = 0.3;
    private static final double WITHIN = 0.1;

    private ShipOutlineClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void outline_aPieceAboardIsOutlinedWholeAndTheHullByItsPlate(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final PlannedBuild[] plan = new PlannedBuild[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final ServerLevel[] built = new ServerLevel[1];
        final ShipDesign design = new ShipDesign("JS-04", List.of(part("orbiter_cockpit"), part("airlock"),
                part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"), part("wheels")));
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().getAbilities().flying = false;
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
                .thenWaitUntil(() -> drawn(ship[0]), ARRIVE, "the ship to be drawn on the player's game");
        // Standing behind the pilot's seat, its back ahead, and past it the console; across the airlock, its suit
        // locker; and in the airlock, the hull's wall above the oxygen panel.
        look(ctx, plan, "small_orbiter_cockpit", new Vec3(0.5, 2.55, 4.85), new Vec3(-0.1, 1.9, 4.4), "seat", true);
        look(ctx, plan, "small_orbiter_cockpit", new Vec3(0.65, 2.55, 4.85), new Vec3(0.25, 1.6, 2.9), "console",
                true);
        look(ctx, plan, "small_airlock", new Vec3(-0.6, 2.0, 0.8), new Vec3(1.15, 1.4, 0.75), "locker", true);
        look(ctx, plan, "small_airlock", new Vec3(0.3, 2.0, 1.0), new Vec3(-1.85, 2.6, 0.6), "hull", false);
    }

    /*
     * The player looking from {@code eye} at {@code target}, both in the model of {@code model}'s space: the block
     * aimed at is outlined whole past it, {@code whole}, or by its plate within it, and is photographed.
     */
    private static void look(final ClientTestContext ctx, final PlannedBuild[] plan, final String model,
                             final Vec3 eye, final Vec3 target, final String what, final boolean whole) {
        final Vec3[] world = new Vec3[2];
        ctx.thenServer(0, level -> {
            world[0] = inWorld(plan[0], model, eye);
            world[1] = inWorld(plan[0], model, target);
            final Vec3 along = world[1].subtract(world[0]);
            final float yaw = (float) Math.toDegrees(Math.atan2(-along.x, along.z));
            final float pitch = (float) Math.toDegrees(-Math.atan2(along.y, along.horizontalDistance()));
            ctx.serverPlayer().setGameMode(GameType.CREATIVE);
            ctx.serverPlayer().getAbilities().flying = true;
            ctx.serverPlayer().onUpdateAbilities();
            ctx.serverPlayer().teleportTo(level, world[0].x, world[0].y - EYE, world[0].z, yaw, pitch);
        }).then(LOOK, () -> {
            ctx.assertTrue(ctx.mc().hitResult instanceof BlockHitResult, "aiming at the " + what + ", the player"
                    + " aims at a block: " + ctx.mc().hitResult);
            final BlockHitResult hit = (BlockHitResult) ctx.mc().hitResult;
            final Level level = ctx.mc().level;
            final double[] extent = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE,
                -Double.MAX_VALUE, -Double.MAX_VALUE};
            final boolean drawn = FormedMeshes.outline(level, hit.getBlockPos(), level.getBlockState(
                    hit.getBlockPos()), hit.getLocation(), (x1, y1, z1, x2, y2, z2) -> {
                        widen(extent, x1, y1, z1);
                        widen(extent, x2, y2, z2);
                    });
            ctx.assertTrue(drawn, "the " + what + " aimed at is outlined from the model: "
                    + level.getBlockState(hit.getBlockPos()));
            final double reach = Math.max(Math.max(-extent[0], -extent[1]), Math.max(-extent[2],
                    Math.max(extent[3] - 1, Math.max(extent[4] - 1, extent[5] - 1))));
            if (whole) {
                ctx.assertTrue(reach > PAST, "the whole " + what + " is outlined, past the block aimed at: reaches "
                        + reach);
            } else {
                ctx.assertTrue(reach < WITHIN, "the " + what + " is outlined by its plate in the block aimed at: "
                        + "reaches " + reach + " out of it");
            }
        }).thenScreenshot(0, "ship-outline-" + what);
    }

    /* Where the point {@code at} of the model {@code model} is in the world, as the ship stood let go. */
    private static Vec3 inWorld(final PlannedBuild plan, final String model, final Vec3 at) {
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.place().isEmpty() || !spot.place().get().model().getPath().endsWith("/" + model)) {
                continue;
            }
            final FormedPlace place = spot.place().get();
            final double[] turned = new double[3];
            CellSpace.toWorld(new double[] {at.x - place.x(), at.y - place.y(), at.z - place.z()}, place.turn(),
                    turned);
            final BlockPos block = plan.where(spot);
            return new Vec3(block.getX() + turned[0], block.getY() + turned[1], block.getZ() + turned[2]);
        }
        throw new IllegalStateException("no block of " + model + " in the plan");
    }

    private static void widen(final double[] extent, final double x, final double y, final double z) {
        extent[0] = Math.min(extent[0], x);
        extent[1] = Math.min(extent[1], y);
        extent[2] = Math.min(extent[2], z);
        extent[3] = Math.max(extent[3], x);
        extent[4] = Math.max(extent[4], y);
        extent[5] = Math.max(extent[5], z);
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
