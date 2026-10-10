/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import com.mojang.logging.LogUtils;
import dev.jstech.tests.JsTests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * Runs only some of the GameTests, so a change is proved by the tests that cover it without waiting for all of them.
 * {@code -PgtOnly=DockStationGameTests,fs_append} keeps every test of a named class under {@code gametest}, and every
 * test whose name holds one of the other words; with nothing given every test runs.
 *
 * <p>The game test server takes its own copy of the tests when it is made and groups that copy into batches only as it
 * starts, so the copy is narrowed in between, as the server is about to start.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class GameTestSelection {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ONLY = "jsc.gametests.only";
    private static final String TEST_PACKAGE = "dev.jstech.tests.gametest.";

    private GameTestSelection() {
    }

    @SubscribeEvent
    public static void onAboutToStart(final ServerAboutToStartEvent event) {
        final String only = System.getProperty(ONLY, "").strip();
        if (only.isEmpty() || !(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        final Set<String> names = new HashSet<>();
        final Set<String> words = new HashSet<>();
        for (final String entry : only.split(",")) {
            final String word = entry.strip();
            if (!word.isEmpty() && !addClass(word, names)) {
                words.add(word.toLowerCase(Locale.ROOT));
            }
        }
        try {
            final Field field = GameTestServer.class.getDeclaredField("testFunctions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<TestFunction> tests = (List<TestFunction>) field.get(server);
            final int all = tests.size();
            tests.removeIf(test -> !names.contains(test.testName())
                    && words.stream().noneMatch(test.testName()::contains));
            LOGGER.info("[JSC-GT] running {} of {} GameTests for {}", tests.size(), all, only);
        } catch (final ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[JSC-GT] could not narrow the GameTests to {}; running all of them", only, e);
        }
    }

    /* Adds the test names of the class so named, if there is one; false when the word names no test class. */
    private static boolean addClass(final String simpleName, final Set<String> names) {
        try {
            final Class<?> holder = Class.forName(TEST_PACKAGE + simpleName, false,
                    GameTestSelection.class.getClassLoader());
            for (final Method method : holder.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GameTest.class)) {
                    names.add(method.getName().toLowerCase(Locale.ROOT));
                }
            }
            return true;
        } catch (final ClassNotFoundException notAClass) {
            return false;
        }
    }
}
