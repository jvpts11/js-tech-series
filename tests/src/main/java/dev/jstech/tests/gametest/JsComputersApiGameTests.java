/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.api.JsComputersApi;
import dev.jstech.computers.os.GraphicsPrograms;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The registration calls an addon makes: a program named as graphical is known as one on the side that counts video
 * memory, and a missing argument is ignored the way it is across the rest of the API.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class JsComputersApiGameTests {

    private static final String ARENA = "empty";

    private JsComputersApiGameTests() {
    }

    @GameTest(template = ARENA)
    public static void registerGraphicsProgram_marksTheProgramAsHoldingVideoMemory(final GameTestHelper helper) {
        final ResourceLocation program = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "graphics_probe");

        helper.assertTrue(!GraphicsPrograms.isGraphicalProgram(program), "not graphical before it is named");
        JsComputersApi.registerGraphicsProgram(program);

        helper.assertTrue(GraphicsPrograms.isGraphicalProgram(program), "graphical once the common side names it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    @SuppressWarnings("removal")
    public static void register_ignoresAMissingArgument(final GameTestHelper helper) {
        JsComputersApi.registerIsa(null);
        JsComputersApi.registerArchitecture(null);
        JsComputersApi.registerGraphicsProgram(null);

        helper.succeed();
    }
}
