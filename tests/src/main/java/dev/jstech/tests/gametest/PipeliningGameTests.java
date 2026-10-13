/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The craft engine is concurrent by design: a chain of machine steps pipelines instead of running one stage
 * fully before the next. This mirrors the setup a player builds to speed up a chain: two Metallurgic Infusers, each
 * with a Crafting Interface holding the recipe of its own stage (copper to infused alloy on the first, infused alloy
 * to reinforced on the second), on a Mainframe with a GPU (two queues). Requesting the end of the chain must run BOTH
 * infusers at the same time: the second stage starts as soon as the first has made one, not after the first has made
 * them all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PipeliningGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = MekanismRig.SETTLE;
    private static final ResourceLocation INFUSER = MekanismRig.mek("metallurgic_infuser");
    private static final ResourceLocation ALLOY_INFUSED = MekanismRig.mek("alloy_infused");
    private static final ResourceLocation ALLOY_REINFORCED = MekanismRig.mek("alloy_reinforced");
    private static final ResourceLocation DUST_DIAMOND = MekanismRig.mek("dust_diamond");

    private PipeliningGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 2000)
    public static void twoStageChainRunsBothInfusersAtOnce(final GameTestHelper helper) {
        final MekanismRig.Rig rig = MekanismRig.build(helper, INFUSER);
        final TestWorldBuilder world = rig.world();
        MekanismRig.placeSecondMachine(world, INFUSER);
        rig.net().mainframe().getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        final StorageKey reinforced = MekanismRig.itemKey(ALLOY_REINFORCED);
        final StorageKey infused = MekanismRig.itemKey(ALLOY_INFUSED);
        final INetworkOperation[] op = new INetworkOperation[1];
        final boolean[] sawBothRunning = {false};
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    MekanismRig.mountBuses(world);
                    MekanismRig.mountBottomInputRouter(world);
                    MekanismRig.mountSecondMachineBuses(world, null, null);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // Enough copper, redstone and diamond for four reinforced alloys, sized to the unit.
                    rig.net().seed(Items.COPPER_INGOT, 4);
                    rig.net().seed(Items.REDSTONE, 4);
                    rig.net().seed(MekanismRig.item(DUST_DIAMOND), 8);
                    // Each infuser's interface holds its own stage, so each stage runs on its own machine.
                    MekanismRig.holdFirst(world, NetworkRecipe.ofProcessing(infusedAlloy()));
                    MekanismRig.holdSecond(world, NetworkRecipe.ofProcessing(reinforcedAlloy()));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(rig.net().mainframe().parallelQueues() == 2, "the GPU must give two queues");
                    op[0] = rig.net().mainframe().submitNetworkCraft(reinforced, 4, false, "chain", null);
                    helper.assertTrue(op[0] != null, "the reinforced-alloy chain must be planned");
                })
                .thenWaitUntil(() -> {
                    powerBoth(helper);
                    if (infuserStepsRunning(rig.net().mainframe()) >= 2) {
                        sawBothRunning[0] = true;
                    }
                    helper.assertTrue(op[0].isDone(), "the chain is still running: "
                            + rig.net().mainframe().activeOperationRecords());
                })
                .thenExecute(() -> {
                    helper.assertTrue(op[0].toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "the chain must complete; status=" + op[0].toRecord().status());
                    helper.assertTrue(sawBothRunning[0],
                            "the two infuser stages must have run at the same time: the second must start before the "
                                    + "first has made every infused alloy (a pipelined chain, not one stage after "
                                    + "another)");
                    final NetworkStorage storage = rig.net().storage(helper.getLevel());
                    helper.assertTrue(storage.count(reinforced) == 4, "four reinforced alloys; got "
                            + storage.count(reinforced));
                    helper.assertTrue(storage.count(infused) == 0 && storage.count(Items.COPPER_INGOT) == 0
                                    && storage.count(Items.REDSTONE) == 0
                                    && storage.count(MekanismRig.itemKey(DUST_DIAMOND)) == 0,
                            "every input must be spent to the unit; stock=" + storage.query());
                })
                .thenSucceed();
    }

    /** Copper + 10 mB of redstone -> Infused Alloy. */
    private static ProcessingPattern infusedAlloy() {
        return new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(Items.COPPER_INGOT), 1),
                        new ProcessingPattern.ProcessingInput(StorageKey.of(Items.REDSTONE), 1)),
                List.of(new ProcessingPattern.ProcessingOutput(MekanismRig.itemKey(ALLOY_INFUSED), 1, 100)),
                400);
    }

    /** Infused Alloy + 20 mB of diamond -> Reinforced Alloy. */
    private static ProcessingPattern reinforcedAlloy() {
        return new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(MekanismRig.itemKey(ALLOY_INFUSED), 1),
                        new ProcessingPattern.ProcessingInput(MekanismRig.itemKey(DUST_DIAMOND), 2)),
                List.of(new ProcessingPattern.ProcessingOutput(MekanismRig.itemKey(ALLOY_REINFORCED), 1, 100)),
                400);
    }

    private static void powerBoth(final GameTestHelper helper) {
        MekanismRig.power(helper);
        MekanismRig.power(helper.getLevel(), helper.absolutePos(MekanismRig.MACHINE_B));
    }

    private static long infuserStepsRunning(final MainframeBlockEntity mainframe) {
        final StorageKey infused = MekanismRig.itemKey(ALLOY_INFUSED);
        final StorageKey reinforced = MekanismRig.itemKey(ALLOY_REINFORCED);
        return mainframe.activeOperationRecords().stream()
                .filter(r -> r.subs().isEmpty() && (infused.equals(r.key()) || reinforced.equals(r.key()))
                        && r.status() == OperationRecord.STATUS_PROCESSING)
                .count();
    }
}
