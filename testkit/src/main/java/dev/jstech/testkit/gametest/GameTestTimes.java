/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The report of time of a run of GameTests: how long each test took, written as the run ends to
 * {@code jstestkit/gametest-times.txt} beside the game, the slowest first, and the slowest few to the log; and which
 * tests are running now, for the {@link StallMonitor} to name when the server stops moving.
 */
public final class GameTestTimes {

    /** Where the report is written, under the game's folder. */
    public static final String REPORT = "jstestkit/gametest-times.txt";
    /** How many of the slowest tests the log names. */
    public static final int LOGGED = 10;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Long> RUNNING = new ConcurrentHashMap<>();
    private static final List<Timed> TIMED = new CopyOnWriteArrayList<>();
    private static final double MILLIS_PER_SECOND = 1_000.0;

    private GameTestTimes() {
    }

    /** Hands the game's bus the start and the end of a run of GameTests, once, from the kit's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, event -> {
            if (event.getServer() instanceof GameTestServer server) {
                watch(server);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> {
            if (event.getServer() instanceof GameTestServer) {
                report(event.getServer());
            }
        });
    }

    /** The tests running now, by name, each with the moment it began, in nanoseconds. */
    public static Map<String, Long> running() {
        return Map.copyOf(RUNNING);
    }

    /** Every test that ended, with how long it took, in the order they ended. */
    public static List<Timed> timed() {
        return List.copyOf(TIMED);
    }

    /** The report's lines: the tests that ended, the slowest first, and what they took in all. */
    public static List<String> lines(final List<Timed> timed) {
        final List<Timed> slowest = new ArrayList<>(timed);
        slowest.sort(Comparator.comparingLong(Timed::millis).reversed().thenComparing(Timed::name));
        long total = 0L;
        for (final Timed each : timed) {
            total += each.millis();
        }
        final List<String> out = new ArrayList<>();
        out.add(timed.size() + " tests took " + total / MILLIS_PER_SECOND + " s between them");
        for (final Timed each : slowest) {
            out.add(each.millis() + " ms  " + (each.passed() ? "passed" : "FAILED") + "  " + each.name());
        }
        return out;
    }

    /* Listens to every test of the run, before the run begins. */
    private static void watch(final GameTestServer server) {
        final Listener listener = new Listener();
        for (final GameTestBatch batch : batchesOf(server)) {
            for (final GameTestInfo test : batch.gameTestInfos()) {
                test.addListener(listener);
            }
        }
    }

    private static void report(final MinecraftServer server) {
        final List<Timed> timed = timed();
        if (timed.isEmpty()) {
            return;
        }
        final List<String> lines = lines(timed);
        final Path file = server.getServerDirectory().resolve(REPORT);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOGGER.warn("The report of the GameTests' times could not be written to {}", file, e);
        }
        LOGGER.info("The slowest GameTests of the run, of {} (all in {}):", timed.size(), file);
        for (final String line : lines.subList(1, Math.min(lines.size(), LOGGED + 1))) {
            LOGGER.info("  {}", line);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<GameTestBatch> batchesOf(final GameTestServer server) {
        try {
            final Field field = GameTestServer.class.getDeclaredField("testBatches");
            field.setAccessible(true);
            return (List<GameTestBatch>) field.get(server);
        } catch (final ReflectiveOperationException e) {
            LOGGER.warn("The GameTests' batches could not be read; their times are not kept", e);
            return List.of();
        }
    }

    /**
     * One test that ended.
     *
     * @param name   its name
     * @param millis how long it ran
     * @param passed whether it passed
     */
    public record Timed(String name, long millis, boolean passed) {
    }

    /* What a test tells as it goes: when its structure is placed it begins, and it ends passed or failed. */
    private static final class Listener implements GameTestListener {

        @Override
        public void testStructureLoaded(final GameTestInfo test) {
            RUNNING.put(test.getTestName(), System.nanoTime());
        }

        @Override
        public void testPassed(final GameTestInfo test, final GameTestRunner runner) {
            ended(test, true);
        }

        @Override
        public void testFailed(final GameTestInfo test, final GameTestRunner runner) {
            ended(test, false);
        }

        @Override
        public void testAddedForRerun(final GameTestInfo was, final GameTestInfo again, final GameTestRunner runner) {
            again.addListener(this);
        }

        private static void ended(final GameTestInfo test, final boolean passed) {
            RUNNING.remove(test.getTestName());
            TIMED.add(new Timed(test.getTestName(), test.getRunTime(), passed));
        }
    }
}
