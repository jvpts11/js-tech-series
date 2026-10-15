/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.optional.OptionalMod;
import dev.jstech.tests.JsTests;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Another mod behind its guard, in the game: known as installed or not by the game's list of mods, its adapter never
 * reached when it is absent, and an adapter that breaks as it starts left out with the game going on.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OptionalModGameTests {

    private static final String ARENA = "empty";

    private OptionalModGameTests() {
    }

    @GameTest(template = ARENA)
    public static void isLoaded_followsTheGamesListOfMods(final GameTestHelper helper) {
        helper.assertTrue(OptionalMod.of(JsTests.MODID).isLoaded(), "the test mod is installed");
        helper.assertTrue(!OptionalMod.of("no_such_mod_here").isLoaded(), "a mod nobody made is not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ifLoaded_neverReachesTheAdapterOfAnAbsentMod(final GameTestHelper helper) {
        final AtomicBoolean reached = new AtomicBoolean();

        helper.assertTrue(!OptionalMod.of("no_such_mod_here").ifLoaded(() -> {
            reached.set(true);
            return () -> { };
        }) && !reached.get(), "the adapter of a mod that is not installed is never asked for");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ifLoaded_leavesOutAnAdapterThatBreaks(final GameTestHelper helper) {
        final boolean started = OptionalMod.of(JsTests.MODID).ifLoaded(() -> () -> {
            throw new NoClassDefFoundError("the other mod changed under its adapter");
        });

        helper.assertTrue(!started, "an adapter that breaks as it starts is left out, and the game goes on");
        helper.succeed();
    }
}
