/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestConveyors;
import dev.jstech.tests.testkit.BenchReport;
import dev.jstech.tests.testkit.BenchSupport;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What items riding the belts cost the server: forty-six runs of forty-four belts, the first twenty blocks of each
 * laden four items to a block, three thousand six hundred and eighty items moving at once. The server's tick is read
 * with the belts standing empty and again with the items riding, each a hundred ticks after what came before, so the
 * game's average holds none of the ticks that laid the belts or the items; no item reaches the end of its run while it
 * is timed. The numbers go to the log and to {@link BenchReport}; the test asks only that every item still rides, a
 * machine's speed being its own.
 *
 * <p>Runs alone in its own batch so the clock is not shared with other tests.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConveyorBenchGameTests {

    private static final String BENCH = "bench";
    private static final int Y = 2;
    private static final int RUNS = 46;
    private static final int LENGTH = 44;
    private static final int LADEN = 20;
    private static final int PER_BLOCK = 4;
    private static final int ITEMS = RUNS * LADEN * PER_BLOCK;
    /* The game averages a tick over the last hundred: each reading waits longer than that after what came before. */
    private static final int AVERAGED = 110;
    private static final double MICROS_PER_MILLI = 1_000;

    private ConveyorBenchGameTests() {
    }

    @GameTest(template = BENCH, batch = "jscore_bench_conveyors", timeoutTicks = 600)
    public static void bench_thousandsOfItemsRideTheBelts(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        for (int z = 1; z <= RUNS; z++) {
            for (int x = 1; x <= LENGTH; x++) {
                helper.setBlock(new BlockPos(x, Y, z), TestConveyors.BELT.get().defaultBlockState()
                        .setValue(ConveyorBlock.FACING, Direction.EAST));
            }
        }
        final double[] idle = new double[1];
        helper.startSequence()
                .thenExecuteAfter(AVERAGED, () -> BenchSupport.guarded(() -> {
                    idle[0] = BenchSupport.averageTickMs(server);
                    for (int z = 1; z <= RUNS; z++) {
                        for (int x = 1; x <= LADEN; x++) {
                            final ConveyorBlockEntity belt = belt(helper, new BlockPos(x, Y, z));
                            for (int k = 0; k < PER_BLOCK; k++) {
                                belt.put(new ItemStack(Items.COBBLESTONE), k / (double) PER_BLOCK);
                            }
                        }
                    }
                }))
                .thenExecuteAfter(AVERAGED, () -> BenchSupport.guarded(() -> {
                    final double riding = BenchSupport.averageTickMs(server);
                    int on = 0;
                    for (int z = 1; z <= RUNS; z++) {
                        for (int x = 1; x <= LENGTH; x++) {
                            on += belt(helper, new BlockPos(x, Y, z)).lane().size();
                        }
                    }
                    helper.assertTrue(on == ITEMS, "every item still rides the belts: " + on + " of " + ITEMS);
                    final double items = Math.max(0, riding - idle[0]);
                    final BenchReport report = new BenchReport("conveyors").put("belts", (long) RUNS * LENGTH)
                            .put("items", (long) ITEMS).put("idle_tick_ms", idle[0]).put("riding_tick_ms", riding)
                            .put("items_ms", items).put("microseconds_an_item", items * MICROS_PER_MILLI / ITEMS);
                    JsTests.LOGGER.info("[JSC-BENCH] conveyors {}", report.summary());
                    try {
                        report.write();
                    } catch (final IOException failed) {
                        JsTests.LOGGER.warn("[JSC-BENCH] the conveyor numbers could not be written", failed);
                    }
                }))
                .thenSucceed();
    }

    private static ConveyorBlockEntity belt(final GameTestHelper helper, final BlockPos relative) {
        if (!(helper.getBlockEntity(relative) instanceof ConveyorBlockEntity belt)) {
            throw new GameTestAssertException("no conveyor at " + relative.toShortString());
        }
        return belt;
    }
}
