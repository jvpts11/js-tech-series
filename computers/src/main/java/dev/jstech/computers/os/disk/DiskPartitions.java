/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.util.Sizes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * How a disk's partitions are read and changed (computers/docs/design/storage-devices.md). Every partition is a volume:
 * it holds what a disk with no table holds, items, files, systems and its console, in the same components. The open
 * partition, the one the disk's machine started from, keeps them on the disk itself, so what reads the disk a system
 * runs from reads it as before; every other partition keeps them in the table and is read through a <em>view</em>, a
 * copy of the disk carrying that partition's components, which {@link #commit} writes back. A disk nothing has
 * partitioned reads as one partition over all of it, open, its file system the one its system was installed with, or
 * FAT on a plain disk.
 *
 * <p>This is the one class that knows the open partition is kept apart from the others; everything else asks it.
 */
public final class DiskPartitions {

    /*
     * What a volume holds, kept in the disk's own components for its open partition and with the table for the others.
     * The disk's health, its bad sectors, is the disk's and not a partition's, so it is not among them.
     */
    private static final List<Supplier<? extends DataComponentType<?>>> HELD = List.of(
            ComputingComponents.DISK_VOLUME, ComputingComponents.DISK_USAGE, ComputingComponents.FILESYSTEM,
            ComputingComponents.DISK_SYSTEMS, ComputingComponents.DISK_CONSOLE, ComputingComponents.DISK_UNCLEAN,
            ComputingComponents.DISK_PUBLIC_PERMILLE, ComputingComponents.VOLUME_LABEL, ComputingComponents.ZFS_MEMBER);
    private static final PartitionTable NONE = new PartitionTable(DiskLayout.empty(PartitionScheme.MBR, 0L), List.of(),
            0);

    private DiskPartitions() {
    }

    /**
     * The scheme an installer writes on a machine of {@code era}: the Master Boot Record under a BIOS, from the Vintage
     * to the Transition, and the GUID Partition Table under UEFI, from the Standard on.
     */
    public static PartitionScheme schemeFor(@Nullable final Era era) {
        return era != null && era.isAtLeast(HardwareEra.STANDARD) ? PartitionScheme.GPT : PartitionScheme.MBR;
    }

    /** Whether a table has been written on the disk; one nothing has partitioned reads as one partition over it all. */
    public static boolean written(final ItemStack disk) {
        return disk.has(ComputingComponents.PARTITION_TABLE.get());
    }

    /** Whether the stack is a partition's view rather than a disk. */
    public static boolean isView(final ItemStack stack) {
        return stack.has(ComputingComponents.PARTITION_VIEW.get());
    }

    /** The disk's partition table: the one written on it, or one partition over it all when none is. */
    public static PartitionTable table(final ItemStack disk) {
        final PartitionTable written = disk.get(ComputingComponents.PARTITION_TABLE.get());
        if (written != null) {
            return written;
        }
        final DiskSpec spec = HardwareRegistry.disk(disk);
        if (spec == null || isView(disk)) {
            return NONE;
        }
        return new PartitionTable(DiskLayout.whole(schemeFor(spec.era()), spec.capacityMb(),
                plainFileSystemOf(disk, spec.era())), List.of(), 1);
    }

    /** The disk's table as the tools see it. */
    public static DiskLayout layout(final ItemStack disk) {
        return table(disk).layout();
    }

    /**
     * The partition a volume is: a view's own, or the disk's open one (the one over it all when nothing has partitioned
     * it); null for a disk whose table has no partition open.
     */
    @Nullable
    public static PartitionEntry partitionOf(final ItemStack volume) {
        final PartitionView view = volume.get(ComputingComponents.PARTITION_VIEW.get());
        if (view != null) {
            return new PartitionEntry(view.number(), 0L, view.sizeMb(), FileSystemType.byName(view.fs()),
                    PartitionRole.DATA, false);
        }
        return table(volume).openPartition();
    }

    /** How big a volume is, in megabytes: its partition's size, or nothing when it has none open. */
    public static long sizeMbOf(final ItemStack volume) {
        final PartitionEntry partition = partitionOf(volume);
        return partition == null ? 0L : partition.sizeMb();
    }

    /** How many items a volume holds when empty: its partition's size counted in the items of the disk's era. */
    public static long capacityItems(final ItemStack volume) {
        final DiskSpec spec = HardwareRegistry.disk(volume);
        return spec == null ? 0L : EraStorage.itemsFor(spec.era(), sizeMbOf(volume));
    }

    /**
     * What is free on a volume, in megabytes, counted as the room a system or a file would really have: a volume
     * holding anything at all, a file smaller than a megabyte, is never taken for an empty one.
     */
    public static long freeMbOf(final ItemStack volume) {
        final DiskSpec spec = HardwareRegistry.disk(volume);
        final long sizeMb = sizeMbOf(volume);
        if (spec == null) {
            return sizeMb;
        }
        /*
         * Counted from what is held, rounded up: the capacity is whole items, which a size that is not a multiple of
         * an item's megabytes rounds up past the partition, so counting from the free weight would call room free
         * that is not there, and a resize would take a partition below what it holds.
         */
        final long capacityWeight = capacityItems(volume) * StorageKey.MB_EQ_PER_ITEM;
        final long usedWeight = Math.max(0L, capacityWeight - OsDisks.systemDiskFreeWeight(volume));
        final long usedMb = Sizes.ceilDiv(Sizes.times(usedWeight, EraStorage.mbPerItem(spec.era())),
                StorageKey.MB_EQ_PER_ITEM);
        return Math.max(0L, sizeMb - usedMb);
    }

    /** The file system of a volume, or null for a raw partition or a disk with nothing open. */
    @Nullable
    public static FileSystemType fileSystemOf(final ItemStack volume) {
        final PartitionEntry partition = partitionOf(volume);
        return partition == null ? null : partition.fs();
    }

    /** Writes {@code table} on the disk. */
    public static void write(final ItemStack disk, final PartitionTable table) {
        disk.set(ComputingComponents.PARTITION_TABLE.get(), table);
    }

    /**
     * Partition {@code number} of the disk as a volume to read and change: the disk itself for its open partition, and
     * for any other a view, which {@link #commit} writes back.
     */
    public static ItemStack view(final ItemStack disk, final int number) {
        final PartitionTable table = table(disk);
        if (table.open() == number) {
            return disk;
        }
        final PartitionEntry partition = table.layout().find(number);
        if (partition == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack view = disk.copy();
        clearHeld(view);
        view.remove(ComputingComponents.PARTITION_TABLE.get());
        view.applyComponents(table.heldBy(number));
        view.set(ComputingComponents.PARTITION_VIEW.get(), new PartitionView(number, partition.sizeMb(),
                partition.fs() == null ? "" : partition.fs().serializedName()));
        return view;
    }

    /** Writes what a view of one of the disk's partitions holds back with the disk; the disk itself needs nothing. */
    public static void commit(final ItemStack disk, final ItemStack view) {
        if (view == disk) {
            return;
        }
        final PartitionView mark = view.get(ComputingComponents.PARTITION_VIEW.get());
        final PartitionTable table = table(disk);
        if (mark == null || table.open() == mark.number() || table.layout().find(mark.number()) == null) {
            return;
        }
        write(disk, table.holding(mark.number(), heldOf(view)));
    }

    /**
     * Opens partition {@code number}, as the disk's machine starts from it: what the open one held goes with the table,
     * and what this one holds comes onto the disk.
     */
    public static void open(final ItemStack disk, final int number) {
        final PartitionTable table = table(disk);
        if (table.open() == number || table.layout().find(number) == null) {
            return;
        }
        PartitionTable next = table;
        if (table.open() != 0) {
            next = next.holding(table.open(), heldOf(disk));
        }
        final DataComponentPatch incoming = next.heldBy(number);
        clearHeld(disk);
        disk.applyComponents(incoming);
        write(disk, next.holding(number, DataComponentPatch.EMPTY).opened(number));
    }

    /**
     * The disk's partitions that hold a file system a system may read, each as a volume; the second member of a ZFS
     * mirror holds nothing of its own and is none.
     */
    public static List<Volume> volumes(final ItemStack disk) {
        final List<Volume> out = new ArrayList<>();
        for (final PartitionEntry partition : layout(disk).partitions()) {
            if (partition.holdsData()) {
                final ItemStack view = view(disk, partition.number());
                if (!ZfsMirrors.secondMember(view)) {
                    out.add(new Volume(disk, partition, view));
                }
            }
        }
        return out;
    }

    /**
     * The volumes a machine keeps its items on, from its disks in their order: a disk nothing has partitioned is one
     * volume, as is an empty slot (holding nothing), so the volumes of a machine with no table are its disks one for
     * one; a partitioned disk gives each partition that holds data.
     */
    public static List<Volume> storeVolumes(final List<ItemStack> disks) {
        final List<Volume> out = new ArrayList<>(disks.size());
        for (final ItemStack disk : disks) {
            if (written(disk)) {
                out.addAll(volumes(disk));
            } else {
                out.add(new Volume(disk, partitionOf(disk), disk));
            }
        }
        return out;
    }

    /**
     * Writes {@code layout} as the disk's table: what every partition gone from it held is erased, items and all, and
     * a partition made in it is given a volume of its own, empty.
     */
    public static void relayout(final ItemStack disk, final DiskLayout layout) {
        final PartitionTable table = table(disk);
        for (final PartitionEntry was : table.layout().partitions()) {
            if (layout.find(was.number()) == null) {
                erase(disk, table, was.number());
            }
        }
        PartitionTable next = table(disk).withLayout(layout);
        for (final PartitionEntry now : layout.partitions()) {
            if (now.number() != next.open() && now.holdsData()
                    && next.heldBy(now.number()).get(ComputingComponents.DISK_VOLUME.get()) == null) {
                next = next.holding(now.number(), DataComponentPatch.builder()
                        .set(ComputingComponents.DISK_VOLUME.get(), UUID.randomUUID()).build());
            }
        }
        write(disk, next);
    }

    /**
     * Takes the table off the disk, erasing what every partition but the open one held: the disk reads as one partition
     * over all of it again, holding what its own components hold, which a whole-disk format erases next.
     */
    public static void unpartition(final ItemStack disk) {
        if (!written(disk)) {
            return;
        }
        final PartitionTable table = table(disk);
        for (final PartitionEntry partition : table.layout().partitions()) {
            if (partition.number() != table.open()) {
                erase(disk, table, partition.number());
            }
        }
        disk.remove(ComputingComponents.PARTITION_TABLE.get());
    }

    /** Erases what partition {@code number} holds, items and all, leaving it formatted and empty. */
    public static void wipe(final ItemStack disk, final int number) {
        final PartitionTable table = table(disk);
        if (table.layout().find(number) == null) {
            return;
        }
        erase(disk, table, number);
        relayout(disk, table(disk).layout());
    }

    /*
     * Erases what partition {@code number} holds: the items its volume keeps, and its components, on the disk for the
     * open one and with the table for another.
     */
    private static void erase(final ItemStack disk, final PartitionTable table, final int number) {
        if (table.open() == number) {
            DriveVolumes.erase(disk);
            clearHeld(disk);
            return;
        }
        final ItemStack view = view(disk, number);
        if (!view.isEmpty()) {
            DriveVolumes.erase(view);
        }
        write(disk, table.holding(number, DataComponentPatch.EMPTY));
    }

    /*
     * The file system a disk nothing has partitioned is taken to have: the one its system's installer formats with, or
     * FAT on a plain disk, which every system reads.
     */
    private static FileSystemType plainFileSystemOf(final ItemStack disk, final Era era) {
        final ResourceLocation osId = OsDisks.systemOn(disk);
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        return os != null ? OsRegistry.fileSystemOf(os, era) : FileSystemType.plainFor(era);
    }

    private static void clearHeld(final ItemStack stack) {
        for (final Supplier<? extends DataComponentType<?>> type : HELD) {
            stack.remove(type.get());
        }
    }

    private static DataComponentPatch heldOf(final ItemStack stack) {
        final DataComponentPatch.Builder patch = DataComponentPatch.builder();
        for (final Supplier<? extends DataComponentType<?>> type : HELD) {
            copy(patch, stack, type.get());
        }
        return patch.build();
    }

    private static <T> void copy(final DataComponentPatch.Builder patch, final ItemStack stack,
                                 final DataComponentType<T> type) {
        final T value = stack.get(type);
        if (value != null) {
            patch.set(type, value);
        }
    }

    /**
     * One partition of a disk as a volume.
     *
     * @param disk      the disk it is on
     * @param partition the partition, or null for an empty slot
     * @param stack     what it holds: the disk itself for the open one, a view for another
     */
    public record Volume(ItemStack disk, @Nullable PartitionEntry partition, ItemStack stack) {

        /** Writes what the volume holds back with its disk, after a change to it. */
        public void commit() {
            DiskPartitions.commit(disk, stack);
        }
    }
}
