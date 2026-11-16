/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.JsCore;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Core's basic lines and battery, its pipes, its cable and its battery bank, stay made by the Core's own recipes
 * with J's Industrial installed, while the setting that hands them to J's Industrial's own recipes is off, as it comes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BasicLineRecipesGameTests {

    private static final String ARENA = "empty";
    private static final List<String> BASIC = List.of("copper_pipe", "iron_gas_pipe", "vacuum_jacketed_pipe",
            "insulated_copper_cable", "lead_acid_battery_bank");

    private BasicLineRecipesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void basicLines_standWithIndustrialInstalledWhileItsOwnRecipesAreOff(final GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("jsindustrial"), "J's Industrial is installed in the test server");
        final List<String> missing = BASIC.stream().filter(name -> helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name)).isEmpty()).toList();
        helper.assertTrue(missing.isEmpty(), "each is made by the Core's own recipe: missing " + missing);
        helper.succeed();
    }
}
