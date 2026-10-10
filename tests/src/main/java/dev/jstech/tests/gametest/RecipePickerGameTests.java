/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PendingCraftOperation;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import dev.jstech.tests.testkit.CraftingRig;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.grid;
import static dev.jstech.tests.testkit.CraftingFixtures.kilnRig;
import static dev.jstech.tests.testkit.CraftingFixtures.nuggetsToIngot;
import static dev.jstech.tests.testkit.CraftingFixtures.smelt;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * GameTests for the recipes listed for one result and for a craft request that names the recipe the player
 * picked.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipePickerGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private RecipePickerGameTests() {
    }

    /** The recipes that make one result come in a stable order: the machine ones first, then the bench ones. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void recipesFor_listsMachineRecipesThenBenchPatternsForTheResult(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey ingot = storageKey(Items.IRON_INGOT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(rig.hold(NetworkRecipe.ofProcessing(smelt().withName("Blast", ""))),
                            "the processing recipe goes into the interface");
                    helper.assertTrue(rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                                    MultiStagePattern.Stage.proc(smelt()))).withName("Iron line", ""))),
                            "the multi-stage recipe goes into the interface");
                    helper.assertTrue(net.cc().loadPattern(nuggetsToIngot()), "the bench pattern loads");
                    helper.assertTrue(net.cc().loadPattern(new CraftingPattern(grid(Items.OAK_LOG),
                            new ItemStack(Items.OAK_PLANKS, 4))), "an unrelated bench pattern loads");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<NetworkRecipe> recipes = net.mainframe().recipesFor(ingot);
                    helper.assertTrue(recipes.size() == 3, "three recipes make the ingot; got " + recipes.size());
                    helper.assertTrue(recipes.get(0).proc().isPresent()
                                    && recipes.get(0).displayName().equals("Blast"),
                            "the processing recipe comes first; got " + recipes.get(0).displayName());
                    helper.assertTrue(recipes.get(1).multi().isPresent()
                                    && recipes.get(1).displayName().equals("Iron line"),
                            "the multi-stage recipe comes second; got " + recipes.get(1).displayName());
                    helper.assertTrue(recipes.get(2).bench().isPresent(), "the bench pattern comes last");
                    helper.assertTrue(net.mainframe().recipesFor(storageKey(Items.OAK_PLANKS)).size() == 1,
                            "the planks have their one bench pattern");
                    helper.assertTrue(net.mainframe().recipesFor(storageKey(Items.DIAMOND)).isEmpty(),
                            "nothing makes a diamond");
                })
                .thenSucceed();
    }

    /** A craft request that names a recipe runs that recipe: the pipeline, the machine, or the bench pattern's plan. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void submitCraftRequest_runsTheRecipeThePlayerPicked(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey ingot = storageKey(Items.IRON_INGOT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.RAW_IRON, 16);
                    net.seed(Items.IRON_NUGGET, 18);
                    rig.hold(NetworkRecipe.ofProcessing(smelt().withName("Blast", "")));
                    rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(smelt()))).withName("Iron line", "")));
                    net.cc().loadPattern(nuggetsToIngot());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final var multi = net.mainframe().submitCraftRequest(ingot, 1, false, "test", null, 1);
                    helper.assertTrue(multi instanceof NetworkMultiStageOperation,
                            "index 1 runs the pipeline; got " + multi);
                    final var machine = net.mainframe().submitCraftRequest(ingot, 1, false, "test", null, 0);
                    helper.assertTrue(machine instanceof NetworkProcessingOperation,
                            "index 0 runs the machine; got " + machine);
                    final var bench = net.mainframe().submitCraftRequest(ingot, 1, false, "test", null, 2);
                    helper.assertTrue(bench instanceof PendingCraftOperation,
                            "index 2 plans the bench pattern; got " + bench);
                    final var auto = net.mainframe().submitCraftRequest(ingot, 1, false, "test", null, 7);
                    helper.assertTrue(auto instanceof NetworkProcessingOperation,
                            "an index past the list is the machine's own choice, the first machine recipe; got "
                                    + auto);
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(net.storage(helper).count(ingot) >= 4,
                        "the bench plan and the three machine runs make an ingot each; ingots="
                                + net.storage(helper).count(ingot)))
                .thenSucceed();
    }
}
