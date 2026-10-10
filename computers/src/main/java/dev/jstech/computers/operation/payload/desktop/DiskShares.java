/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Where the space of a disk went, in storage weight: what is stored on it, its files, and the room a system on it
 * keeps for itself. Every window that reports a disk's use derives its figure from these three, so two windows
 * cannot disagree about the same disk.
 *
 * @param storageWeight the weight of the items and fluids stored on the disk
 * @param fileWeight    the weight of the files in its filesystem
 * @param systemWeight  the weight reserved for the system installed on it, zero when there is none
 */
record DiskShares(long storageWeight, long fileWeight, long systemWeight) {

    private static final DiskShares NONE = new DiskShares(0L, 0L, 0L);

    /** The shares of a disk, or none for a stack that is not a disk. */
    static DiskShares of(final ItemStack stack) {
        if (!(stack.getItem() instanceof DiskItem diskItem)) {
            return NONE;
        }
        final ResourceLocation systemId = OsDisks.systemOn(stack);
        final OsDef system = systemId != null ? OsRegistry.getOs(systemId) : null;
        final long reserved = system != null
                ? system.footprintItemsOn(diskItem.spec().era()) * StorageKey.MB_EQ_PER_ITEM : 0L;
        return new DiskShares(DriveVolumes.usedWeight(stack), DiskFilesystem.filesWeight(stack), reserved);
    }

    /** The capacity of a disk in megabytes, zero for a stack that is not a disk. */
    static long capacityMbOf(final ItemStack stack) {
        return stack.getItem() instanceof DiskItem item ? item.spec().capacityMb() : 0L;
    }

    /** What is stored on the disk, in whole items. */
    long storageItems() {
        return storageWeight / StorageKey.MB_EQ_PER_ITEM;
    }

    /** What its files take, in whole items. */
    long fileItems() {
        return fileWeight / StorageKey.MB_EQ_PER_ITEM;
    }

    /** What its system keeps for itself, in whole items. */
    long systemItems() {
        return systemWeight / StorageKey.MB_EQ_PER_ITEM;
    }

    /** All three shares together, in whole items. */
    long usedItems() {
        return storageItems() + fileItems() + systemItems();
    }

    /**
     * All three shares together in megabytes. Megabytes follow the disk's own era, since what an item costs there is
     * what its usage is worth.
     */
    long usedMb(final long mbPerItem) {
        return (storageWeight + fileWeight + systemWeight) * mbPerItem / StorageKey.MB_EQ_PER_ITEM;
    }
}
