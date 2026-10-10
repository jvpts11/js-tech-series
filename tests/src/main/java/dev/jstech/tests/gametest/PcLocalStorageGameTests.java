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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningPC;

/**
 * GameTests for the local storage of a Personal Computer: selects landing in it, the disk capacity cap, inserts
 * moving into the network, and the storage travelling with the disk.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PcLocalStorageGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private PcLocalStorageGameTests() {
    }

    @GameTest(template = ARENA)
    public static void terminalSelect_landsInPcLocalStorage(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        // A disk gives the PC local storage, so a SELECT lands there.
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
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
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    helper.assertTrue(computer.networkUuid() != null, "PC must be on the network");
                })
                /*
                 * Let the Mainframe's in-RAM catalog pick up the freshly seeded items before the SELECT
                 * locks against it (the terminal only ever lets a player pick an already-indexed item).
                 */
                .thenExecuteAfter(2, () -> {
                    /*
                     * The terminal SELECT-to-storage resolves the computer's local storage as the
                     * destination; route the timed pull straight into it.
                     */
                    final var op = mainframe.submitNetworkSelect(Items.COBBLESTONE, 50,
                            computer.localStorage(), "storage", null);
                    helper.assertTrue(op != null, "Mainframe should dispatch the SELECT-to-storage Operation");
                })
                /*
                 * The pull is timed by the disks it moves between, and a busy server runs it a few ticks later than
                 * an idle one; wait for it to land rather than for a fixed count of ticks.
                 */
                .thenWaitUntil(() -> {
                    final long inStorage = computer.localStore().count(StorageKey.of(Items.COBBLESTONE));
                    helper.assertTrue(inStorage == 50,
                            "SELECT must land 50 cobblestone in the PC's local storage; got " + inStorage);
                })
                .thenExecute(() -> {
                    // The Operation is logged with provenance for the Operations tab.
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(log.size() == 1, "the SELECT should be logged once; got " + log.size());
                    helper.assertTrue(log.get(0).moved() == 50,
                            "logged op should record 50 moved; got " + log.get(0).moved());
                    helper.assertTrue(!log.get(0).moves().isEmpty(),
                            "logged op should carry provenance moves (from which server)");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void localStorage_cappedByDiskCapacity(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        // A 500 GB disk holds 2,000 items, and the SELECT below asks for more than that.
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.GB_500)));
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
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 2_500);
                    helper.assertTrue(computer.networkUuid() != null, "PC must be on the network");
                })
                .thenExecuteAfter(2, () -> {
                    // Ask for more than the 2,000-item disk can hold.
                    final var op = mainframe.submitNetworkSelect(Items.COBBLESTONE, 2_500,
                            computer.localStorage(), "storage", null);
                    helper.assertTrue(op != null, "Mainframe should dispatch the SELECT");
                })
                /*
                 * Waited for rather than counted in ticks: how long a SELECT of 2,500 items takes is the
                 * network's business and the server's load, and a fixed number of ticks here is a test
                 * that fails when the machine is busy rather than when the code is wrong.
                 */
                .thenWaitUntil(() -> helper.assertTrue(
                        computer.localStore().count(StorageKey.of(Items.COBBLESTONE)) == 2_000,
                        "local storage must cap at the 2,000-item disk capacity; got "
                                + computer.localStore().count(StorageKey.of(Items.COBBLESTONE))))
                .thenExecute(() -> {
                    final long inStorage = computer.localStore().count(StorageKey.of(Items.COBBLESTONE));
                    final long inNet = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(inStorage + inNet == 2_500,
                            "the rest must stay in the network, nothing lost; storage=" + inStorage + " net=" + inNet);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void terminalInsert_movesIntoNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
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
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe must own a network");
                    final var op = mainframe.submitNetworkInsert(Items.COBBLESTONE, 64, "terminal");
                    helper.assertTrue(op != null, "Mainframe should dispatch the terminal INSERT Operation");
                })
                .thenExecuteAfter(8, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    final long held = NetworkStorage.of(helper.getLevel(), net).count(Items.COBBLESTONE);
                    helper.assertTrue(held == 64,
                            "INSERT must deposit 64 cobblestone into the network; got " + held);
                    final var log = mainframe.recentOperations();
                    helper.assertTrue(log.size() == 1, "the INSERT should be logged once; got " + log.size());
                    helper.assertTrue(log.get(0).type() == OperationRecord.TYPE_INSERT,
                            "logged op should be an INSERT");
                    helper.assertTrue(log.get(0).moved() == 64,
                            "logged op should record 64 moved; got " + log.get(0).moved());
                    helper.assertTrue(!log.get(0).moves().isEmpty(),
                            "logged op should carry provenance moves (to which server)");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void localStorage_travelsWithDisk(final GameTestHelper helper) {
        final BlockPos pc = new BlockPos(2, 2, 2);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.localStore().insert(StorageKey.of(Items.COBBLESTONE), 100) == 100L,
                            "should store 100 in local storage");
                    helper.assertTrue(computer.localStore().used() == 100L, "local storage holds 100");
                    // Pull the disk out of the computer.
                    final ItemStack disk = computer.getHardware().extractItem(
                            PersonalComputerBlockEntity.DISK_SLOTS_START, 1, false);
                    helper.assertFalse(disk.isEmpty(), "the disk should come out");
                    // The items travel WITH the disk; local storage is empty without it.
                    final var contents = dev.jstech.computers.storage.DriveVolumes.contents(disk);
                    helper.assertTrue(contents.count(Items.COBBLESTONE) == 100L,
                            "the pulled disk must carry its 100 cobblestone");
                    helper.assertTrue(computer.localStore().used() == 0L,
                            "local storage is empty once the disk is removed; got " + computer.localStore().used());
                })
                .thenSucceed();
    }
}
