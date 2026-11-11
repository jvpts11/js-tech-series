/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.computers.storage.VolumeSnapshot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshots of a partition (computers/docs/design/storage-devices.md): btrfs and ZFS keep one at once, the items and
 * the files as they are, and rolling back returns the partition there, the cure for a corruption. ZFS rolls back at
 * once with {@code zfs rollback}; btrfs starts from the snapshot at the next start, after {@code btrfs subvolume
 * set-default}. A snapshot weighs what changed since it was taken.
 *
 * <p>Every method takes the partition as a volume, the disk itself for its open partition or a view the caller commits.
 */
public final class DiskSnapshots {

    /** The first number btrfs gives a snapshot: 256 and 257 are the root and home its installers lay out. */
    public static final int FIRST_NUMBER = 258;

    private DiskSnapshots() {
    }

    /** Takes a snapshot of the partition named {@code name} at {@code time}, or says why not. */
    @Nullable
    public static DiskReport.Refusal take(final ItemStack volume, final String name, final long time) {
        final FileSystemType fs = DiskPartitions.fileSystemOf(volume);
        if (fs == null || !fs.snapshots()) {
            return DiskReport.Refusal.NO_SNAPSHOTS;
        }
        final StorageVolume store = DriveVolumes.of(volume);
        if (find(store, name) != null) {
            return DiskReport.Refusal.SNAPSHOT_EXISTS;
        }
        int number = FIRST_NUMBER;
        for (final VolumeSnapshot kept : store.snapshots()) {
            number = Math.max(number, kept.number() + 1);
        }
        store.keep(new VolumeSnapshot(UUID.randomUUID(), number, name, time, store.items(),
                volume.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY)));
        DriveVolumes.refreshUsage(volume, store);
        return null;
    }

    /** The snapshot of the partition named {@code name}, or null. */
    @Nullable
    public static VolumeSnapshot find(final StorageVolume store, final String name) {
        for (final VolumeSnapshot kept : store.snapshots()) {
            if (kept.name().equals(name)) {
                return kept;
            }
        }
        return null;
    }

    /** The snapshot of the partition numbered {@code number}, or null. */
    @Nullable
    public static VolumeSnapshot find(final StorageVolume store, final int number) {
        for (final VolumeSnapshot kept : store.snapshots()) {
            if (kept.number() == number) {
                return kept;
            }
        }
        return null;
    }

    /**
     * Rolls the partition back to snapshot {@code name} at once, as ZFS does: refused while later snapshots are kept,
     * unless {@code letNewerGo}, when they are let go first.
     */
    @Nullable
    public static DiskReport.Refusal rollBack(final ItemStack volume, final String name, final boolean letNewerGo) {
        final StorageVolume store = DriveVolumes.peek(volume);
        final VolumeSnapshot snapshot = find(store, name);
        if (snapshot == null) {
            return DiskReport.Refusal.NO_SUCH_SNAPSHOT;
        }
        final List<VolumeSnapshot> all = store.snapshots();
        final List<VolumeSnapshot> newer = List.copyOf(all.subList(all.indexOf(snapshot) + 1, all.size()));
        if (!newer.isEmpty() && !letNewerGo) {
            return DiskReport.Refusal.NEWER_SNAPSHOTS;
        }
        for (final VolumeSnapshot later : newer) {
            store.forget(later.id());
        }
        restore(volume, store, snapshot);
        return null;
    }

    /** Lets snapshot {@code name} go, and the room only it kept with it. */
    @Nullable
    public static DiskReport.Refusal destroy(final ItemStack volume, final String name) {
        final StorageVolume store = DriveVolumes.peek(volume);
        final VolumeSnapshot snapshot = find(store, name);
        if (snapshot == null) {
            return DiskReport.Refusal.NO_SUCH_SNAPSHOT;
        }
        store.forget(snapshot.id());
        DriveVolumes.refreshUsage(volume, store);
        return null;
    }

    /**
     * Sets the snapshot numbered {@code number} as the one btrfs starts from at the next start, or, for a number that
     * is no snapshot's, the partition as it stands again.
     */
    @Nullable
    public static DiskReport.Refusal startFrom(final ItemStack volume, final int number) {
        final FileSystemType fs = DiskPartitions.fileSystemOf(volume);
        if (fs == null || !fs.snapshots()) {
            return DiskReport.Refusal.NO_SNAPSHOTS;
        }
        final StorageVolume store = DriveVolumes.peek(volume);
        final VolumeSnapshot snapshot = find(store, number);
        store.startFrom(snapshot == null ? null : snapshot.id());
        return null;
    }

    /**
     * What the partition does as the machine starts: btrfs mounts the snapshot set as its default, so the partition
     * holds what it kept from then on. Returns whether it changed.
     */
    public static boolean atStart(final ItemStack volume) {
        final StorageVolume store = DriveVolumes.peek(volume);
        final UUID starts = store.startsFrom();
        if (starts == null) {
            return false;
        }
        store.startFrom(null);
        for (final VolumeSnapshot kept : store.snapshots()) {
            if (kept.id().equals(starts)) {
                restore(volume, store, kept);
                return true;
            }
        }
        return false;
    }

    /**
     * What the snapshots keep of the partition's files that it no longer holds, in data weight: a file a snapshot has
     * that was since changed or deleted. A version two snapshots share weighs once.
     */
    public static long filesWeight(final ItemStack volume) {
        final List<VolumeSnapshot> snapshots = DriveVolumes.peek(volume).snapshots();
        if (snapshots.isEmpty()) {
            return 0L;
        }
        final FilesystemContents now = volume.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        final Set<StoredFile> kept = new HashSet<>();
        long weight = 0L;
        for (final VolumeSnapshot snapshot : snapshots) {
            for (final StoredFile file : snapshot.files().files().values()) {
                if (!file.equals(now.files().get(file.path())) && kept.add(file)) {
                    weight += file.weight(DiskFilesystem.eraOf(volume));
                }
            }
        }
        return weight;
    }

    /* Makes the partition hold what {@code snapshot} kept: its items and its files. */
    private static void restore(final ItemStack volume, final StorageVolume store, final VolumeSnapshot snapshot) {
        store.rollBack(snapshot);
        if (snapshot.files().files().isEmpty() && snapshot.files().directories().isEmpty()) {
            volume.remove(ComputingComponents.FILESYSTEM.get());
        } else {
            volume.set(ComputingComponents.FILESYSTEM.get(), snapshot.files());
        }
        DriveVolumes.refreshUsage(volume, store);
    }
}
