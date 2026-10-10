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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for Operations dispatched by the Mainframe: the virtual-thread runtime completing and closing, the
 * log on power loss, abandoned inserts, aborted selects, live progress, and timed select, insert and delete.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkOperationGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkOperationGameTests() {
    }

    @GameTest(template = ARENA)
    public static void poweringOffMidOperation_recordsDiscardedInTheLog(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(3, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final StorageKey cobble = StorageKey.of(Items.COBBLESTONE);
        final dev.jstech.computers.operation.NetworkSelectOperation[] op =
                new dev.jstech.computers.operation.NetworkSelectOperation[1];
        final ItemStackHandler dest = new ItemStackHandler(9);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(mainframe.networkUuid() != null, "mainframe owns a network");
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 100);
                })
                /*
                 * Let the incremental ANALYZE index the seeded items, then hold the type so the
                 * SELECT can never finish: it stays in-flight (WAITING) until we power off.
                 */
                .thenExecuteAfter(3, () -> {
                    final long held = mainframe.lockType(cobble, Long.MAX_VALUE, null);
                    helper.assertTrue(held == 100, "LOCK must hold all 100 cobblestone; got " + held);
                    op[0] = mainframe.submitNetworkSelect(Items.COBBLESTONE, 50,
                            new dev.jstech.computers.storage.ExternalDataPort(dest, null), "test");
                    helper.assertTrue(op[0] != null, "Mainframe should dispatch the SELECT");
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(op[0].isWaiting(), "the SELECT must be in-flight (WAITING) before power-off");
                    mainframe.togglePower(); // power off with an Operation still in flight
                })
                /*
                 * Powering off makes the next tick run closeDispatch(), which abandons every
                 * in-flight Operation and records it so the log keeps a trace instead of losing it.
                 */
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(mainframe.isRunning(), "the mainframe is powered off");
                    final boolean discarded = mainframe.recentOperations().stream()
                            .anyMatch(r -> r.status() == OperationRecord.STATUS_DISCARDED);
                    helper.assertTrue(discarded,
                            "an Operation abandoned by power-off must be logged as DISCARDED, not vanish");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkOperation_insertDispatchedByMainframe(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(3, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    final var op = mainframe.submitNetworkInsert(Items.COBBLESTONE, 40, "test");
                    helper.assertTrue(op != null, "Mainframe should dispatch the INSERT Operation");
                })
                // The timed Operation streams over a few ticks (disk latency, then the write).
                .thenExecuteAfter(8, () -> {
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 40,
                            "INSERT Operation should have stored 40 via the dispatcher; got "
                                    + ns.count(Items.COBBLESTONE));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void dispatch_completesSubmittedOperations(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity be = placeRunningMainframe(helper, a);
        final int n = 6;
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(be.isRunning(), "mainframe should be running");
                    final int submitted = be.submitSelfTest(n, 50_000);
                    helper.assertTrue(submitted == n, "should submit " + n + " ops, got " + submitted);
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(be.completedOps() == n,
                            "all " + n + " ops should complete; done=" + be.completedOps());
                    helper.assertTrue(be.pendingOps() == 0, "no ops should remain pending");
                    helper.assertTrue(be.runningOps() == 0, "no ops should still be running");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void dispatch_closesOnPowerOff(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity be = placeRunningMainframe(helper, a);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> be.submitSelfTest(4, 50_000))
                .thenExecute(be::togglePower) // power off
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(be.isRunning(), "mainframe should be stopped");
                    helper.assertTrue(be.pendingOps() == 0, "stopped dispatcher reports no pending");
                    helper.assertTrue(be.runningOps() == 0, "stopped dispatcher reports nothing running");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void insert_abandonsCleanlyOnPowerOff(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        rackBe.getServers().setStackInSlot(0, ServerStacks.defaultServer());
        rackBe.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_8)));
        rackBe.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_8)));

        /*
         * A large INSERT: far beyond one tick of the server's RAM-bounded write rate, so it is
         * certainly still in flight when power is cut a few ticks in.
         */
        final long demand = 30_000L;
        final java.util.concurrent.atomic.AtomicReference<
                dev.jstech.computers.operation.NetworkInsertOperation> opBox =
                new java.util.concurrent.atomic.AtomicReference<>();

        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final var op = mainframe.submitNetworkInsert(Items.COBBLESTONE, demand, "test");
                    helper.assertTrue(op != null, "the INSERT should dispatch on a running mainframe");
                    opBox.set(op);
                })
                .thenExecuteAfter(6, () -> {
                    final var op = opBox.get();
                    helper.assertFalse(op.isDone(), "the INSERT should still be in flight before power-off");
                    helper.assertTrue(op.writtenTotal() > 0L,
                            "it should have written some before power-off; written=" + op.writtenTotal());
                    mainframe.togglePower(); // power off mid-flight -> closeDispatch must abandon it
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final var op = opBox.get();
                    helper.assertFalse(mainframe.isRunning(), "mainframe should be powered off");
                    helper.assertTrue(op.isDone(), "a power-off must settle the in-flight INSERT (no wedge)");
                    final long written = op.writtenTotal();
                    helper.assertTrue(written > 0L && written < demand,
                            "it was abandoned mid-flight; written=" + written);
                    helper.assertTrue(op.leftover() == demand - written,
                            "leftover must account for every unwritten item; leftover=" + op.leftover()
                                    + " written=" + written);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void select_abortsWhenDestinationGone(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        // A large pull into a roomy sink, so it spans many ticks (it cannot finish before we cut it).
        final long seeded = 8_000L;
        final ItemStackHandler dest = new ItemStackHandler(1000);
        final java.util.concurrent.atomic.AtomicBoolean gone = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicReference<
                dev.jstech.computers.operation.NetworkSelectOperation> opBox =
                new java.util.concurrent.atomic.AtomicReference<>();

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> rackBe.getServerStorage(0).insert(Items.COBBLESTONE, seeded))
                // Let the in-RAM catalog see the seeded items before the SELECT locks against it.
                .thenExecuteAfter(2, () -> {
                    final var op = mainframe.submitNetworkSelect(
                            Items.COBBLESTONE, seeded, new dev.jstech.computers.storage.ExternalDataPort(dest, null), "test", null);
                    helper.assertTrue(op != null, "the SELECT should dispatch on a running mainframe");
                    op.abortWhen(gone::get);
                    opBox.set(op);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertFalse(opBox.get().isDone(), "the SELECT should still be pulling before its sink is gone");
                    gone.set(true); // the destination vanished (the requesting player logged out)
                })
                .thenExecuteAfter(2, () -> {
                    final var op = opBox.get();
                    helper.assertTrue(op.isDone(), "the SELECT must settle once its destination is gone");
                    long inDest = 0L;
                    for (int i = 0; i < dest.getSlots(); i++) {
                        if (dest.getStackInSlot(i).is(Items.COBBLESTONE)) {
                            inDest += dest.getStackInSlot(i).getCount();
                        }
                    }
                    final long inNet = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(inDest > 0L && inDest < seeded,
                            "it moved some but not all before aborting; inDest=" + inDest);
                    helper.assertTrue(inDest + inNet == seeded,
                            "no items lost or created on abort; dest=" + inDest + " net=" + inNet);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void activeOperations_reportLiveProgress(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(mainframe.hasActiveOperations(), "idle before any submit");
                    helper.assertTrue(mainframe.submitNetworkInsert(Items.COBBLESTONE, 200_000L, "test") != null,
                            "the large INSERT should dispatch");
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(mainframe.hasActiveOperations(), "an Operation should be in flight");
                    final var live = mainframe.activeOperationRecords();
                    helper.assertTrue(live.size() == 1, "exactly one in-flight Operation; got " + live.size());
                    final var rec = live.get(0);
                    helper.assertTrue(rec.status() == OperationRecord.STATUS_PROCESSING,
                            "an in-flight Operation reads PROCESSING");
                    helper.assertTrue(rec.requested() == 200_000L, "requested is the demand");
                    helper.assertTrue(rec.moved() > 0L && rec.moved() < 200_000L,
                            "moved reflects live progress; got " + rec.moved());
                    helper.assertTrue(rec.icon().is(Items.COBBLESTONE), "the icon is the moved item");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkSelect_movesItemsOverTimeAndUnlocks(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final BlockPos barrel = new BlockPos(4, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);

        final long[] stored = {0L};
        final dev.jstech.computers.operation.NetworkSelectOperation[] op = {null};

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 40))
                .thenExecuteAfter(4, () -> {
                    stored[0] = rackBe.getServerStorage(0).count(Items.COBBLESTONE);
                    helper.assertTrue(stored[0] > 0L, "the server should hold cobblestone");
                    final var dest = helper.getLevel().getCapability(
                            net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(barrel), null);
                    helper.assertTrue(dest != null, "the barrel must expose an item handler");
                    op[0] = mainframe.submitNetworkSelect(Items.COBBLESTONE, stored[0], new dev.jstech.computers.storage.ExternalDataPort(dest, null), "select");
                    helper.assertTrue(op[0] != null, "the SELECT must be accepted");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(op[0].isDone(), "the SELECT must finish");
                    helper.assertTrue(op[0].status() == OperationRecord.STATUS_COMPLETED,
                            "the SELECT must complete fully; status " + op[0].status());
                    long inBarrel = 0L;
                    if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            if (container.getItem(i).is(Items.COBBLESTONE)) {
                                inBarrel += container.getItem(i).getCount();
                            }
                        }
                    }
                    helper.assertTrue(inBarrel == stored[0],
                            "every selected item must reach the barrel; got " + inBarrel + " of " + stored[0]);
                    helper.assertTrue(rackBe.getServerStorage(0).count(Items.COBBLESTONE) == 0L,
                            "the items must have left the server");
                    helper.assertTrue(!mainframe.networkIndex().isLocked(op[0].operationId()),
                            "the lock must be released on completion");
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(!log.isEmpty() && log.get(0).type() == OperationRecord.TYPE_SELECT,
                            "the SELECT must be logged");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkInsert_writesItemsIntoServersOverTime(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        final dev.jstech.computers.operation.NetworkInsertOperation[] op = {null};

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () ->
                        op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, 40, "you"))
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(op[0] != null && op[0].isDone(), "the INSERT must finish");
                    helper.assertTrue(op[0].status() == OperationRecord.STATUS_COMPLETED,
                            "the INSERT must store everything; status " + op[0].status());
                    helper.assertTrue(op[0].writtenTotal() == 40L,
                            "40 items must be written; got " + op[0].writtenTotal());
                    helper.assertTrue(rackBe.getServerStorage(0).count(Items.COBBLESTONE) == 40L,
                            "the server must hold the inserted items");
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(!log.isEmpty() && log.get(0).type() == OperationRecord.TYPE_INSERT,
                            "the INSERT must be logged");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkInsert_failsAndReportsLeftoverWhenNetworkFull(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final var store = rackBe.getServerStorage(0);
                    store.insert(Items.COBBLESTONE, store.free()); // fill the only server to capacity
                    helper.assertTrue(store.free() == 0L, "the server must be full for this test");

                    final var op = mainframe.submitNetworkInsert(Items.DIRT, 16, "you");
                    helper.assertTrue(op != null && op.isDone(),
                            "an INSERT into a full network finishes immediately");
                    helper.assertTrue(op.status() == OperationRecord.STATUS_FAILED,
                            "nothing fit, so it FAILED; status " + op.status());
                    helper.assertTrue(op.writtenTotal() == 0L, "nothing was written");
                    helper.assertTrue(op.leftover() == 16L,
                            "the whole request is leftover; got " + op.leftover());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkDelete_pullsItemsOutAndLogsDelete(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final BlockPos barrel = new BlockPos(4, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);

        final long[] stored = {0L};
        final dev.jstech.computers.operation.NetworkSelectOperation[] op = {null};

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 32))
                .thenExecuteAfter(4, () -> {
                    stored[0] = rackBe.getServerStorage(0).count(Items.COBBLESTONE);
                    final var dest = helper.getLevel().getCapability(
                            net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(barrel), null);
                    op[0] = mainframe.submitNetworkDelete(Items.COBBLESTONE, stored[0], new dev.jstech.computers.storage.ExternalDataPort(dest, null), "export");
                    helper.assertTrue(op[0] != null, "the DELETE must be accepted");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(op[0].isDone() && op[0].status() == OperationRecord.STATUS_COMPLETED,
                            "the DELETE must complete");
                    helper.assertTrue(rackBe.getServerStorage(0).count(Items.COBBLESTONE) == 0L,
                            "the items must leave the network");
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(!log.isEmpty() && log.get(0).type() == OperationRecord.TYPE_DELETE,
                            "it must be logged as a DELETE, not a SELECT");
                })
                .thenSucceed();
    }
}
