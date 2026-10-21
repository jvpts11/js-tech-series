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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The stall monitor of a long run of GameTests: a thread of its own looks at the server every second, and when the
 * server has not finished a tick for a while (a minute unless {@code -Djstestkit.stall_seconds} says), it writes
 * once, to the log and to {@code jstestkit/stall-<n>.txt} beside the game, the stack of every thread and the tests
 * running then, so a run that hangs says where, while it still hangs.
 */
public final class StallMonitor {

    /** The system property that sets how long the server may stand still, in seconds. */
    public static final String SECONDS_PROPERTY = "jstestkit.stall_seconds";
    /** How long the server may stand still when the run does not say, in seconds. */
    public static final int DEFAULT_SECONDS = 60;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOOK_EVERY_MILLIS = 1_000L;
    private static final int FRAMES = 60;

    private static volatile Thread watcher;

    private StallMonitor() {
    }

    /** Hands the game's bus the start and the end of a run of GameTests, once, from the kit's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            if (event.getServer() instanceof GameTestServer server) {
                start(server);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> stop());
    }

    /** What a stall's report says: how long the server stood still, the tests running, every thread's stack. */
    public static String report(final long stillSeconds, final Map<String, Long> running,
                                final Map<Thread, StackTraceElement[]> threads) {
        final StringBuilder out = new StringBuilder("The server has not finished a tick for ").append(stillSeconds)
                .append(" s");
        final long now = System.nanoTime();
        out.append(System.lineSeparator()).append("Running then:");
        if (running.isEmpty()) {
            out.append(" no test");
        }
        running.forEach((name, began) -> out.append(System.lineSeparator()).append("  ").append(name)
                .append(", for ").append(TimeUnit.NANOSECONDS.toSeconds(now - began)).append(" s"));
        for (final Map.Entry<Thread, StackTraceElement[]> each : threads.entrySet()) {
            out.append(System.lineSeparator()).append(System.lineSeparator()).append('"')
                    .append(each.getKey().getName()).append("\" ").append(each.getKey().getState());
            final StackTraceElement[] stack = each.getValue();
            for (int i = 0; i < Math.min(FRAMES, stack.length); i++) {
                out.append(System.lineSeparator()).append("    at ").append(stack[i]);
            }
        }
        return out.toString();
    }

    private static void start(final MinecraftServer server) {
        final int seconds = Math.max(1, Integer.getInteger(SECONDS_PROPERTY, DEFAULT_SECONDS));
        final Thread thread = new Thread(() -> watch(server, seconds), "J's Core Test Kit stall monitor");
        thread.setDaemon(true);
        watcher = thread;
        thread.start();
    }

    private static void stop() {
        final Thread thread = watcher;
        watcher = null;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static void watch(final MinecraftServer server, final int seconds) {
        int lastTick = server.getTickCount();
        long stillSince = System.nanoTime();
        boolean said = false;
        int stalls = 0;
        while (watcher == Thread.currentThread()) {
            try {
                Thread.sleep(LOOK_EVERY_MILLIS);
            } catch (final InterruptedException e) {
                return;
            }
            final int tick = server.getTickCount();
            if (tick != lastTick) {
                lastTick = tick;
                stillSince = System.nanoTime();
                said = false;
                continue;
            }
            final long still = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - stillSince);
            if (!said && still >= seconds) {
                said = true;
                stalls++;
                write(server, stalls, report(still, GameTestTimes.running(), Thread.getAllStackTraces()));
            }
        }
    }

    private static void write(final MinecraftServer server, final int stall, final String report) {
        LOGGER.error(report);
        final Path file = server.getServerDirectory().resolve("jstestkit").resolve("stall-" + stall + ".txt");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, report, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOGGER.warn("The stall's report could not be written to {}", file, e);
        }
    }
}
