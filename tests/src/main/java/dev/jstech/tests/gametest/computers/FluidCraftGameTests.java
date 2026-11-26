/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.engine.ICraftPlanning;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.crafting.CraftingPayloads;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * A liquid or a gas a machine makes can be ordered as an item can: the craft catalogue lists it, the planner plans
 * it, and a prompt finds it by its name. Before, a machine recipe whose result was a liquid was left out of the
 * catalogue and {@code iql CRAFT 100 jscore:mercury} answered that no item was called that.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FluidCraftGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* What one run of the test recipe makes, in millibuckets. */
    private static final long WATER_PER_RUN = 250L;

    private FluidCraftGameTests() {
    }

    @GameTest(template = ARENA)
    public static void byName_findsLiquidsAndGasesAsWellAsItems(final GameTestHelper helper) {
        final StorageKey cobblestone = StorageKey.byName("cobblestone");
        helper.assertTrue(cobblestone != null && cobblestone.isItem(), "an item still answers first");
        helper.assertTrue(StorageKey.of(new FluidStack(Fluids.WATER, 1)).equals(StorageKey.byName("minecraft:water")),
                "a liquid answers by its fluid");
        final StorageKey mercury = StorageKey.byName("jscore:mercury");
        helper.assertTrue(mercury != null && mercury.isFluid(), "a material liquid in a room answers as its liquid");
        final StorageKey hydrogen = StorageKey.byName("jscore:hydrogen");
        helper.assertTrue(hydrogen != null && hydrogen.kind() == StorageKey.Kind.GAS,
                "a material that is a gas in a room answers as its gas; got " + hydrogen);
        final StorageKey chlorine = StorageKey.byName("gas|jscore:chlorine");
        helper.assertTrue(chlorine != null && chlorine.kind() == StorageKey.Kind.GAS, "a key's own id names its state");
        helper.assertTrue(StorageKey.byName("jscore:nothing_is_called_this") == null, "nothing answers to nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void catalog_listsAndPlansAMachineRecipeThatMakesALiquid(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final StorageKey water = StorageKey.of(new FluidStack(Fluids.WATER, 1));
        final ProcessingPattern melt = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(Items.ICE), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(water, WATER_PER_RUN, ProcessingPattern.FULL_CHANCE)),
                ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(CraftingRig.direct(world, net.cc(), TestMachines.KILN.get()).hold(melt),
                            "the interface takes the recipe");
                    net.seed(Items.ICE, 8);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<CraftCatalogPayload.Entry> catalog = CraftingPayloads.buildCraftCatalog(
                            helper.getLevel(), net.mainframe().networkUuid());
                    helper.assertTrue(catalog.stream().anyMatch(entry -> entry.result().equals(water)),
                            "the catalogue lists the water the recipe makes; got " + catalog);
                    helper.assertTrue(!net.mainframe().recipesFor(water).isEmpty(), "the network knows a recipe");
                    final ICraftPlanning planner = net.mainframe().networkOperations().planner();
                    helper.assertTrue(planner != null, "the network has an engine to plan with");
                    final Map<StorageKey, Long> stock = net.mainframe().networkIndex().snapshot();
                    final var plan = planner.plan(water, 1000L, net.mainframe().networkPatterns(),
                            net.mainframe().networkProcessingPatterns(), stock);
                    helper.assertTrue(plan.feasible() && plan.produced() >= 1000L,
                            "a bucket of water plans from four runs on the ice in stock; got " + plan);
                })
                .thenSucceed();
    }
}
