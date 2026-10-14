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
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.slf4j.Logger;

/**
 * Runs only some of the GameTests, so a change is proved by the tests that cover it without waiting for all of them.
 * {@code -PgtMods=core,industrial} keeps the suites of those mods ({@link TestSuites}). {@code
 * -PgtOnly=DockStationGameTests,fs_append} keeps every test of a named class, and every test whose name holds one of
 * the other words. Given both, a test runs when it is in one of the suites and is named; with neither, every test runs.
 *
 * <p>The game test server takes its own copy of the tests when it is made and groups that copy into batches only as it
 * starts, so the copy is narrowed in between, as the server is about to start.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class GameTestSelection {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ONLY = "jsc.gametests.only";
    private static final String MODS = "jsc.gametests.mods";

    private GameTestSelection() {
    }

    @SubscribeEvent
    public static void onAboutToStart(final ServerAboutToStartEvent event) {
        final String only = System.getProperty(ONLY, "").strip();
        final String mods = System.getProperty(MODS, "").strip();
        if ((only.isEmpty() && mods.isEmpty()) || !(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        final List<TestFunction> tests;
        try {
            final Field field = GameTestServer.class.getDeclaredField("testFunctions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<TestFunction> found = (List<TestFunction>) field.get(server);
            tests = found;
        } catch (final ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[JSC-GT] could not narrow the GameTests to {} {}; running all of them", mods, only, e);
            return;
        }
        final int all = tests.size();
        final Map<String, Class<?>> holders = holders();
        // A mistyped suite stops the server: a smaller battery, or none, would end on "All 0 required tests passed".
        final Set<String> suites = TestSuites.asked(mods);
        final Set<String> inSuites = new HashSet<>();
        for (final Class<?> holder : holders.values()) {
            if (suites.contains(TestSuites.of(holder))) {
                addTests(holder, inSuites);
            }
        }
        final Set<String> names = new HashSet<>();
        final Set<String> words = new HashSet<>();
        for (final String entry : only.split(",")) {
            final String word = entry.strip();
            if (word.isEmpty()) {
                continue;
            }
            final Class<?> holder = holders.get(word);
            if (holder != null) {
                addTests(holder, names);
            } else {
                words.add(word.toLowerCase(Locale.ROOT));
            }
        }
        tests.removeIf(test -> !(suites.isEmpty() || inSuites.contains(test.testName()))
                || !(only.isEmpty() || names.contains(test.testName())
                        || words.stream().anyMatch(test.testName()::contains)));
        if (tests.isEmpty()) {
            throw new IllegalStateException("no GameTest is in the suites [" + mods + "] and named [" + only
                    + "], so the run would prove nothing");
        }
        LOGGER.info("[JSC-GT] running {} of {} GameTests for suites [{}] and names [{}]", tests.size(), all, mods,
                only);
    }

    /* Every GameTest holder of the tests mod, by its simple name, whichever suite's package it sits in. */
    private static Map<String, Class<?>> holders() {
        final Map<String, Class<?>> found = new HashMap<>();
        final List<String> classNames = ModList.get().getModFileById(JsTests.MODID).getFile().getScanResult()
                .getAnnotatedBy(GameTestHolder.class, ElementType.TYPE)
                .map(annotation -> annotation.clazz().getClassName()).toList();
        for (final String className : classNames) {
            try {
                final Class<?> holder = Class.forName(className, false, GameTestSelection.class.getClassLoader());
                found.put(holder.getSimpleName(), holder);
            } catch (final ClassNotFoundException missing) {
                LOGGER.warn("[JSC-GT] the GameTest holder {} could not be loaded", className, missing);
            }
        }
        return found;
    }

    /* Adds the names of every GameTest the class holds, as the server names them. */
    private static void addTests(final Class<?> holder, final Set<String> names) {
        for (final Method method : holder.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GameTest.class)) {
                names.add(method.getName().toLowerCase(Locale.ROOT));
            }
        }
    }
}
