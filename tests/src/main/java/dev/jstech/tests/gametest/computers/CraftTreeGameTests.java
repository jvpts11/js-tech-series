/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PendingCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.CraftingFixtures.Network;
import dev.jstech.tests.testkit.computers.CraftingRig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.computers.CraftingFixtures.kilnRig;
import static dev.jstech.tests.testkit.computers.CraftingFixtures.smelt;
import static dev.jstech.tests.testkit.computers.CraftingFixtures.storageKey;

/**
 * A craft's tree: where several recipes make one thing the planner takes the one that closes, a shortage is named by
 * the raw thing nothing makes, and a request whose tree does not close fails saying so instead of running its last
 * step on what it has.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftTreeGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final String SHORT_OF = "jsc.operation.failure.short_of";

    private CraftTreeGameTests() {
    }

    /**
     * Ingots from dust, dust ground from ingots, ingots from raw ore: the first way to an ingot loops back on itself,
     * so the planner makes the ingot from the ore, whether the ingot is asked for or the dust made of it.
     */
    @GameTest(template = ARENA)
    public static void plan_takesAnotherWayWhereTheFirstLoopsBackOnItself(final GameTestHelper helper) {
        final ProcessingPattern fromDust = CraftingRig.pattern(Items.SUGAR, Items.IRON_INGOT, 200);
        final ProcessingPattern grind = CraftingRig.pattern(Items.IRON_INGOT, Items.SUGAR, 200);
        final ProcessingPattern fromOre = CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200);
        final List<ProcessingPattern> machines = List.of(fromDust, grind, fromOre);
        final Map<StorageKey, Long> stock = Map.of(storageKey(Items.RAW_IRON), 4L);
        final CraftPlanner.Plan ingots = CraftPlanner.plan(storageKey(Items.IRON_INGOT), 2, List.of(), machines, stock);
        helper.assertTrue(ingots.feasible(), "two ingots come from the ore; missing " + ingots.missing());
        helper.assertTrue(ingots.steps().size() == 1 && ingots.steps().getFirst().machine() == fromOre,
                "made by the ore's recipe alone; steps " + ingots.steps().size());
        final CraftPlanner.Plan dust = CraftPlanner.plan(storageKey(Items.SUGAR), 2, List.of(), machines, stock);
        helper.assertTrue(dust.feasible(), "the dust is ground from ingots made of the ore; missing " + dust.missing());
        helper.succeed();
    }

    /**
     * A machine that gives two things each run, as a Packaging Line gives a chip's plain dies and a class of them, is
     * a way to either: asked for the second, it runs as often as the second needs; and a recipe taking both runs it
     * once, the second taken from what the first run gave beside its result.
     */
    @GameTest(template = ARENA)
    public static void plan_makesWhatAMachineGivesBesideItsResult(final GameTestHelper helper) {
        final ProcessingPattern sorter = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(storageKey(Items.RAW_IRON), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(storageKey(Items.IRON_INGOT), 8L,
                                ProcessingPattern.FULL_CHANCE),
                        new ProcessingPattern.ProcessingOutput(storageKey(Items.GOLD_INGOT), 8L,
                                ProcessingPattern.FULL_CHANCE)), ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
        final ProcessingPattern both = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(storageKey(Items.IRON_INGOT), 8L),
                        new ProcessingPattern.ProcessingInput(storageKey(Items.GOLD_INGOT), 8L)),
                List.of(new ProcessingPattern.ProcessingOutput(storageKey(Items.DIAMOND), 1L,
                        ProcessingPattern.FULL_CHANCE)), ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
        final List<ProcessingPattern> machines = List.of(sorter, both);
        final Map<StorageKey, Long> stock = Map.of(storageKey(Items.RAW_IRON), 3L);
        final CraftPlanner.Plan gold = CraftPlanner.plan(storageKey(Items.GOLD_INGOT), 20, List.of(), machines, stock);
        helper.assertTrue(gold.feasible() && gold.steps().size() == 1 && gold.steps().getFirst().machine() == sorter
                && gold.steps().getFirst().runs() == 3, "twenty of the second output are three runs of the sorter; "
                + "missing " + gold.missing() + ", steps " + gold.steps());
        final CraftPlanner.Plan diamond = CraftPlanner.plan(storageKey(Items.DIAMOND), 1, List.of(), machines,
                Map.of(storageKey(Items.RAW_IRON), 1L));
        helper.assertTrue(diamond.feasible() && diamond.steps().size() == 2
                && diamond.steps().getFirst().runs() == 1, "one ore sorted once gives both the recipe takes; missing "
                + diamond.missing() + ", steps " + diamond.steps());
        helper.succeed();
    }

    /**
     * A liquid a machine gives beside its result is made by running that machine when the stock is short of it, as
     * coal tar comes off the Coke Oven beside the coke: a phenol order with a quarter of its tar in stock said nothing
     * on the network made the rest.
     */
    @GameTest(template = ARENA)
    public static void plan_makesALiquidAMachineGivesBesideItsResult(final GameTestHelper helper) {
        final StorageKey tar = StorageKey.of(new FluidStack(Fluids.WATER, 1));
        final ProcessingPattern coke = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(storageKey(Items.COAL), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(storageKey(Items.CHARCOAL), 1L,
                                ProcessingPattern.FULL_CHANCE),
                        new ProcessingPattern.ProcessingOutput(tar, 250L, ProcessingPattern.FULL_CHANCE)),
                ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
        final ProcessingPattern phenol = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(tar, 1000L)),
                List.of(new ProcessingPattern.ProcessingOutput(storageKey(Items.SLIME_BALL), 1L,
                        ProcessingPattern.FULL_CHANCE)), ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
        final Map<StorageKey, Long> stock = Map.of(tar, 250L, storageKey(Items.COAL), 4L);
        final CraftPlanner.Plan plan = CraftPlanner.plan(storageKey(Items.SLIME_BALL), 1, List.of(),
                List.of(coke, phenol), stock);
        helper.assertTrue(plan.feasible(), "the rest of the tar comes from the coke oven; missing " + plan.missing());
        helper.assertTrue(plan.steps().size() == 2 && plan.steps().getFirst().machine() == coke
                && plan.steps().getFirst().runs() == 3, "three runs of coke give the 750 mB short; steps "
                + plan.steps());
        helper.succeed();
    }

    /** A plan short of a made thing names the raw thing behind it that nothing makes, apart from all it is short of. */
    @GameTest(template = ARENA)
    public static void plan_namesTheRawThingTheShortageComesDownTo(final GameTestHelper helper) {
        final List<ItemStack> nine = new ArrayList<>();
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            nine.add(new ItemStack(Items.IRON_INGOT));
        }
        final CraftingPattern block = new CraftingPattern(nine, new ItemStack(Items.IRON_BLOCK));
        final Map<StorageKey, Long> stock = Map.of(storageKey(Items.RAW_IRON), 2L);
        final CraftPlanner.Plan plan = CraftPlanner.plan(storageKey(Items.IRON_BLOCK), 1, List.of(block),
                List.of(smelt()), stock);
        helper.assertFalse(plan.feasible(), "two ores make no block of nine ingots");
        helper.assertTrue(plan.missing().containsKey(storageKey(Items.IRON_INGOT)),
                "the ingots are among what it falls short of; missing " + plan.missing());
        helper.assertTrue(plan.unmakeable().equals(Map.of(storageKey(Items.RAW_IRON), 7L)),
                "the raw shortage is seven ores, which nothing makes; unmakeable " + plan.unmakeable());
        helper.succeed();
    }

    /**
     * Ingots asked for from a smelter with no ore on the network: no bare run of the smelter starving for ore, but a
     * failure that says the network is short of the ore and nothing makes it.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void request_aTreeThatDoesNotCloseFailsNamingWhatItIsShortOf(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<INetworkOperation> asked = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> rig.hold(NetworkRecipe.ofProcessing(smelt())))
                .thenExecuteAfter(SETTLE, () -> {
                    final INetworkOperation op = net.mainframe().submitCraftRequest(storageKey(Items.IRON_INGOT), 4,
                            false, "test", null);
                    helper.assertTrue(op instanceof PendingCraftOperation,
                            "the whole tree is planned, not the smelter run bare; got " + op);
                    asked.set(op);
                })
                .thenWaitUntil(() -> helper.assertTrue(asked.get().isDone(), "the request settles"))
                .thenExecute(() -> {
                    final OperationRecord record = asked.get().toRecord();
                    helper.assertTrue(record.status() == OperationRecord.STATUS_FAILED,
                            "it fails; status " + record.status());
                    helper.assertTrue(record.cause().key().equals(SHORT_OF)
                                    && record.cause().arguments().contains("Raw Iron"),
                            "it says it is short of raw iron, which nothing makes; cause " + record.cause());
                })
                .thenSucceed();
    }
}
