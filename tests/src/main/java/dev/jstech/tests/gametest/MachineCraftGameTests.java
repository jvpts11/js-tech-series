/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.crafting.ProcessingPattern.ProcessingInput;
import dev.jstech.computers.crafting.ProcessingPattern.ProcessingOutput;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the machine-crafting data model: processing and multi-stage patterns serialize to and from
 * {@code .craft} without loss (chances and timeout survive), and go into a Crafting Interface each once.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineCraftGameTests {

    private static final String ARENA = "empty";

    private MachineCraftGameTests() {
    }

    @GameTest(template = ARENA)
    public static void processingPattern_roundTripsAndGoesIntoAnInterface(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();
        final ProcessingPattern pattern = new ProcessingPattern(
                List.of(new ProcessingInput(StorageKey.of(Items.IRON_INGOT), 1L)),
                List.of(new ProcessingOutput(StorageKey.of(Items.COPPER_INGOT), 2L, 100),
                        new ProcessingOutput(StorageKey.of(Items.GOLD_NUGGET), 1L, 50)),
                200);

        final String snbt = CraftFile.serializeProcessing(pattern, registries).orElseThrow();
        helper.assertTrue("proc".equals(CraftFile.typeOf(snbt)),
                "the tagged type is proc; got " + CraftFile.typeOf(snbt));
        final ProcessingPattern parsed = CraftFile.parseProcessing(snbt, registries).orElseThrow();
        helper.assertTrue(parsed.sameRecipe(pattern), "the .craft round-trip preserves the recipe");
        helper.assertTrue(parsed.outputs().get(1).chancePercent() == 50, "the 50% output chance survives");
        helper.assertTrue(parsed.timeoutTicks() == 200, "the timeout survives");

        final CraftingInterfacePart part = new CraftingInterfacePart(HardwareEra.STANDARD);
        final NetworkRecipe recipe = NetworkRecipe.ofProcessing(parsed);
        helper.assertTrue(part.place(recipe), "the read pattern goes into an interface");
        helper.assertTrue(part.patterns().size() == 1, "the interface holds one pattern");
        helper.assertFalse(part.place(NetworkRecipe.ofProcessing(pattern)),
                "the same recipe, read or not, goes in once");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void multiStagePattern_roundTrips(final GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();
        final ProcessingPattern proc = new ProcessingPattern(
                List.of(new ProcessingInput(StorageKey.of(Items.IRON_INGOT), 1L)),
                List.of(new ProcessingOutput(StorageKey.of(Items.COPPER_INGOT), 1L, 100)),
                150);
        final MultiStagePattern multi = new MultiStagePattern(List.of(MultiStagePattern.Stage.proc(proc)));

        final String snbt = CraftFile.serializeMultiStage(multi, registries).orElseThrow();
        helper.assertTrue("multi".equals(CraftFile.typeOf(snbt)), "the tagged type is multi");
        final MultiStagePattern parsed = CraftFile.parseMultiStage(snbt, registries).orElseThrow();
        helper.assertTrue(parsed.stages().size() == 1, "one stage survives");
        helper.assertTrue(parsed.stages().get(0).isProcessing(), "the stage is a processing stage");
        helper.assertTrue(parsed.stages().get(0).proc().orElseThrow().timeoutTicks() == 150,
                "the stage's timeout survives");
        helper.succeed();
    }
}
