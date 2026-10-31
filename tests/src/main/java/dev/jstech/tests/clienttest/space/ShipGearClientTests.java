/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.GearMotion;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipReadings;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A small ship's gear moving on a player's game, as the approved storyboard draws it: its console saying the gear is
 * up, the legs fold flat into the floor and the bays' doors shut, the belly left clean and still; saying it is down
 * and the ship rolls, the legs come down and the wheels turn, and stop when it stops. The ship stands in its dock and
 * its console is told on the player's game alone, which the server leaves be while the parked ship's readings do not
 * change, so the gear is watched where the camera is.
 */
public final class ShipGearClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The camera, in the dock's own places: off the ship's left side, low, looking at its gear under the belly. */
    private static final Vec3 EYES = new Vec3(-9, 1.2, 1);
    private static final Vec3 AT = new Vec3(0, 0.6, 1);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* A little more than the gear takes to go all the way up or down, in ticks. */
    private static final int CYCLE = (int) Math.ceil(GearMotion.CYCLE * 20) + 10;
    /* A quarter of the way, in ticks: the struts folding. */
    private static final int QUARTER = (int) Math.round(GearMotion.CYCLE * 20 / 4);
    /* A few ticks: rolling, the wheels turn a little; still, nothing moves. */
    private static final int A_FEW = 4;
    /* How fast it is said to roll, in metres a second. */
    private static final double ROLLING = 6;
    /* How far two pixels may differ and be the same, in each channel; how many must differ for a change. */
    private static final int SAME = 6;
    private static final int GONE = 400;
    private static final int TURNED = 20;
    private static final int STILL = 3;
    /* Of the nine pixels round one, how many must differ too for it to be of a thing, not a bit flying past. */
    private static final int MOST = 6;
    private static final int CHANNEL = 0xFF;
    private static final ShipDesign SHIP = new ShipDesign("JS-07", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("workshop"), part("reactor_flush")), List.of(part("twin_bells"),
            part("side_intakes"), part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipGearClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gear_foldsIntoTheFloorAndComesDownTurningItsWheels(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final MovingStructure[] ship = new MovingStructure[1];
        final int[][] frames = new int[7][];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                if (ship[0] != null) {
                    MovingStructures.takeAway(ship[0]);
                }
                if (built[0] != null) {
                    clear(ctx, built[0]);
                    built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
            ctx.serverPlayer().setGameMode(GameType.SPECTATOR);
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(0, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenServer(SETTLE, level -> ship[0] = ((ShipyardTerminalBlockEntity) level.getBlockEntity(
                        terminal[0])).release(null).orElseThrow())
                .thenWaitUntilServer(level -> ship[0].ready(), ARRIVE, "its blocks to come into the structure",
                        level -> "arriving")
                .thenWaitUntil(() -> console(ctx, ship[0]) != null, ARRIVE, "its console on the player's game");
        stand(ctx, EYES);
        ctx.then(2, () -> {
            ctx.lookAt(place(ctx, AT));
            ctx.mc().options.hideGui = true;
        })
                .then(SETTLE, () -> frames[0] = frame(ctx))
                .thenScreenshot(0, "ship-gear-down")
                .then(0, () -> tell(ctx, ship[0], false, false, 0))
                .then(QUARTER, () -> frames[1] = frame(ctx))
                .thenScreenshot(0, "ship-gear-folding")
                .then(CYCLE, () -> frames[2] = frame(ctx))
                .thenScreenshot(0, "ship-gear-stowed")
                .then(A_FEW, () -> frames[3] = frame(ctx))
                .then(0, () -> tell(ctx, ship[0], true, true, ROLLING))
                .then(CYCLE, () -> frames[4] = frame(ctx))
                .thenScreenshot(0, "ship-gear-rolling")
                .then(A_FEW, () -> frames[5] = frame(ctx))
                .then(0, () -> tell(ctx, ship[0], true, true, 0))
                .then(A_FEW, () -> frames[6] = frame(ctx))
                .then(A_FEW, () -> {
                    final int[] stopped = frame(ctx);
                    // Where the gear is seen: what differs between down and up, bits flying past left out.
                    final boolean[] gear = area(frames[0], frames[2], ctx.mc().getWindow().getGuiScaledWidth());
                    final int seen = count(gear);
                    ctx.assertTrue(seen >= GONE, "all the way up, the gear is gone from under the belly: " + seen);
                    ctx.assertTrue(changed(frames[0], frames[1], gear) >= TURNED
                            && changed(frames[1], frames[2], gear) >= TURNED, "going up, the gear is seen folding,"
                            + " between down and in: " + changed(frames[0], frames[1], gear));
                    ctx.assertTrue(changed(frames[2], frames[3], gear) <= STILL, "and stays gone, nothing moving: "
                            + changed(frames[2], frames[3], gear));
                    ctx.assertTrue(changed(frames[2], frames[4], gear) >= seen / 2, "down again, the gear is out"
                            + " under the belly: " + changed(frames[2], frames[4], gear) + " of " + seen);
                    ctx.assertTrue(changed(frames[4], frames[5], gear) >= TURNED, "rolling, its wheels turn: "
                            + changed(frames[4], frames[5], gear));
                    ctx.assertTrue(changed(frames[6], stopped, gear) <= STILL, "stopped, its wheels stand still: "
                            + changed(frames[6], stopped, gear));
                });
    }

    /*
     * The pixels that differ between {@code a} and {@code b}, frames {@code width} wide, each kept only with most of
     * the pixels round it differing too: a thing seen in one and not the other, not the scattered bits of something
     * flying past.
     */
    private static boolean[] area(final int[] a, final int[] b, final int width) {
        final int height = a.length / width;
        final boolean[] differs = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            differs[i] = differ(a[i], b[i]);
        }
        final boolean[] out = new boolean[a.length];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int round = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        round += differs[(y + dy) * width + x + dx] ? 1 : 0;
                    }
                }
                out[y * width + x] = differs[y * width + x] && round >= MOST;
            }
        }
        return out;
    }

    private static int count(final boolean[] area) {
        int found = 0;
        for (final boolean in : area) {
            found += in ? 1 : 0;
        }
        return found;
    }

    /* How many pixels of {@code area} differ between two frames. */
    private static int changed(final int[] a, final int[] b, final boolean[] area) {
        int found = 0;
        for (int i = 0; i < Math.min(a.length, area.length); i++) {
            if (area[i] && differ(a[i], b[i])) {
                found++;
            }
        }
        return found;
    }

    /* Tells the console of {@code ship}, on the player's game alone, its gear {@code down}, on the land or not. */
    private static void tell(final ClientTestContext ctx, final MovingStructure ship, final boolean down,
                             final boolean grounded, final double speed) {
        final AirReserveEntity console = console(ctx, ship);
        ctx.assertTrue(console != null, "its console on the player's game");
        console.flew(new ShipReadings(speed, 0, 0, 100, false, 0, false, down, false, down ? 0 : 40, grounded,
                ShipReadings.Pilot.NONE));
    }

    /* The console of {@code ship} on the player's game, if it has arrived. */
    private static AirReserveEntity console(final ClientTestContext ctx, final MovingStructure ship) {
        final BlockPos at = ((ShipFlight) ship.motion()).consoleIn(ship).orElseThrow();
        return ctx.mc().level.getBlockEntity(at) instanceof AirReserveEntity keeper ? keeper : null;
    }

    /* The whole frame, its pixels row by row. */
    private static int[] frame(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int[] out = new int[width * height];
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y * width + x] = frame.pixel(x, y);
                }
            }
        }
        return out;
    }

    /* Whether two pixels differ by more than {@link #SAME} in a channel. */
    private static boolean differ(final int a, final int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((a >> shift & CHANNEL) - (b >> shift & CHANNEL)) > SAME) {
                return true;
            }
        }
        return false;
    }

    /* Floats the player, a spectator, with their eyes at the dock's place {@code eyes}. */
    private static void stand(final ClientTestContext ctx, final Vec3 eyes) {
        ctx.thenServer(0, level -> {
            final Vec3 there = place(ctx, eyes);
            ctx.serverPlayer().teleportTo(level, there.x, there.y - ctx.serverPlayer().getEyeHeight(), there.z, 0,
                    0);
        }).thenWaitUntil(() -> ctx.player().getEyePosition().distanceTo(place(ctx, eyes)) < 0.1, ARRIVE,
                "the player to float by the dock");
    }

    /* The world's point of the dock's place {@code at}: across from the bay's middle, up from the ground, along. */
    private static Vec3 place(final ClientTestContext ctx, final Vec3 at) {
        return Vec3.atLowerCornerOf(ctx.abs(CORNER.offset(11, 3, 13))).add(at);
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
