/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The dev runs carry CC: Tweaked, so the bridge to it is tested against the real thing: the guard sees
 * the mod and the API answers behind it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerCraftGameTests {

    private ComputerCraftGameTests() {
    }

    private static final String ARENA = "empty";

    @GameTest(template = ARENA)
    public static void integration_seesComputerCraftInTheDevRuns(final GameTestHelper helper) {
        helper.assertTrue(ComputerCraftIntegration.isLoaded(), "CC: Tweaked is loaded in the dev runs");
        final String version = ComputerCraftIntegration.installedVersion();
        helper.assertTrue(version.startsWith("1."), "the API names its version; got '" + version + "'");
        helper.succeed();
    }
}
