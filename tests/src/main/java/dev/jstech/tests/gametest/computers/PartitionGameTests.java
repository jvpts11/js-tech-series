/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Partitions as volumes (computers/docs/design/storage-devices.md): a disk nothing has partitioned reads as one
 * partition over it all; each partition of a partitioned disk holds its own files and items, read through a view and
 * written back; opening another partition brings what it holds onto the disk; a machine's store counts each partition
 * that holds data; and formatting the whole disk takes the table off with everything on it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PartitionGameTests {

    private static final String ARENA = "empty";
    /* A Legacy IDE disk of 40 GB: 2,560 items of 16 MB. */
    private static final long LEGACY_40G_MB = 40_960L;
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath("jsc", "mc_dos");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath("jsc", "unix");

    private PartitionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void table_aDiskNothingPartitionedIsOnePartitionOverItAll(final GameTestHelper helper) {
        final ItemStack disk = new ItemStack(HardwareItems.DISK_TRENCH_20M.get());
        helper.assertFalse(DiskPartitions.written(disk), "nothing is written on a new disk");
        final DiskLayout layout = DiskPartitions.layout(disk);
        helper.assertTrue(layout.scheme() == PartitionScheme.MBR && layout.partitions().size() == 1,
                "a Vintage disk reads as one partition on a Master Boot Record: " + layout);
        final PartitionEntry whole = layout.find(1);
        helper.assertTrue(whole.sizeMb() == 20L && whole.fs() == FileSystemType.FAT16 && whole.active(),
                "twenty megabytes of FAT16, active: " + whole);
        helper.assertTrue(DiskPartitions.capacityItems(disk) == 20L, "holding the disk's twenty items");
        helper.assertTrue(DiskPartitions.view(disk, 1) == disk, "and it is the disk itself");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partitions_eachHoldsItsOwnFilesAndItems(final GameTestHelper helper) {
        final ItemStack disk = twoPartitions();
        final ItemStack shared = DiskPartitions.view(disk, 2);
        helper.assertTrue(shared != disk, "the second partition is read through a view");
        helper.assertTrue(DiskPartitions.capacityItems(shared) == 512L,
                "8 GB of 16 MB items: " + DiskPartitions.capacityItems(shared));
        helper.assertTrue(DiskPartitions.capacityItems(disk) == 2_048L,
                "the open one holds its own 32 GB: " + DiskPartitions.capacityItems(disk));
        DiskFilesystem.write(disk, "readme.txt", FileType.TXT, "on C", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        DiskFilesystem.write(shared, "notes.txt", FileType.TXT, "on E", Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
        DriveVolumes.of(shared).add(StorageKey.of(Items.IRON_INGOT), 5L);
        DiskPartitions.commit(disk, shared);
        final ItemStack again = DiskPartitions.view(disk, 2);
        helper.assertTrue(DiskFilesystem.read(again, "notes.txt").orElse("").equals("on E"),
                "what was written on the second partition is kept with the disk");
        helper.assertFalse(DiskFilesystem.exists(again, "readme.txt"), "and not what is on the first");
        helper.assertFalse(DiskFilesystem.exists(disk, "notes.txt"), "nor the other way round");
        helper.assertTrue(DriveVolumes.peek(again).count(StorageKey.of(Items.IRON_INGOT)) == 5L,
                "its items are its own volume's");
        helper.assertTrue(DriveVolumes.peek(disk).count(StorageKey.of(Items.IRON_INGOT)) == 0L,
                "the first partition holds none of them");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void open_bringsWhatThePartitionHoldsOntoTheDisk(final GameTestHelper helper) {
        final ItemStack disk = twoPartitions();
        DiskFilesystem.write(disk, "readme.txt", FileType.TXT, "on C", Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
        final ItemStack shared = DiskPartitions.view(disk, 2);
        DiskFilesystem.write(shared, "notes.txt", FileType.TXT, "on E", Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
        DiskPartitions.commit(disk, shared);
        DiskPartitions.open(disk, 2);
        helper.assertTrue(DiskPartitions.table(disk).open() == 2, "the second partition is open");
        helper.assertTrue(DiskFilesystem.read(disk, "notes.txt").orElse("").equals("on E"),
                "the disk now carries what it holds");
        helper.assertTrue(DiskFilesystem.read(DiskPartitions.view(disk, 1), "readme.txt").orElse("")
                .equals("on C"), "and the first is kept with the table");
        DiskPartitions.open(disk, 1);
        helper.assertTrue(DiskFilesystem.read(disk, "readme.txt").orElse("").equals("on C"),
                "opening the first again brings it back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void localStore_countsEachPartitionThatHoldsData(final GameTestHelper helper) {
        final ItemStack disk = twoPartitions();
        final LocalStore store = new LocalStore(List.of(disk), () -> { });
        helper.assertTrue(store.diskCount() == 2, "two volumes: " + store.diskCount());
        helper.assertTrue(store.capacity() == 2_048L + 512L, "the room of both: " + store.capacity());
        final long put = store.insert(StorageKey.of(Items.COBBLESTONE), 2_100L);
        helper.assertTrue(put == 2_100L, "fills the first and goes on into the second: " + put);
        final LocalStore later = new LocalStore(List.of(disk), () -> { });
        helper.assertTrue(later.count(StorageKey.of(Items.COBBLESTONE)) == 2_100L,
                "and what went into the second is still there when the store is read again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unpartition_takesTheTableOffWithWhatItHeld(final GameTestHelper helper) {
        final ItemStack disk = twoPartitions();
        final ItemStack shared = DiskPartitions.view(disk, 2);
        DriveVolumes.of(shared).add(StorageKey.of(Items.IRON_INGOT), 5L);
        DiskPartitions.commit(disk, shared);
        DiskPartitions.unpartition(disk);
        helper.assertFalse(DiskPartitions.written(disk), "the table is gone");
        helper.assertTrue(DiskPartitions.layout(disk).partitions().size() == 1, "one partition over it all again");
        helper.assertTrue(DiskPartitions.capacityItems(disk) == 2_560L, "holding the whole disk's room");
        helper.succeed();
    }

    /**
     * MC-DOS and UNIX System V on one Vintage disk, which has no boot manager: the active partition is the one that
     * starts, and making the other active starts the other at the next power-on.
     */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void activePartition_decidesWhatAVintageDiskStarts(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = EraNetwork.bareComputer(world, EraKit.VINTAGE, AT, 1);
        pc.togglePower();
        final int slot = 0;
        DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, DiskPartitions.layout(pc.diskInSlot(slot)).diskMb());
        layout = layout.create(0L, 8L, FileSystemType.FAT16, PartitionRole.DATA).layout();
        layout = layout.create(8L, 12L, FileSystemType.UFS, PartitionRole.DATA).layout();
        DiskPartitions.relayout(pc.diskInSlot(slot), layout.activate(1).layout());
        helper.assertTrue(pc.installOs(MC_DOS, slot), "MC-DOS goes on the first partition");
        DiskPartitions.open(pc.diskInSlot(slot), 2);
        helper.assertTrue(pc.installOs(UNIX, slot), "UNIX on the second");
        helper.startSequence()
                .thenExecute(pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc) && MC_DOS.equals(pc.installedOsId()),
                        "the active partition, MC-DOS's, starts: " + pc.installedOsId()))
                .thenExecute(() -> {
                    pc.togglePower();
                    final DiskLayout now = DiskPartitions.layout(pc.diskInSlot(slot));
                    DiskPartitions.relayout(pc.diskInSlot(slot), now.activate(2).layout());
                    pc.togglePower();
                })
                .thenWaitUntil(() -> helper.assertTrue(up(pc) && UNIX.equals(pc.installedOsId()),
                        "made active, UNIX's starts next: " + pc.installedOsId()))
                .thenSucceed();
    }

    /* Whether the machine is on and its system has come up. */
    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }

    /* A Legacy 40 GB disk with a 32 GB NTFS partition, open, and an 8 GB FAT32 one beside it. */
    private static ItemStack twoPartitions() {
        final ItemStack disk = new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get());
        DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, LEGACY_40G_MB);
        layout = layout.create(0L, 32_768L, FileSystemType.NTFS, PartitionRole.DATA).layout();
        layout = layout.create(32_768L, 8_192L, FileSystemType.FAT32, PartitionRole.DATA).layout();
        DiskPartitions.relayout(disk, layout);
        return disk;
    }
}
