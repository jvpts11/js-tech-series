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
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for network state surviving a save and reload: the operation log and the installed hardware.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkReloadGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkReloadGameTests() {
    }

    @GameTest(template = ARENA)
    public static void operationLog_persistsAcrossReload(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity be = placeRunningMainframe(helper, a);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    be.recordOperation(OperationRecord.TYPE_INSERT, new ItemStack(Items.COBBLESTONE), 64, 64,
                            OperationRecord.STATUS_COMPLETED,
                            java.util.List.of(new OperationRecord.MoveRow("you", 64, "SRV-abc123")));
                    helper.assertTrue(be.recentOperations().size() == 1, "one op should be recorded");

                    final var registries = helper.getLevel().registryAccess();
                    final net.minecraft.nbt.CompoundTag saved = be.saveWithFullMetadata(registries);
                    final var reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                            helper.absolutePos(a), be.getBlockState(), saved, registries);
                    helper.assertTrue(reloaded instanceof MainframeBlockEntity, "reloaded BE should be a Mainframe");

                    final var log = ((MainframeBlockEntity) reloaded).recentOperations();
                    helper.assertTrue(log.size() == 1, "the op must survive reload; got " + log.size());
                    final OperationRecord rec = log.get(0);
                    helper.assertTrue(rec.type() == OperationRecord.TYPE_INSERT, "type must persist");
                    helper.assertTrue(rec.moved() == 64, "moved must persist; got " + rec.moved());
                    helper.assertTrue(rec.icon().is(Items.COBBLESTONE), "icon item must persist");
                    helper.assertTrue(!rec.moves().isEmpty() && rec.moves().get(0).to().equals("SRV-abc123"),
                            "provenance must persist");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void installedHardware_persistsAcrossReload(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity be = placeRunningMainframe(helper, a);
        /*
         * Add an extra CPU and a disk on top of the valid build, so a wrong/changed hardware NBT key
         * (the Mainframe persists under "Inventory", not the base's "Hardware") would be caught here:
         * the reloaded build would silently lose these components.
         */
        be.getInventory().setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START + 1,
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        be.getInventory().setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final long capacityBefore = be.capacity();
                    final long storageBefore = be.storageItems();
                    /*
                     * The OS (installed by the setup helper) reserves part of the disk; the usable storage is
                     * the raw disk capacity minus the OS footprint.
                     */
                    final long expectedStorage = DiskSize.TB_1.capacityItems() - be.reservedByOs();
                    helper.assertTrue(storageBefore == expectedStorage,
                            "build should report the disk's capacity net of the OS footprint before reload; got "
                                    + storageBefore + " expected " + expectedStorage);

                    final var registries = helper.getLevel().registryAccess();
                    final net.minecraft.nbt.CompoundTag saved = be.saveWithFullMetadata(registries);
                    final var reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                            helper.absolutePos(a), be.getBlockState(), saved, registries);
                    helper.assertTrue(reloaded instanceof MainframeBlockEntity, "reloaded BE should be a Mainframe");
                    final MainframeBlockEntity loaded = (MainframeBlockEntity) reloaded;

                    helper.assertTrue(loaded.getInventory()
                                    .getStackInSlot(MainframeBlockEntity.DISK_SLOTS_START).getItem()
                                    instanceof dev.jstech.computers.item.DiskItem,
                            "the installed disk must survive reload under the preserved NBT key");
                    helper.assertTrue(loaded.getInventory()
                                    .getStackInSlot(MainframeBlockEntity.CPU_SLOTS_START + 1).getItem()
                                    instanceof dev.jstech.computers.item.CpuItem,
                            "the second CPU must survive reload under the preserved NBT key");
                    helper.assertTrue(loaded.storageItems() == storageBefore,
                            "reloaded build must report the same storage capacity; got " + loaded.storageItems()
                                    + " expected " + storageBefore);
                    helper.assertTrue(loaded.capacity() == capacityBefore,
                            "reloaded build must report the same orchestration capacity; got " + loaded.capacity()
                                    + " expected " + capacityBefore);
                })
                .thenSucceed();
    }
}
