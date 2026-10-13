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
import dev.jstech.computers.storage.ServerStore;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for the storage a data network pools from its servers: queries, selects and inserts, manual locks,
 * the disk capacity cap, megabyte counting, kept components, and a server without a CPU.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkStorageGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkStorageGameTests() {
    }

    @GameTest(template = ARENA)
    public static void networkStorage_queriesSelectsAndInserts(final GameTestHelper helper) {
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

                    // Seed the Server with 100 cobblestone.
                    final ServerStore store = rackBe.getServerStorage(0);
                    store.insert(Items.COBBLESTONE, 100);

                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), net);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 100,
                            "QUERY: network holds 100 cobblestone; got "
                                    + ns.count(Items.COBBLESTONE));

                    // SELECT 30 into a destination handler.
                    final ItemStackHandler dest = new ItemStackHandler(9);
                    final long moved = ns.select(Items.COBBLESTONE, 30, new dev.jstech.computers.storage.ExternalDataPort(dest, null));
                    helper.assertTrue(moved == 30, "SELECT should move 30; got " + moved);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 70,
                            "network should have 70 after SELECT");

                    // INSERT 10 back.
                    final int inserted = ns.insert(new ItemStack(Items.COBBLESTONE, 10));
                    helper.assertTrue(inserted == 10, "INSERT should store 10; got " + inserted);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 80,
                            "network should have 80 after INSERT");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void manualLock_makesConcurrentSelectWaitUntilUnlocked(final GameTestHelper helper) {
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
                // Let the Mainframe's incremental ANALYZE index the freshly seeded items.
                .thenExecuteAfter(3, () -> {
                    final long held = mainframe.lockType(cobble, Long.MAX_VALUE, null);
                    helper.assertTrue(held == 100, "LOCK must hold all 100 cobblestone; got " + held);
                    op[0] = mainframe.submitNetworkSelect(Items.COBBLESTONE, 50,
                            new dev.jstech.computers.storage.ExternalDataPort(dest, null), "test");
                    helper.assertTrue(op[0] != null, "Mainframe should dispatch the SELECT");
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(op[0].isWaiting(), "the SELECT must WAIT while the type is locked");
                    helper.assertFalse(op[0].isDone(), "a waiting SELECT is not done");
                    final long released = mainframe.unlockType(cobble);
                    helper.assertTrue(released == 100, "UNLOCK must release the 100 held; got " + released);
                })
                .thenExecuteAfter(12, () -> {
                    helper.assertTrue(op[0].isDone(), "the SELECT must finish once the lock is released");
                    helper.assertTrue(op[0].status() == OperationRecord.STATUS_COMPLETED,
                            "the SELECT must complete fully after unlock; status " + op[0].status());
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 50,
                            "the network must hold 50 after the SELECT; got " + ns.count(Items.COBBLESTONE));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkStorage_capsAtDiskCapacity(final GameTestHelper helper) {
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
        rackBe.getServers().setStackInSlot(0, ServerStacks.defaultServer());
        rackBe.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), net);
                    final int stored = ns.insert(new ItemStack(Items.COBBLESTONE, 2_500));
                    helper.assertTrue(stored == 2_000,
                            "INSERT must cap at the 2,000-item disk capacity; stored " + stored);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 2_000,
                            "network should hold exactly 2,000; got " + ns.count(Items.COBBLESTONE));
                })
                .thenSucceed();
    }

    /*
     * What the network says it holds, in the unit a drive's label is written in. The two numbers are not
     * one scaled by a constant: what an item costs is the era of the drive under it, so the drives are
     * asked rather than a count multiplied, and a 500 GB standard drive holding 2 000 items is full.
     */
    @GameTest(template = ARENA)
    public static void networkStorage_countsMegabytesTheWayTheDrivesDo(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(3, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        rackBe.getServers().setStackInSlot(0, ServerStacks.defaultServer());
        final ItemStack drive = new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500));
        rackBe.insertDrive(0, drive);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
                    final long nameplate = ((dev.jstech.computers.item.DiskItem) drive.getItem()).spec().capacityMb();
                    helper.assertTrue(ns.capacityMb() == nameplate,
                            "the network is as big as the drive's label says; got " + ns.capacityMb()
                                    + " against " + nameplate);
                    helper.assertTrue(ns.usedMb() == 0L, "and empty; got " + ns.usedMb());

                    ns.insert(new ItemStack(Items.COBBLESTONE, 960));
                    final long perItem = ((dev.jstech.computers.item.DiskItem) drive.getItem())
                            .spec().era().mbPerItem();
                    helper.assertTrue(ns.usedMb() == 960L * perItem,
                            "960 items take 960 times what one costs on that drive; got " + ns.usedMb());
                    helper.assertTrue(ns.used() == 960L && ns.capacity() == 2_000L,
                            "while the item count stays a count; got " + ns.used() + " of " + ns.capacity());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void networkStorage_preservesComponents(final GameTestHelper helper) {
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

        final ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Excalibur"));

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), net);
                    helper.assertTrue(ns.insert(named.copyWithCount(1)) == 1, "the named sword should store");
                    ns.insert(new ItemStack(Items.DIAMOND_SWORD, 1)); // a plain one too
                })
                .thenExecuteAfter(2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), net);
                    // The named and the plain sword are distinct keys, counted separately.
                    helper.assertTrue(ns.count(StorageKey.of(named)) == 1L,
                            "the named variant is its own key; got " + ns.count(StorageKey.of(named)));
                    helper.assertTrue(ns.count(Items.DIAMOND_SWORD) == 2L,
                            "two swords total across variants; got " + ns.count(Items.DIAMOND_SWORD));
                    // Pull the named one back out and confirm it kept its custom name.
                    final ItemStackHandler dest = new ItemStackHandler(4);
                    helper.assertTrue(ns.select(StorageKey.of(named), 1, new dev.jstech.computers.storage.ExternalDataPort(dest, null)) == 1L, "named SELECT moves 1");
                    final ItemStack out = dest.getStackInSlot(0);
                    helper.assertTrue(ItemStack.isSameItemSameComponents(out, named),
                            "the pulled sword must keep its components; got '" + out.getHoverName().getString() + "'");
                    helper.assertTrue(ns.count(Items.DIAMOND_SWORD) == 1L, "only the plain sword remains");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void server_withoutCpu_servesNoNetworkStorage(final GameTestHelper helper) {
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
        rackBe.getServers().setStackInSlot(0, ServerStacks.cpulessServer());

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 20))
                .thenExecuteAfter(6, () -> {
                    helper.assertTrue(mainframe.networkIndex().available(Items.COBBLESTONE) == 0L,
                            "a CPU-less Server must not appear in the network index");
                    final var net = mainframe.networkUuid();
                    helper.assertTrue(NetworkStorage.of(helper.getLevel(), net).count(Items.COBBLESTONE) == 0L,
                            "and it serves no storage to the network");
                })
                .thenSucceed();
    }
}
