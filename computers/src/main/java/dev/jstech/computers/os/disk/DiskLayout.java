/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.computers.os.FileSystemType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A disk's partition table as the tools see it (computers/docs/design/storage-devices.md): its scheme, how big the disk
 * is, and its partitions, kept in the order they lie on the disk. Every change a tool or an installer makes is asked of
 * it and answered with the table it leaves or why it refuses, by the real rules: four partitions and 2 TB at most on
 * the Master Boot Record, no FAT16 partition over 2 GB, a partition never shrunk below what it holds, each file system
 * resized only as it allows, and a table turned into the other scheme only while the disk is empty. Pure: what the
 * partitions hold is the disk's, and how much a partition holds is asked of whoever has it.
 *
 * @param scheme     how the table is written
 * @param diskMb     how big the disk is, in megabytes
 * @param partitions its partitions, in the order they lie on the disk
 */
public record DiskLayout(PartitionScheme scheme, long diskMb, List<PartitionEntry> partitions) {

    public DiskLayout {
        partitions = partitions.stream().sorted(Comparator.comparingLong(PartitionEntry::startMb)).toList();
    }

    /** A disk with a table and nothing on it. */
    public static DiskLayout empty(final PartitionScheme scheme, final long diskMb) {
        return new DiskLayout(scheme, diskMb, List.of());
    }

    /** A disk that is one partition of {@code fs} over all the table reaches, the one that starts. */
    public static DiskLayout whole(final PartitionScheme scheme, final long diskMb, final FileSystemType fs) {
        return new DiskLayout(scheme, diskMb, List.of(new PartitionEntry(1, 0L, scheme.reachOf(diskMb), fs,
                PartitionRole.DATA, true)));
    }

    /** How much of the disk the table reaches: the whole disk, or its first 2 TB on the Master Boot Record. */
    public long reachMb() {
        return scheme.reachOf(diskMb);
    }

    /** The partition numbered {@code number}, or null when there is none. */
    @Nullable
    public PartitionEntry find(final int number) {
        for (final PartitionEntry partition : partitions) {
            if (partition.number() == number) {
                return partition;
            }
        }
        return null;
    }

    /** The partition the Master Boot Record starts, or null when none is marked. */
    @Nullable
    public PartitionEntry active() {
        for (final PartitionEntry partition : partitions) {
            if (partition.active()) {
                return partition;
            }
        }
        return null;
    }

    /** Whether the table has room for no more partitions: four on the Master Boot Record. */
    public boolean full() {
        return partitions.size() >= scheme.mostPartitions();
    }

    /** The stretches no partition takes, within what the table reaches, in the order they lie. */
    public List<FreeSpace> free() {
        final List<FreeSpace> gaps = new ArrayList<>();
        long at = 0L;
        for (final PartitionEntry partition : partitions) {
            if (partition.startMb() > at) {
                gaps.add(new FreeSpace(at, partition.startMb() - at));
            }
            at = Math.max(at, partition.endMb());
        }
        if (reachMb() > at) {
            gaps.add(new FreeSpace(at, reachMb() - at));
        }
        return gaps;
    }

    /** How much of what the table reaches no partition takes. */
    public long freeMb() {
        long sum = 0L;
        for (final FreeSpace gap : free()) {
            sum += gap.sizeMb();
        }
        return sum;
    }

    /**
     * A new partition of {@code sizeMb} megabytes starting at {@code atMb}, inside one free stretch, formatted with
     * {@code fs} (null leaves it raw) for {@code role}, numbered with the lowest number free.
     */
    public Change create(final long atMb, final long sizeMb, @Nullable final FileSystemType fs,
                         final PartitionRole role) {
        if (full()) {
            return Change.refused(Refusal.TABLE_FULL);
        }
        if (fs != null && sizeMb > fs.mostPartitionMb()) {
            return Change.refused(Refusal.TOO_BIG_FOR_ITS_FILE_SYSTEM);
        }
        for (final FreeSpace gap : free()) {
            if (atMb >= gap.startMb() && atMb < gap.startMb() + gap.sizeMb()) {
                // Measured as the room left from where it starts, which a size as big as a long cannot wrap past.
                if (sizeMb <= 0L || sizeMb > gap.startMb() + gap.sizeMb() - atMb) {
                    return Change.refused(Refusal.NO_ROOM);
                }
                final int number = lowestFreeNumber();
                final List<PartitionEntry> next = new ArrayList<>(partitions);
                next.add(new PartitionEntry(number, atMb, sizeMb, fs, role, false));
                return Change.made(new DiskLayout(scheme, diskMb, next), number);
            }
        }
        return Change.refused(Refusal.NO_ROOM);
    }

    /** The table without partition {@code number}, its room free again. */
    public Change delete(final int number) {
        final PartitionEntry partition = find(number);
        if (partition == null) {
            return Change.refused(Refusal.NO_SUCH_PARTITION);
        }
        final List<PartitionEntry> next = new ArrayList<>(partitions);
        next.remove(partition);
        return Change.made(new DiskLayout(scheme, diskMb, next), number);
    }

    /** Partition {@code number} formatted with {@code fs} for {@code role}: what it held is gone. */
    public Change format(final int number, final FileSystemType fs, final PartitionRole role) {
        final PartitionEntry partition = find(number);
        if (partition == null) {
            return Change.refused(Refusal.NO_SUCH_PARTITION);
        }
        if (partition.sizeMb() > fs.mostPartitionMb()) {
            return Change.refused(Refusal.TOO_BIG_FOR_ITS_FILE_SYSTEM);
        }
        return replaced(partition, partition.formatted(fs, role));
    }

    /**
     * Partition {@code number} at {@code toMb} megabytes, its start where it is: it grows only into the free stretch
     * right after it, never shrinks below the {@code heldMb} it holds, and changes only as its file system allows while
     * {@code mounted}.
     */
    public Change resize(final int number, final long toMb, final boolean mounted, final long heldMb) {
        final PartitionEntry partition = find(number);
        if (partition == null) {
            return Change.refused(Refusal.NO_SUCH_PARTITION);
        }
        if (toMb == partition.sizeMb()) {
            return Change.made(this, number);
        }
        final boolean grow = toMb > partition.sizeMb();
        if (partition.fs() != null && !partition.fs().resizes(grow, mounted)) {
            return Change.refused(grow ? Refusal.GROWS_ONLY_UNMOUNTED : mounted
                    && partition.fs().resizes(false, false) ? Refusal.SHRINKS_ONLY_UNMOUNTED : Refusal.NEVER_SHRINKS);
        }
        if (toMb < Math.max(1L, heldMb)) {
            return Change.refused(Refusal.BELOW_WHAT_IT_HOLDS);
        }
        if (partition.fs() != null && toMb > partition.fs().mostPartitionMb()) {
            return Change.refused(Refusal.TOO_BIG_FOR_ITS_FILE_SYSTEM);
        }
        if (grow && toMb - partition.sizeMb() > roomAfter(partition)) {
            return Change.refused(Refusal.NO_ROOM);
        }
        return replaced(partition, partition.sized(toMb));
    }

    /** How far partition {@code number} may grow: the free stretch right after it. */
    public long roomAfter(final PartitionEntry partition) {
        long next = reachMb();
        for (final PartitionEntry other : partitions) {
            if (other.startMb() >= partition.endMb() && other.startMb() < next) {
                next = other.startMb();
            }
        }
        return Math.max(0L, next - partition.endMb());
    }

    /** Partition {@code number} made the one the Master Boot Record starts, every other no longer. */
    public Change activate(final int number) {
        if (find(number) == null) {
            return Change.refused(Refusal.NO_SUCH_PARTITION);
        }
        final List<PartitionEntry> next = new ArrayList<>();
        for (final PartitionEntry partition : partitions) {
            next.add(partition.activated(partition.number() == number));
        }
        return Change.made(new DiskLayout(scheme, diskMb, next), number);
    }

    /** The table written in {@code to}: only on an empty disk, as Disk Management and diskpart insist. */
    public Change convert(final PartitionScheme to) {
        if (!partitions.isEmpty()) {
            return Change.refused(Refusal.NOT_EMPTY);
        }
        return Change.made(empty(to, diskMb), 0);
    }

    private Change replaced(final PartitionEntry was, final PartitionEntry now) {
        final List<PartitionEntry> next = new ArrayList<>(partitions);
        next.set(next.indexOf(was), now);
        return Change.made(new DiskLayout(scheme, diskMb, next), now.number());
    }

    private int lowestFreeNumber() {
        int number = 1;
        while (find(number) != null) {
            number++;
        }
        return number;
    }

    /** A stretch of the disk no partition takes. */
    public record FreeSpace(long startMb, long sizeMb) {
    }

    /** Why a change to the table is refused. */
    public enum Refusal {
        /* The Master Boot Record already holds its four. */
        TABLE_FULL,
        /* There is not that much free room where it was asked for. */
        NO_ROOM,
        NO_SUCH_PARTITION,
        /* FAT16 holds 2 GB at most. */
        TOO_BIG_FOR_ITS_FILE_SYSTEM,
        /* A partition never shrinks below what it holds. */
        BELOW_WHAT_IT_HOLDS,
        /* Its file system grows only unmounted. */
        GROWS_ONLY_UNMOUNTED,
        /* Its file system shrinks only unmounted, from a live medium or another system. */
        SHRINKS_ONLY_UNMOUNTED,
        /* Its file system only ever grows. */
        NEVER_SHRINKS,
        /* The scheme of a table changes only on an empty disk. */
        NOT_EMPTY
    }

    /**
     * What a change answers: the table it leaves and the number of the partition it touched, or why it was refused.
     *
     * @param layout  the table after the change, or null when it was refused
     * @param number  the partition it touched, made or changed
     * @param refusal why it was refused, or null when it was made
     */
    public record Change(@Nullable DiskLayout layout, int number, @Nullable Refusal refusal) {

        static Change made(final DiskLayout layout, final int number) {
            return new Change(layout, number, null);
        }

        static Change refused(final Refusal refusal) {
            return new Change(null, 0, refusal);
        }

        /** Whether the change was made. */
        public boolean ok() {
            return refusal == null;
        }
    }
}
