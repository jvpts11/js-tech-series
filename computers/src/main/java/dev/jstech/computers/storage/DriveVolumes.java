/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * How a drive item reaches its stored items. The item carries a volume id and a usage summary; the
 * contents live in {@link StorageVolumes}. Writers take the live volume through {@link #of} and refresh
 * the summary afterwards; readers that only need a size use the summary, which is valid on both sides.
 */
public final class DriveVolumes {

    /*
     * A fresh disk exposes nothing to the network until the owner publishes part of it; this keeps a
     * newly placed computer's storage private by default. Tunable.
     */
    public static final int DEFAULT_PUBLIC_PERMILLE = 0;

    private DriveVolumes() {
    }

    /** Whether {@code stack} is a disk, of this mod's or of any mod that declares one. */
    public static boolean isDisk(final ItemStack stack) {
        return HardwareRegistry.disk(stack) != null;
    }

    /**
     * The public-share permille stored on a disk stack, or the private default if the component is absent or the
     * stack is not a disk.
     */
    public static int publicPermille(final ItemStack stack) {
        if (!isDisk(stack)) {
            return DEFAULT_PUBLIC_PERMILLE;
        }
        final Integer stored = stack.get(ComputingComponents.DISK_PUBLIC_PERMILLE.get());
        return stored == null ? DEFAULT_PUBLIC_PERMILLE : DiskPrivacy.clampPermille(stored);
    }

    /** Writes a clamped public-share permille onto a disk stack (a no-op for a non-disk stack). */
    public static void setPublicPermille(final ItemStack stack, final int permille) {
        if (isDisk(stack)) {
            stack.set(ComputingComponents.DISK_PUBLIC_PERMILLE.get(), DiskPrivacy.clampPermille(permille));
        }
    }

    /**
     * The live volume behind a drive, created and stamped onto the drive on first use. A blank read-only
     * volume off the server thread, or for a stack that is not a drive.
     */
    public static StorageVolume of(final ItemStack drive) {
        if (!isDisk(drive)) {
            return StorageVolume.EMPTY;
        }
        final StorageVolumes store = StorageVolumes.current();
        if (store == null) {
            return StorageVolume.EMPTY;
        }
        UUID id = drive.get(ComputingComponents.DISK_VOLUME.get());
        if (id == null) {
            id = UUID.randomUUID();
            drive.set(ComputingComponents.DISK_VOLUME.get(), id);
        }
        return store.volume(id);
    }

    /** The live volume behind a drive when it already has one, else the blank read-only volume. */
    public static StorageVolume peek(final ItemStack drive) {
        final UUID id = drive.get(ComputingComponents.DISK_VOLUME.get());
        if (id == null) {
            return StorageVolume.EMPTY;
        }
        final StorageVolumes store = StorageVolumes.current();
        final StorageVolume volume = store == null ? null : store.find(id);
        return volume == null ? StorageVolume.EMPTY : volume;
    }

    /** An immutable copy of a drive's contents; empty when it has none or this is not the server. */
    public static ServerStorageContents contents(final ItemStack drive) {
        return peek(drive).snapshot();
    }

    /** The drive's usage summary, valid wherever the item is. */
    public static DiskUsage usage(final ItemStack drive) {
        return drive.getOrDefault(ComputingComponents.DISK_USAGE.get(), DiskUsage.EMPTY);
    }

    /** The stored data weight, from the summary. */
    public static long usedWeight(final ItemStack drive) {
        return usage(drive).usedWeight();
    }

    /** Rewrites the drive's summary from its volume; every write to the volume ends with this. */
    public static void refreshUsage(final ItemStack drive, final StorageVolume volume) {
        if (volume.isEmpty()) {
            drive.remove(ComputingComponents.DISK_USAGE.get());
        } else {
            drive.set(ComputingComponents.DISK_USAGE.get(), DiskUsage.of(volume));
        }
    }

    /** Replaces a drive's contents outright and refreshes its summary. */
    public static void write(final ItemStack drive, final Map<StorageKey, Long> contents) {
        final StorageVolume volume = of(drive);
        volume.replaceAll(contents);
        refreshUsage(drive, volume);
    }

    /** Wipes a drive's stored items: the volume is dropped from the store and the drive forgets it. */
    public static void erase(final ItemStack drive) {
        final UUID id = drive.get(ComputingComponents.DISK_VOLUME.get());
        if (id != null) {
            final StorageVolumes store = StorageVolumes.current();
            if (store != null) {
                store.remove(id);
            }
        }
        drive.remove(ComputingComponents.DISK_VOLUME.get());
        drive.remove(ComputingComponents.DISK_USAGE.get());
    }
}
