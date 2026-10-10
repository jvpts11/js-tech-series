/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPress;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** How a processing recipe finds the tanks that hold its fluids. */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProcessingRecipeGameTests {

    private static final String ARENA = "empty";

    private ProcessingRecipeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tanksFor_backtracksWhenAnEarlierInputTookTheTankALaterOneNeeds(final GameTestHelper helper) {
        final ProcessingRecipe recipe = ProcessingRecipeBuilder.of(TestPress.PRESSING)
                .fluidInput(Fluids.WATER, 100).fluidInput(Fluids.WATER, 500)
                .output(Items.STONE, 1).build();
        final ProcessingInput input = new ProcessingInput(List.of(),
                List.of(new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.WATER, 200)));

        final int[] tanks = recipe.tanksFor(input);

        helper.assertTrue(tanks != null && tanks[0] == 1 && tanks[1] == 0,
                "the small input goes to the small tank so the large one fits the large tank");
        helper.succeed();
    }
}
