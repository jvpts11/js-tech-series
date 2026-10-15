/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.task.Task;
import dev.jstech.core.task.Tasks;
import dev.jstech.tests.JsTests;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Work the server does away from its tick: done on a thread of its own, its result handed back on the server's thread,
 * and nothing handed back once it is cancelled.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TaskGameTests {

    private static final String ARENA = "empty";
    private static final int CANCEL_WAIT_TICKS = 20;
    private static final long SLOW_MILLIS = 300;

    private TaskGameTests() {
    }

    @GameTest(template = ARENA)
    public static void onServer_handsTheResultBackOnTheServersThread(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final AtomicBoolean workedAway = new AtomicBoolean();
        final AtomicBoolean handedOnServer = new AtomicBoolean();
        final AtomicInteger result = new AtomicInteger();
        Tasks.onServer(() -> {
            workedAway.set(!server.isSameThread());
            return 6 * 7;
        }, value -> {
            handedOnServer.set(server.isSameThread());
            result.set(value);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(result.get() == 42, "the result came back");
            helper.assertTrue(workedAway.get(), "the work ran away from the server's thread");
            helper.assertTrue(handedOnServer.get(), "and its result came back on it");
        });
    }

    @GameTest(template = ARENA)
    public static void cancel_handsNothingBack(final GameTestHelper helper) {
        final AtomicBoolean handed = new AtomicBoolean();
        final Task<Integer> task = Tasks.onServer(() -> {
            Thread.sleep(SLOW_MILLIS);
            return 1;
        }, value -> handed.set(true));
        task.cancel();

        helper.runAfterDelay(CANCEL_WAIT_TICKS, () -> {
            helper.assertTrue(!handed.get() && task.cancelled() && task.over(), "a cancelled task hands nothing back");
            helper.succeed();
        });
    }
}
