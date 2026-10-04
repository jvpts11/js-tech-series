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
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A multi-stage pipeline must size each stage by what the NEXT stage consumes, not by the final quantity:
 * asking for nine nuggets (one ingot each nine) means smelting ONE ingot, not nine. Sizing every stage with
 * the final amount over-produces where a stage multiplies (1 ingot -> 9 nuggets) and starves where it
 * divides (9 ingots -> 1 block).
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiStageQuantityGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private MultiStageQuantityGameTests() {
    }

    @GameTest(template = ARENA)
    public static void stageDemands_followTheYieldOfEachStage(final GameTestHelper helper) {
        final ProcessingPattern smelt = smelt();
        // 1 ingot -> 9 nuggets: nine nuggets need ONE smelted ingot.
        final MultiStagePattern multiply = new MultiStagePattern(List.of(
                MultiStagePattern.Stage.proc(smelt), MultiStagePattern.Stage.bench(nuggetsPattern())));
        final long[] nine = multiply.stageDemands(9);
        helper.assertTrue(nine[0] == 1 && nine[1] == 9, "nine nuggets = 1 ingot then 9 nuggets; got "
                + nine[0] + ", " + nine[1]);
        final long[] ten = multiply.stageDemands(10);
        helper.assertTrue(ten[0] == 2 && ten[1] == 10, "ten nuggets = 2 ingots (two bench runs); got "
                + ten[0] + ", " + ten[1]);
        // 9 ingots -> 1 block: one block needs NINE smelted ingots.
        final MultiStagePattern divide = new MultiStagePattern(List.of(
                MultiStagePattern.Stage.proc(smelt), MultiStagePattern.Stage.bench(blockPattern())));
        final long[] block = divide.stageDemands(1);
        helper.assertTrue(block[0] == 9 && block[1] == 1, "one block = 9 ingots then 1 block; got "
                + block[0] + ", " + block[1]);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void multiStage_smeltsOnlyWhatTheBenchStageConsumes(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final MultiStagePattern multi = new MultiStagePattern(List.of(
                MultiStagePattern.Stage.proc(smelt()), MultiStagePattern.Stage.bench(nuggetsPattern())));
        final NetworkMultiStageOperation[] op = new NetworkMultiStageOperation[1];

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // Only ONE raw iron in the network: a stage sized with the final "9" could never finish.
                    net.seed(Items.RAW_IRON, 1);
                    rig.hold(NetworkRecipe.ofMultiStage(multi));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op[0] = net.mainframe().submitNetworkMultiStage(multi, 9, "test");
                    helper.assertTrue(op[0] != null, "the pipeline is accepted");
                })
                .thenExecuteAfter(120, () -> {
                    helper.assertTrue(op[0].isDone() && op[0].toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "the pipeline must complete with a single smelted ingot; status="
                                    + op[0].toRecord().status() + " active="
                                    + net.mainframe().activeOperationRecords());
                    helper.assertTrue(rig.machine() != null && rig.machine().made() == 1,
                            "the kiln smelted exactly one ingot");
                    final long nuggets = net.storage(helper.getLevel()).count(StorageKey.of(Items.IRON_NUGGET));
                    helper.assertTrue(nuggets >= 9, "nine nuggets must reach storage; got " + nuggets);
                })
                .thenSucceed();
    }

    private static ProcessingPattern smelt() {
        return CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200);
    }

    /** 1 iron ingot -> 9 iron nuggets (the vanilla shapeless recipe). */
    private static CraftingPattern nuggetsPattern() {
        final List<ItemStack> grid = emptyGrid();
        grid.set(0, new ItemStack(Items.IRON_INGOT));
        return new CraftingPattern(grid, new ItemStack(Items.IRON_NUGGET, 9));
    }

    /** 9 iron ingots -> 1 iron block. */
    private static CraftingPattern blockPattern() {
        final List<ItemStack> grid = emptyGrid();
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.set(i, new ItemStack(Items.IRON_INGOT));
        }
        return new CraftingPattern(grid, new ItemStack(Items.IRON_BLOCK));
    }

    private static List<ItemStack> emptyGrid() {
        final List<ItemStack> grid = new ArrayList<>(CraftingPattern.GRID_SIZE);
        for (int i = 0; i < CraftingPattern.GRID_SIZE; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
