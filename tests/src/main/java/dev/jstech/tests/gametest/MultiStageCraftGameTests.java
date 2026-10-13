/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkMultiStageOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import dev.jstech.tests.testkit.CraftingRig;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.cobblePattern;
import static dev.jstech.tests.testkit.CraftingFixtures.kilnRig;
import static dev.jstech.tests.testkit.CraftingFixtures.stoneButtonPattern;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * GameTests for multi-stage crafting pipelines: a processing stage, a request the recursive planner cannot
 * see, a mixed processing and bench pipeline, and a failed stage failing the pipeline while conserving items.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiStageCraftGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private MultiStageCraftGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_runsItsProcessingStage(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final MultiStagePattern multi = new MultiStagePattern(List.of(MultiStagePattern.Stage.proc(
                cobblePattern(200))));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(NetworkRecipe.ofMultiStage(multi));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkMultiStage(multi, 2, "test") != null,
                        "the multi-stage operation is accepted"))
                .thenExecuteAfter(60, () -> {
                    final long stone = net.storage(helper).count(storageKey(Items.STONE));
                    helper.assertTrue(stone >= 2, "the multi-stage ran its processing stage; stone=" + stone);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craftRequest_runsAMultiStageRecipeTheRecursivePlannerCannotSee(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final StorageKey stone = storageKey(Items.STONE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    /*
                     * Only a multi-stage recipe for stone. The recursive craft planner unwraps processing patterns
                     * but never multi-stage ones, so it is blind to this recipe, which is why the CLI and IQL,
                     * before they shared the terminal's entry point, could not craft it.
                     */
                    rig.hold(NetworkRecipe.ofMultiStage(new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(cobblePattern(200))))));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe().submitNetworkCraft(stone, 1, true, "test") == null,
                            "the recursive planner is blind to a multi-stage-only recipe");
                    helper.assertTrue(net.mainframe().submitCraftRequest(stone, 1, true, "test (Shell)", null) != null,
                            "the shared craft entry point runs the multi-stage recipe");
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(net.storage(helper).count(stone) > 0,
                        "the multi-stage recipe ran through the shared entry point; stone="
                                + net.storage(helper).count(stone)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_mixedPipelineRunsProcThenBench(final GameTestHelper helper) {
        /*
         * A two-stage pipeline mixing both stage kinds: a processing stage (cobblestone to stone in the kiln)
         * followed by a bench stage (stone to a stone button). The processing output flows through network storage
         * into the bench stage, and the whole pipeline settles COMPLETED.
         */
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkMultiStageOperation> opHolder = new AtomicReference<>();
        final MultiStagePattern multi = new MultiStagePattern(List.of(
                MultiStagePattern.Stage.proc(cobblePattern(200)),
                MultiStagePattern.Stage.bench(stoneButtonPattern())));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    // The bench pattern is not in the ROM: a pipeline's bench stage runs from its own pattern.
                    rig.hold(NetworkRecipe.ofMultiStage(multi));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    opHolder.set(net.mainframe().submitNetworkMultiStage(multi, 1, "test"));
                    helper.assertTrue(opHolder.get() != null, "the mixed multi-stage operation is accepted");
                })
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(opHolder.get().isDone(), "the mixed pipeline settles");
                    helper.assertTrue(opHolder.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "the mixed pipeline settles COMPLETED; status=" + opHolder.get().toRecord().status());
                    final long buttons = net.storage(helper).count(storageKey(Items.STONE_BUTTON));
                    helper.assertTrue(buttons >= 1,
                            "the bench stage crafts the button from the processing stage's output; got " + buttons);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void multiStage_failedStageFailsThePipelineAndConserves(final GameTestHelper helper) {
        /*
         * Stage 1 is held by no interface, so it times out FAILED. The pipeline fails with it, stage 2 never runs,
         * and the network's inputs are conserved.
         */
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkMultiStageOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> net.seed(Items.COBBLESTONE, 16))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final MultiStagePattern multi = new MultiStagePattern(List.of(
                            MultiStagePattern.Stage.proc(cobblePattern(5)),
                            MultiStagePattern.Stage.bench(stoneButtonPattern())));
                    opHolder.set(net.mainframe().submitNetworkMultiStage(multi, 1, "test"));
                    helper.assertTrue(opHolder.get() != null, "the operation is accepted");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(opHolder.get().isDone(), "the pipeline settles instead of hanging");
                    helper.assertTrue(opHolder.get().toRecord().status() == OperationRecord.STATUS_FAILED,
                            "a failed stage fails the whole pipeline; status=" + opHolder.get().toRecord().status());
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.COBBLESTONE)) == 16,
                            "no machine ran, so the pipeline's inputs are conserved");
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE_BUTTON)) == 0,
                            "the bench stage after the failed stage never runs");
                })
                .thenSucceed();
    }
}
