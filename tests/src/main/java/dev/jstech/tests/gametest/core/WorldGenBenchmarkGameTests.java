/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.logging.LogUtils;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.BenchReport;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * How fast the series' ground is made: the test land, with every kind of generation the Core has (its terrain, its
 * landforms, rock in layers, seas, veins, reservoirs), beside the game's own overworld ground under plains, a square
 * of chunks of each made from nothing on the same server, one after the other, with a small square of each first so
 * the clock does not count the code warming up. It measures how long each square takes to be made whole, and the
 * longest the server works a tick meanwhile; the test land must be made no slower than the game's own ground.
 *
 * <p>Runs alone in its own batch so the clock is not shared with other tests. The numbers go to {@code local/bench/}
 * through {@link BenchReport}.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldGenBenchmarkGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ARENA = "empty";
    private static final ResourceKey<Level> TEST = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bench_test_land"));
    private static final ResourceKey<Level> VANILLA = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bench_vanilla_land"));
    /** The square measured: seventeen chunks a side. */
    private static final int RADIUS = 8;
    /** The square made first to warm the code up: five a side, elsewhere. */
    private static final int WARM_RADIUS = 2;
    /** Where the squares stand, in chunks from the middle of the world, clear of each other. */
    private static final int FAR = 64;
    private static final double NANOS_A_MILLI = 1_000_000.0;
    /** How much slower than the game's own ground the test land may be made, for the noise of one run. */
    private static final double SLACK = 1.5;

    private WorldGenBenchmarkGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_bench_worldgen", timeoutTicks = 240_000)
    public static void bench_testLandIsMadeBesideTheGamesOwnGround(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerLevel test = RuntimeDimensions.getOrCreate(server, TEST, TestWorldGen.TEST_LAND.id());
        final ServerLevel vanilla = RuntimeDimensions.getOrCreate(server, VANILLA, TestWorldGen.VANILLA_LAND.id());
        final Bench bench = new Bench(List.of(new Square("warm_test_land", test, WARM_RADIUS, -FAR),
                new Square("warm_vanilla", vanilla, WARM_RADIUS, -FAR), new Square("test_land", test, RADIUS, FAR),
                new Square("vanilla", vanilla, RADIUS, FAR)));
        helper.onEachTick(bench);
        helper.succeedWhen(() -> {
            helper.assertTrue(bench.done(), "the squares are still being made: " + bench.progress());
            final BenchReport report = new BenchReport("worldgen");
            for (final Square square : bench.squares()) {
                report.put(square.name() + "_ms", square.millis());
                report.put(square.name() + "_ms_a_chunk", square.millis() / square.chunks());
                report.put(square.name() + "_longest_tick_ms", square.longestTick());
            }
            final Square land = bench.squares().get(2);
            final Square game = bench.squares().get(3);
            report.put("test_land_over_vanilla", land.millis() / game.millis());
            LOGGER.info("[JSC-BENCH] worldgen {}", report.summary());
            try {
                report.write();
            } catch (final IOException failed) {
                LOGGER.warn("[JSC-BENCH] the worldgen numbers could not be written", failed);
            }
            RuntimeDimensions.remove(server, TEST);
            RuntimeDimensions.remove(server, VANILLA);
            helper.assertTrue(land.millis() <= game.millis() * SLACK, "the test land was made in " + land.millis()
                    + " ms, the game's own ground in " + game.millis() + " ms");
        });
    }

    /** Makes each square in turn, a tick at a time, and keeps its numbers. */
    private static final class Bench implements Runnable {

        private final List<Square> squares;
        private int at;

        private Bench(final List<Square> squares) {
            this.squares = squares;
        }

        @Override
        public void run() {
            if (done()) {
                return;
            }
            final Square square = this.squares.get(this.at);
            if (square.tick()) {
                this.at++;
            }
        }

        private boolean done() {
            return this.at >= this.squares.size();
        }

        /* Which square is being made, and how many of its chunks are whole so far. */
        private String progress() {
            if (done()) {
                return "all made";
            }
            final Square square = this.squares.get(this.at);
            return square.name() + ", " + square.whole() + " of " + square.chunks() + " chunks whole";
        }

        private List<Square> squares() {
            return this.squares;
        }
    }

    /** A square of chunks forced into being in {@code level}, round the chunk {@code centre, centre}. */
    private static final class Square {

        private final String name;
        private final ServerLevel level;
        private final int radius;
        private final int centre;
        private long started;
        private long took;
        private long longest;

        private Square(final String name, final ServerLevel level, final int radius, final int centre) {
            this.name = name;
            this.level = level;
            this.radius = radius;
            this.centre = centre;
        }

        /*
         * Starts it on its first tick, then looks whether it is whole, keeping the longest the server worked a tick
         * meanwhile (the tick before this one, the last the server has timed); whether it is done.
         */
        private boolean tick() {
            final long now = System.nanoTime();
            if (this.started == 0) {
                this.started = now;
                force(true);
                return false;
            }
            final MinecraftServer server = this.level.getServer();
            this.longest = Math.max(this.longest, server.getTickTimesNanos()[Math.floorMod(server.getTickCount() - 1,
                    server.getTickTimesNanos().length)]);
            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    if (this.level.getChunkSource().getChunkNow(this.centre + x, this.centre + z) == null) {
                        return false;
                    }
                }
            }
            this.took = now - this.started;
            force(false);
            return true;
        }

        /*
         * Asks for the square, or lets it go, as a ticket and nothing more: the level's own setChunkForced loads each
         * chunk then and there, holding the server's tick until it is made, which is not how the game asks for land.
         */
        private void force(final boolean on) {
            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    this.level.getChunkSource().updateChunkForced(new ChunkPos(this.centre + x, this.centre + z), on);
                }
            }
        }

        private String name() {
            return this.name;
        }

        /* How many of its chunks are whole now. */
        private int whole() {
            int count = 0;
            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    if (this.level.getChunkSource().getChunkNow(this.centre + x, this.centre + z) != null) {
                        count++;
                    }
                }
            }
            return count;
        }

        private int chunks() {
            return (2 * this.radius + 1) * (2 * this.radius + 1);
        }

        private double millis() {
            return this.took / NANOS_A_MILLI;
        }

        private double longestTick() {
            return this.longest / NANOS_A_MILLI;
        }
    }
}
