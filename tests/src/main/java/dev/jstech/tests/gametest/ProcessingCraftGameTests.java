/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.CraftingFixtures.both;
import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.cobblePattern;
import static dev.jstech.tests.testkit.CraftingFixtures.kiln;
import static dev.jstech.tests.testkit.CraftingFixtures.kilnRig;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * GameTests for processing recipes run on a real machine through a Crafting Interface, with what comes out
 * credited by a Crafting Receiving Bus and every item and fluid conserved whatever happens to the job.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProcessingCraftGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final String NO_INTERFACE = "jsc.operation.failure.no_interface";

    private ProcessingCraftGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_deliversInputsToTheMachine(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    // Held, the kiln keeps what it is given, so the delivery itself can be seen.
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "test") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(15, () -> helper.assertTrue(kiln(rig).input(0).is(Items.COBBLESTONE)
                                && kiln(rig).input(0).getCount() == 4,
                        "the interface delivered the four lots from the network into the machine; got "
                                + kiln(rig).input(0)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_collectsOutputsIntoTheNetwork(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "test") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(60, () -> {
                    final long stone = net.storage(helper).count(storageKey(Items.STONE));
                    helper.assertTrue(stone == 4, "the Receiving Bus credited the four stone made; stone=" + stone);
                    helper.assertTrue(kiln(rig).output().isEmpty(), "nothing the job made is left in the kiln");
                })
                .thenSucceed();
    }

    /** A recipe no interface holds has no machine to run on: the job times out and nothing leaves the network. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_recipeNoInterfaceHoldsTimesOutAndConservesInputs(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> net.seed(Items.COBBLESTONE, 16))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op.set(net.mainframe().submitNetworkProcessing(cobblePattern(5), 4, "test"));
                    helper.assertTrue(op.get() != null, "the operation is accepted");
                })
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(op.get().isDone(), "the job timed out instead of hanging");
                    helper.assertTrue(op.get().toRecord().status() == OperationRecord.STATUS_FAILED,
                            "it settled FAILED");
                    helper.assertTrue(op.get().toRecord().cause().key().equals(NO_INTERFACE),
                            "its cause says no interface holds the recipe; got " + op.get().toRecord().cause());
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.COBBLESTONE)) == 16,
                            "no machine ran, so the inputs are all still in the network");
                })
                .thenSucceed();
    }

    /** A paused interface takes no job: the job waits, without a timeout, and its machine is fed nothing. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_pausedInterfaceIsNotFed(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(10));
                    rig.part().setPaused(true, "");
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe().submitNetworkProcessing(cobblePattern(10),
                        4, "test")))
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(kiln(rig).input(0).isEmpty(), "a paused interface feeds nothing");
                    helper.assertFalse(op.get().isDone(), "the job outlives its timeout while it waits");
                    helper.assertTrue(op.get().isWaiting(), "the job waits for the interface");
                    rig.part().setPaused(false, "");
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_COMPLETED, "once resumed, the job runs and completes");
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE)) == 4, "four stone made");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_reachesCompletedStatus(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op.set(net.mainframe().submitNetworkProcessing(cobblePattern(200), 1, "test"));
                    final byte live = op.get().liveRecord().status();
                    helper.assertTrue(live == OperationRecord.STATUS_WAITING
                                    || live == OperationRecord.STATUS_PROCESSING,
                            "the live record is in flight before completion; got " + live);
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(op.get().isDone(), "the job finished");
                    helper.assertTrue(op.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                            "its status is COMPLETED after the output it asked for came back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void network_aggregatesMachineRecipes(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    rig.hold(CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200));
                    rig.hold(CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(net.mainframe().networkMachineRecipes().size() == 2,
                        "the Mainframe lists the recipes its computer's interfaces hold; got "
                                + net.mainframe().networkMachineRecipes().size()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void processing_rejectsNonPositiveQuantity(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe().submitNetworkProcessing(cobblePattern(200), 0, "t") == null,
                            "quantity 0 is rejected");
                    helper.assertTrue(net.mainframe().submitNetworkProcessing(cobblePattern(200), -10, "t") == null,
                            "a negative quantity is rejected");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_maxJobsCapsConcurrentJobsOnAnInterface(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op1 = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> op2 = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 256);
                    rig.hold(cobblePattern(200));
                    rig.part().setMaxJobs(1, "");
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    op1.set(net.mainframe().submitNetworkProcessing(cobblePattern(200), 999, "a"));
                    op2.set(net.mainframe().submitNetworkProcessing(cobblePattern(200), 999, "b"));
                })
                .thenExecuteAfter(10, () -> {
                    int active = 0;
                    if (!op1.get().isWaiting() && !op1.get().isDone()) {
                        active++;
                    }
                    if (!op2.get().isWaiting() && !op2.get().isDone()) {
                        active++;
                    }
                    helper.assertTrue(active == 1, "most jobs 1 keeps exactly one job on the interface; active="
                            + active);
                    helper.assertTrue(op1.get().isWaiting() || op2.get().isWaiting(),
                            "the job over the cap is WAITING");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_malformedPatternSettlesFailed(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final StorageKey cobble = storageKey(Items.COBBLESTONE);
                    final StorageKey stone = storageKey(Items.STONE);
                    // No outputs => no result key => the job settles FAILED at construction, never hangs.
                    final ProcessingPattern noOut = new ProcessingPattern(
                            List.of(new ProcessingPattern.ProcessingInput(cobble, 1L)), List.of(), 200);
                    final NetworkProcessingOperation op = net.mainframe().submitNetworkProcessing(noOut, 1, "test");
                    helper.assertTrue(op != null && op.isDone(), "a pattern with no outputs settles immediately");
                    helper.assertTrue(op.toRecord().status() == OperationRecord.STATUS_FAILED,
                            "and its status is FAILED");
                    final ProcessingPattern noIn = new ProcessingPattern(List.of(),
                            List.of(new ProcessingPattern.ProcessingOutput(stone, 1L, 100)), 200);
                    final NetworkProcessingOperation op2 = net.mainframe().submitNetworkProcessing(noIn, 1, "test");
                    helper.assertTrue(op2 != null && op2.isDone()
                            && op2.toRecord().status() == OperationRecord.STATUS_FAILED,
                            "a pattern with no inputs is FAILED too");
                })
                .thenSucceed();
    }

    /** Cobblestone becomes stone one for one: whatever the job does, the two together never change. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsAcrossTheCraft(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 200);
                    // Stone the kiln already held belongs to the machine and stays there.
                    kiln(rig).preload(new ItemStack(Items.STONE, 16));
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe().submitNetworkProcessing(cobblePattern(200), 32,
                        "conserve"))
                .thenExecuteAfter(200, () -> {
                    helper.assertTrue(both(helper, net, rig) == before[0],
                            "cobblestone and stone conserved: " + before[0] + " -> " + both(helper, net, rig));
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.STONE)) == 32,
                            "the 32 stone made reached the network; got "
                                    + net.storage(helper).count(storageKey(Items.STONE)));
                    helper.assertTrue(kiln(rig).output().getCount() == 16,
                            "the 16 stone the kiln held before the job are still in it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesFluidsThroughInsertAndSelect(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey water = StorageKey.of(new FluidStack(Fluids.WATER, 1));
                    final long inserted = storage.insert(water, 8000L);
                    helper.assertTrue(inserted > 0, "the network accepts fluid into its mB-eq capacity");
                    helper.assertTrue(storage.count(water) == inserted, "the inserted fluid is counted exactly");
                    final FluidTank tank = new FluidTank(1_000_000);
                    final long moved = storage.select(water, inserted, new ExternalDataPort(null, tank));
                    helper.assertTrue(moved == inserted, "all the fluid moves out of the network");
                    helper.assertTrue(storage.count(water) == 0, "the network fluid is fully drained");
                    helper.assertTrue(tank.getFluidAmount() == inserted,
                            "the sink holds exactly what left, fluid conserved (" + inserted + " mB)");
                })
                .thenSucceed();
    }

    /** A machine that never answers times the job out after it fed it: what it fed is still in the machine. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsOnTimeout(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(6));
                    before[0] = rig.total(net.storage(helper), Items.COBBLESTONE);
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe().submitNetworkProcessing(cobblePattern(6),
                        64, "timeout")))
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_FAILED, "the job timed out with nothing made");
                    final long after = rig.total(net.storage(helper), Items.COBBLESTONE);
                    helper.assertTrue(after == before[0], "no cobblestone lost on timeout: " + before[0] + " -> "
                            + after);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void processing_manyConcurrentJobsConserveAndDontCrash(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 1000);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // Fifty jobs at the one interface at once: the queue must hold, and nothing is made or lost.
                    for (int i = 0; i < 50; i++) {
                        net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "op" + i);
                    }
                })
                .thenExecuteAfter(120, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "cobblestone and stone conserved under 50 jobs: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    /** A job given up on mid-run still collects what its machine goes on making, and nothing is lost. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsOnAbandon(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe().submitNetworkProcessing(
                        cobblePattern(200), 16, "abandon")))
                .thenExecuteAfter(8, () -> op.get().abandon())
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(op.get().isDone(), "an abandoned job settles");
                    helper.assertTrue(both(helper, net, rig) == before[0],
                            "nothing lost on abandon mid-run: " + before[0] + " -> " + both(helper, net, rig));
                    helper.assertTrue(kiln(rig).output().isEmpty(),
                            "what the kiln made after the job settled still went to the network");
                })
                .thenSucceed();
    }

    /**
     * A machine's output waits in the machine while the network has no room for it. It used to be pulled out
     * whole and stored as far as it fitted, and whatever a full network refused was simply gone.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_leavesTheOutputInTheMachineWhenTheNetworkIsFull(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 32);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.mainframe().submitNetworkProcessing(cobblePattern(200), 8, "full");
                    before[0] = both(helper, net, rig);
                })
                // The job has fed its eight lots; now every byte the drives have left goes to dirt.
                .thenExecuteAfter(6, () -> net.rack().getServerStorage(0).insert(Items.DIRT, Long.MAX_VALUE / 4))
                .thenExecuteAfter(60, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "nothing the machine made is lost to a full network: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_conservesItemsThroughPowerCycle(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 128);
                    rig.hold(cobblePattern(200));
                    before[0] = both(helper, net, rig);
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe().submitNetworkProcessing(cobblePattern(200), 8,
                        "power"))
                .thenExecuteAfter(6, net.mainframe()::togglePower)
                .thenExecuteAfter(12, net.mainframe()::togglePower)
                .thenExecuteAfter(60, () -> helper.assertTrue(both(helper, net, rig) == before[0],
                        "nothing lost through a power cycle mid-run: " + before[0] + " -> "
                                + both(helper, net, rig)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_repeatedSubmitsAreIndependent(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final var op1 = net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "a");
                    final var op2 = net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "b");
                    final var op3 = net.mainframe().submitNetworkProcessing(cobblePattern(200), 4, "c");
                    helper.assertTrue(op1 != null && op2 != null && op3 != null, "every submit is accepted");
                    helper.assertTrue(op1 != op2 && op2 != op3 && op1 != op3,
                            "repeated identical submits create independent operations, no aliasing");
                    helper.assertTrue(!op1.operationId().equals(op2.operationId())
                                    && !op2.operationId().equals(op3.operationId()),
                            "each operation gets a distinct id");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void mainframe_operationsStateSurvivesReload(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> net.mainframe().submitNetworkProcessing(cobblePattern(200), 1,
                        "reload"))
                .thenExecuteAfter(30, () -> {
                    final long completedBefore = net.mainframe().completedOps();
                    final int logBefore = net.mainframe().recentOperations().size();
                    helper.assertTrue(completedBefore >= 1, "the job completed before the reload");
                    final var reg = helper.getLevel().registryAccess();
                    net.mainframe().loadWithComponents(net.mainframe().saveWithFullMetadata(reg), reg);
                    helper.assertTrue(net.mainframe().completedOps() == completedBefore,
                            "the completed-ops total survives a reload: " + completedBefore + " -> "
                                    + net.mainframe().completedOps());
                    helper.assertTrue(net.mainframe().recentOperations().size() == logBefore,
                            "the operations log survives a reload (" + logBefore + " entries)");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesLargeItemLoadAtScale(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.IRON_INGOT, 30_000);
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey iron = storageKey(Items.IRON_INGOT);
                    final long total = storage.count(iron);
                    helper.assertTrue(total > 0, "the network holds a large item load (" + total + ")");
                    final ItemStackHandler sink = new ItemStackHandler(1024);
                    final long moved = storage.select(iron, total, new ExternalDataPort(sink, null));
                    helper.assertTrue(moved == total, "the whole load moves out");
                    helper.assertTrue(storage.count(iron) == 0, "the network is fully drained, nothing stuck");
                    long inSink = 0;
                    for (int s = 0; s < sink.getSlots(); s++) {
                        if (sink.getStackInSlot(s).is(Items.IRON_INGOT)) {
                            inSink += sink.getStackInSlot(s).getCount();
                        }
                    }
                    helper.assertTrue(inSink == total,
                            "the sink holds exactly the load, items conserved at scale (" + total + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void networkStorage_conservesLargeFluidLoadAtScale(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    final StorageKey water = StorageKey.of(new FluidStack(Fluids.WATER, 1));
                    final long inserted = storage.insert(water, 500_000L);
                    helper.assertTrue(inserted > 0, "the network accepts a large fluid load (" + inserted + " mB)");
                    helper.assertTrue(storage.count(water) == inserted, "the whole load is counted exactly");
                    final FluidTank tank = new FluidTank(4_000_000);
                    final long moved = storage.select(water, inserted, new ExternalDataPort(null, tank));
                    helper.assertTrue(moved == inserted, "the whole large load moves out in one go");
                    helper.assertTrue(storage.count(water) == 0, "the network is fully drained, nothing stuck");
                    helper.assertTrue(tank.getFluidAmount() == inserted,
                            "the sink holds exactly the load, conserved at scale (" + inserted + " mB)");
                })
                .thenSucceed();
    }

    /**
     * A machine taken away mid-run leaves its job waiting, not failed: the interface still holds the recipe. Put back,
     * the machine is the job's again, and the job settles on what it could make.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void processing_waitsWhileItsMachineIsGoneAndSettlesOnceItIsBack(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobblePattern(20));
                })
                .thenExecuteAfter(SETTLE + 2, () -> op.set(net.mainframe().submitNetworkProcessing(cobblePattern(20),
                        8, "test")))
                .thenExecuteAfter(4, () -> {
                    helper.setBlock(rig.machinePos(), Blocks.AIR);
                    // The computer reads its crafting network again at once, as it would within a second.
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertFalse(op.get().isDone(), "the job outlives its timeout while the machine is gone");
                    helper.assertTrue(op.get().isWaiting(), "the job waits for its machine");
                    helper.setBlock(rig.machinePos(), TestMachines.KILN.get());
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(80, () -> helper.assertTrue(op.get().isDone(),
                        "once the machine is back the job settles instead of hanging"))
                .thenSucceed();
    }

    /** A machine fed through a router on the interface's own cable, its output collected on the far side. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_routedMachineIsFedThroughItsRouter(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = CraftingRig.routed(TestWorldBuilder.forGameTest(helper), net.cc(),
                TestMachines.KILN.get(), false);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.RAW_COPPER, 32);
                    rig.hold(CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe().submitNetworkProcessing(
                        CraftingRig.pattern(Items.RAW_COPPER, Items.COPPER_INGOT, 200), 8, "routed") != null,
                        "the routed job is accepted"))
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(kiln(rig).made() == 8, "the router fed the kiln eight lots; made "
                            + kiln(rig).made());
                    final long stored = net.storage(helper).count(storageKey(Items.COPPER_INGOT));
                    helper.assertTrue(stored == 8, "the ingots reached the network through the bus; got " + stored);
                })
                .thenSucceed();
    }

    /** A machine with sided inputs: each input goes in through the router its filter names, on the face it needs. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_sidedMachineTakesEachInputThroughItsRouter(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = CraftingRig.routed(TestWorldBuilder.forGameTest(helper), net.cc(),
                TestMachines.MIXER.get(), true);
        final ProcessingPattern mix = CraftingRig.mix(Items.DIRT, Items.GRAVEL, Items.COARSE_DIRT, 2);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.DIRT, 16);
                    net.seed(Items.GRAVEL, 16);
                    rig.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.GRAVEL));
                    rig.hold(mix);
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkProcessing(mix, 8, "sided") != null, "the sided job is accepted"))
                .thenExecuteAfter(80, () -> {
                    final long coarse = net.storage(helper).count(storageKey(Items.COARSE_DIRT));
                    helper.assertTrue(coarse == 8, "four mixes made eight coarse dirt; got " + coarse);
                    helper.assertTrue(net.storage(helper).count(storageKey(Items.DIRT)) == 12
                                    && net.storage(helper).count(storageKey(Items.GRAVEL)) == 12,
                            "exactly four of each input left the network");
                })
                .thenSucceed();
    }

    /** One feed moves as many lots as the machine takes and the job needs, not one lot every few ticks. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void processing_feedsManyLotsPerCycle(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final CraftingRig rig = kilnRig(helper, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    kiln(rig).setHeld(true);
                    rig.hold(cobblePattern(200));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkProcessing(cobblePattern(200), 64, "fill") != null,
                        "the processing operation is accepted"))
                .thenExecuteAfter(8, () -> {
                    final long delivered = kiln(rig).input(0).getCount();
                    // One lot every four ticks could never pass three items in eight ticks.
                    helper.assertTrue(delivered >= 16, "one feed delivers many lots; delivered=" + delivered);
                })
                .thenSucceed();
    }
}
