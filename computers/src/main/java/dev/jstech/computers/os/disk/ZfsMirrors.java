/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * ZFS mirrors (computers/docs/design/storage-devices.md): two partitions on two disks holding one pool. The pool is
 * read from the first, which keeps what it holds; the second disk is what ZFS repairs it from, as it reads a damaged
 * part and when it scrubs. The second member is never a volume of its own.
 */
public final class ZfsMirrors {

    private ZfsMirrors() {
    }

    /** Makes partition {@code firstNumber} of {@code first} and {@code secondNumber} of {@code second} one pool. */
    public static void pair(final ItemStack first, final int firstNumber, final ItemStack second,
                            final int secondNumber) {
        final UUID pool = UUID.randomUUID();
        mark(first, firstNumber, ZfsMember.ofMirror(pool, true));
        mark(second, secondNumber, ZfsMember.ofMirror(pool, false));
    }

    /** The volume's place in a mirror, or null when it is in none. */
    @Nullable
    public static ZfsMember memberOf(final ItemStack volume) {
        return volume.get(ComputingComponents.ZFS_MEMBER.get());
    }

    /** Whether the volume is the second member of a mirror, which holds nothing of its own. */
    public static boolean secondMember(final ItemStack volume) {
        final ZfsMember member = memberOf(volume);
        return member != null && !member.primary();
    }

    /**
     * Where the second member of the pool {@code volume} is the first of sits among a machine's {@code disks}, on a
     * disk still alive; null when it is not there, which leaves the pool nothing to repair from.
     */
    @Nullable
    public static Partner partnerOf(final ItemStack volume, final List<ItemStack> disks) {
        final ZfsMember member = memberOf(volume);
        if (member == null || !member.primary() || !member.mirror()) {
            return null;
        }
        for (final ItemStack disk : disks) {
            if (!DriveVolumes.isDisk(disk) || DriveVolumes.health(disk).dead()) {
                continue;
            }
            for (final PartitionEntry partition : DiskPartitions.layout(disk).partitions()) {
                final ZfsMember other = partition.holdsData()
                        ? memberOf(DiskPartitions.view(disk, partition.number())) : null;
                if (other != null && !other.primary() && other.pool().equals(member.pool())) {
                    return new Partner(disk, partition.number());
                }
            }
        }
        return null;
    }

    /**
     * Repairs the corrupted items of {@code key} from the pool's second member, as ZFS does when it reads a damaged
     * part. The volume is the pool's first member, the disk itself or a view the caller commits. Returns the data
     * weight repaired; nothing for a pool with no second member here.
     */
    public static long heal(final ItemStack volume, final List<ItemStack> disks, final StorageKey key) {
        return heal(volume, disks, key, true, false);
    }

    /**
     * Repairs the system files corruption put aside, as ZFS does when the machine starts and reads them. Returns the
     * data weight repaired; nothing for a pool with no second member here.
     */
    public static long healFiles(final ItemStack volume, final List<ItemStack> disks) {
        final StorageVolume store = DriveVolumes.peek(volume);
        return store.corruptedFiles().files().isEmpty() ? 0L : heal(volume, disks, null, false, true);
    }

    /**
     * Scrubs the pool at {@code time}: reads all of it, repairing every damaged part from the second member, and keeps
     * what it repaired for {@code zpool status}. Without a second member nothing is repaired, and the damage stays.
     */
    public static long scrub(final ItemStack volume, final List<ItemStack> disks, final long time) {
        final long repaired = heal(volume, disks, null, true, true);
        final ZfsMember member = memberOf(volume);
        if (member != null) {
            volume.set(ComputingComponents.ZFS_MEMBER.get(), member.scrubbed(time, repaired));
        }
        return repaired;
    }

    /*
     * Repairs from the second member, with {@code items}, the corrupted items of {@code key} (every one for null), and
     * with {@code files} the system files, counting what it repaired on the first member.
     */
    private static long heal(final ItemStack volume, final List<ItemStack> disks, @Nullable final StorageKey key,
                             final boolean items, final boolean files) {
        final ZfsMember member = memberOf(volume);
        if (member == null || partnerOf(volume, disks) == null) {
            return 0L;
        }
        final StorageVolume store = DriveVolumes.peek(volume);
        long weight = 0L;
        for (final Map.Entry<StorageKey, Long> lost : new ArrayList<>(store.corrupted().entrySet())) {
            if (items && (key == null || key.equals(lost.getKey()))) {
                weight += lost.getKey().weight(store.recover(lost.getKey(), lost.getValue()));
            }
        }
        if (files && !store.corruptedFiles().files().isEmpty()) {
            FilesystemContents now = volume.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                    FilesystemContents.EMPTY);
            for (final StoredFile file : new ArrayList<>(store.corruptedFiles().files().values())) {
                now = now.with(file);
                store.settleFile(file.path());
                weight += file.weight(DiskFilesystem.eraOf(volume));
            }
            volume.set(ComputingComponents.FILESYSTEM.get(), now);
        }
        if (weight > 0L) {
            volume.set(ComputingComponents.ZFS_MEMBER.get(), member.repairedMore(weight));
            DriveVolumes.refreshUsage(volume, store);
        }
        return weight;
    }

    private static void mark(final ItemStack disk, final int number, final ZfsMember member) {
        final ItemStack volume = DiskPartitions.view(disk, number);
        if (volume.isEmpty()) {
            return;
        }
        volume.set(ComputingComponents.ZFS_MEMBER.get(), member);
        DiskPartitions.commit(disk, volume);
    }

    /**
     * Where a pool's second member is.
     *
     * @param disk   the disk it is on
     * @param number its partition
     */
    public record Partner(ItemStack disk, int number) {
    }
}
