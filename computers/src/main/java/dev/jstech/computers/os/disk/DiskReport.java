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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's disks as its disk tools see them (computers/docs/design/storage-devices.md): each disk with its table and
 * every partition on it, what each holds and whether the running system has it in use, and why the last change asked of
 * them was refused, if it was. Disk Management, the KDE Partition Manager, GNOME Disks, diskpart, FDISK, fdisk, lsblk,
 * gpart and disklabel all read the same report, so they never disagree about a disk.
 *
 * <p>It travels as text, one line a disk or a partition, the way a screen asks the machine for a file. Pure.
 *
 * @param disks the disks, in the order the machine has them
 * @param error why the last change was refused, by its {@link Refusal}'s name, or empty
 */
public record DiskReport(List<Disk> disks, String error) {

    /** A report with no disks, for a machine that could not be read. */
    public static final DiskReport NONE = new DiskReport(List.of(), "");

    private static final String DISK = "disk";
    private static final String PART = "part";
    private static final String SNAP = "snap";
    private static final String POOL = "pool";
    private static final String ERROR = "error";
    private static final String TAB = "\t";

    public DiskReport {
        disks = List.copyOf(disks);
        error = error == null ? "" : error;
    }

    /** The report written out, as it travels. */
    public String write() {
        final StringBuilder out = new StringBuilder();
        for (final Disk disk : this.disks) {
            out.append(String.join(TAB, DISK, Integer.toString(disk.index()), Integer.toString(disk.slot()),
                    clean(disk.model()), Long.toString(disk.sizeMb()), disk.scheme().serializedName())).append('\n');
            for (final Part part : disk.parts()) {
                out.append(String.join(TAB, PART, Integer.toString(part.number()), Long.toString(part.startMb()),
                        Long.toString(part.sizeMb()), Long.toString(part.usedMb()), part.fs(), clean(part.label()),
                        part.role().serializedName(), Boolean.toString(part.active()),
                        Boolean.toString(part.mounted()), clean(part.system()))).append('\n');
                for (final Snap snap : part.snapshots()) {
                    out.append(String.join(TAB, SNAP, Integer.toString(part.number()),
                            Integer.toString(snap.number()), Long.toString(snap.takenAt()),
                            Long.toString(snap.referMb()), Long.toString(snap.onlyMb()),
                            Boolean.toString(snap.startsNext()), clean(snap.name()))).append('\n');
                }
                final Pool pool = part.pool();
                if (pool != null) {
                    out.append(String.join(TAB, POOL, Integer.toString(part.number()),
                            Boolean.toString(pool.primary()), Boolean.toString(pool.mirrored()),
                            Integer.toString(pool.partnerDisk()), Integer.toString(pool.partnerNumber()),
                            Long.toString(pool.repairedMb()), Integer.toString(pool.repairs()),
                            Long.toString(pool.scrubbedAt()), Long.toString(pool.scrubRepairedMb()),
                            Integer.toString(pool.errors()))).append('\n');
                }
            }
        }
        if (!this.error.isEmpty()) {
            out.append(ERROR).append(TAB).append(this.error).append('\n');
        }
        return out.toString();
    }

    /** A report read back from what travelled; what it does not understand it leaves out. */
    public static DiskReport read(final String text) {
        final List<Disk> disks = new ArrayList<>();
        List<Part> parts = null;
        final Map<Integer, List<Snap>> snaps = new HashMap<>();
        final Map<Integer, Pool> pools = new HashMap<>();
        String error = "";
        int index = -1;
        int slot = -1;
        String model = "";
        long sizeMb = 0L;
        PartitionScheme scheme = PartitionScheme.MBR;
        for (final String line : text.split("\n")) {
            final String[] f = line.split(TAB, -1);
            if (f.length >= 6 && DISK.equals(f[0])) {
                if (parts != null) {
                    disks.add(new Disk(index, slot, model, sizeMb, scheme, joined(parts, snaps, pools)));
                }
                index = number(f[1]);
                slot = number(f[2]);
                model = f[3];
                sizeMb = big(f[4]);
                scheme = PartitionScheme.byName(f[5]);
                parts = new ArrayList<>();
                snaps.clear();
                pools.clear();
            } else if (f.length >= 11 && PART.equals(f[0]) && parts != null) {
                parts.add(new Part(number(f[1]), big(f[2]), big(f[3]), big(f[4]), f[5], f[6],
                        PartitionRole.byName(f[7]), Boolean.parseBoolean(f[8]), Boolean.parseBoolean(f[9]), f[10]));
            } else if (f.length >= 8 && SNAP.equals(f[0])) {
                snaps.computeIfAbsent(number(f[1]), key -> new ArrayList<>()).add(new Snap(number(f[2]), f[7],
                        big(f[3]), big(f[4]), big(f[5]), Boolean.parseBoolean(f[6])));
            } else if (f.length >= 11 && POOL.equals(f[0])) {
                pools.put(number(f[1]), new Pool(Boolean.parseBoolean(f[2]), Boolean.parseBoolean(f[3]),
                        number(f[4]), number(f[5]), big(f[6]), number(f[7]), big(f[8]), big(f[9]), number(f[10])));
            } else if (f.length >= 2 && ERROR.equals(f[0])) {
                error = f[1];
            }
        }
        if (parts != null) {
            disks.add(new Disk(index, slot, model, sizeMb, scheme, joined(parts, snaps, pools)));
        }
        return new DiskReport(disks, error);
    }

    /** The disk numbered {@code index}, or null. */
    @Nullable
    public Disk disk(final int index) {
        for (final Disk disk : this.disks) {
            if (disk.index() == index) {
                return disk;
            }
        }
        return null;
    }

    /** Why the last change was refused, or null when it was not. */
    @Nullable
    public Refusal refusal() {
        for (final Refusal refusal : Refusal.values()) {
            if (refusal.name().equals(this.error)) {
                return refusal;
            }
        }
        return null;
    }

    /* A disk's partitions with the snapshots and pools read after them, which name them by number. */
    private static List<Part> joined(final List<Part> parts, final Map<Integer, List<Snap>> snaps,
                                     final Map<Integer, Pool> pools) {
        final List<Part> out = new ArrayList<>(parts.size());
        for (final Part part : parts) {
            out.add(part.with(snaps.getOrDefault(part.number(), List.of()), pools.get(part.number())));
        }
        return out;
    }

    /* Tabs and line breaks belong to the format, so a name never carries one. */
    private static String clean(final String text) {
        return text == null ? "" : text.replace('\t', ' ').replace('\n', ' ');
    }

    private static int number(final String text) {
        try {
            return Integer.parseInt(text);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    private static long big(final String text) {
        try {
            return Long.parseLong(text);
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * One disk.
     *
     * @param index  its place among the machine's disks, from 0
     * @param slot   the slot it sits in
     * @param model  what its maker calls it
     * @param sizeMb how big it is, in megabytes
     * @param scheme how its table is written
     * @param parts  its partitions, in the order they lie
     */
    public record Disk(int index, int slot, String model, long sizeMb, PartitionScheme scheme, List<Part> parts) {

        public Disk {
            parts = List.copyOf(parts);
        }

        /** Its table as the tools see it. */
        public DiskLayout layout() {
            final List<PartitionEntry> entries = new ArrayList<>();
            for (final Part part : this.parts) {
                entries.add(part.entry());
            }
            return new DiskLayout(this.scheme, this.sizeMb, entries);
        }

        /** Partition {@code number}, or null. */
        @Nullable
        public Part part(final int number) {
            for (final Part part : this.parts) {
                if (part.number() == number) {
                    return part;
                }
            }
            return null;
        }

        /** Whether it has no partition at all, which is when its table may change scheme. */
        public boolean empty() {
            return this.parts.isEmpty();
        }

        /** Whether the running system has any partition of it in use. */
        public boolean inUse() {
            for (final Part part : this.parts) {
                if (part.mounted()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * One partition.
     *
     * @param number  its number
     * @param startMb where it starts, in megabytes
     * @param sizeMb  how big it is, in megabytes
     * @param usedMb  what it holds, in megabytes
     * @param fs      its file system by its serialized name, or empty for a raw one
     * @param label   its volume's label, or empty
     * @param role    what it is for
     * @param active  whether the Master Boot Record starts it
     * @param mounted whether the running system has it in use: its own, or a swap it swaps to
     * @param system  the system on it by the name a person reads, or empty
     */
    public record Part(int number, long startMb, long sizeMb, long usedMb, String fs, String label,
                       PartitionRole role, boolean active, boolean mounted, String system, List<Snap> snapshots,
                       @Nullable Pool pool) {

        public Part {
            snapshots = List.copyOf(snapshots);
        }

        /** A partition with no snapshots and in no ZFS pool. */
        public Part(final int number, final long startMb, final long sizeMb, final long usedMb, final String fs,
                    final String label, final PartitionRole role, final boolean active, final boolean mounted,
                    final String system) {
            this(number, startMb, sizeMb, usedMb, fs, label, role, active, mounted, system, List.of(), null);
        }

        /** The same partition, with its snapshots and its place in a pool. */
        public Part with(final List<Snap> kept, @Nullable final Pool in) {
            return new Part(this.number, this.startMb, this.sizeMb, this.usedMb, this.fs, this.label, this.role,
                    this.active, this.mounted, this.system, kept, in);
        }

        /** Its snapshot named {@code name}, or null. */
        @Nullable
        public Snap snapshot(final String name) {
            for (final Snap snap : this.snapshots) {
                if (snap.name().equals(name)) {
                    return snap;
                }
            }
            return null;
        }

        /** Its file system, or null for a raw partition. */
        @Nullable
        public FileSystemType fileSystem() {
            return this.fs.isEmpty() ? null : FileSystemType.byName(this.fs);
        }

        /** What is free in it, in megabytes. */
        public long freeMb() {
            return Math.max(0L, this.sizeMb - this.usedMb);
        }

        /** Where it ends, in megabytes. */
        public long endMb() {
            return this.startMb + this.sizeMb;
        }

        /** Whether it is a swap partition. */
        public boolean swap() {
            return this.fileSystem() == FileSystemType.SWAP;
        }

        /** The partition as its table has it. */
        public PartitionEntry entry() {
            return new PartitionEntry(this.number, this.startMb, this.sizeMb, this.fileSystem(), this.role,
                    this.active);
        }
    }

    /**
     * One snapshot of a partition.
     *
     * @param number     the number btrfs lists it by
     * @param name       the name it was taken under
     * @param takenAt    the world's time of day it was taken, in ticks
     * @param referMb    what it keeps, in megabytes
     * @param onlyMb     what only it keeps, in megabytes: the room letting it go gives back
     * @param startsNext whether btrfs starts from it at the next start
     */
    public record Snap(int number, String name, long takenAt, long referMb, long onlyMb, boolean startsNext) {
    }

    /**
     * A ZFS partition's pool, as {@code zpool status} reports it.
     *
     * @param primary         whether this partition is the member the pool is read from, or the only one
     * @param mirrored        whether the pool is a mirror of two disks
     * @param partnerDisk     the disk the other member is on, or -1 when it is not in the machine
     * @param partnerNumber   the other member's partition, or -1
     * @param repairedMb      what the pool repaired from the other member, in megabytes
     * @param repairs         how many times it found damage and repaired it
     * @param scrubbedAt      the world's time of day of its last scrub, in ticks, or -1
     * @param scrubRepairedMb what its last scrub repaired, in megabytes
     * @param errors          how many items and files corruption damaged that are still not repaired
     */
    public record Pool(boolean primary, boolean mirrored, int partnerDisk, int partnerNumber, long repairedMb,
                       int repairs, long scrubbedAt, long scrubRepairedMb, int errors) {

        /** Whether its mirror is missing a member, which leaves it nothing to repair from. */
        public boolean degraded() {
            return this.mirrored && this.partnerDisk < 0;
        }
    }

    /**
     * Why a disk tool's change was refused: the table's own rules, and those of a disk the running system uses.
     */
    public enum Refusal {
        /* The Master Boot Record already holds its four. */
        TABLE_FULL,
        /* There is not that much free room where it was asked for. */
        NO_ROOM,
        NO_SUCH_PARTITION,
        NO_SUCH_DISK,
        /* That file system is not made on a partition so big. */
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
        NOT_EMPTY,
        /* The running system has it in use: the partition it runs from, or a swap it swaps to. */
        MOUNTED,
        /* Only the Master Boot Record has an active partition. */
        NOT_MBR,
        /* A file system of a later era than the machine. */
        NOT_IN_THIS_ERA,
        /* A file system that keeps no snapshots. */
        NO_SNAPSHOTS,
        SNAPSHOT_EXISTS,
        NO_SUCH_SNAPSHOT,
        /* ZFS rolls back past later snapshots only when told to let them go. */
        NEWER_SNAPSHOTS,
        /* A pool's verbs on a partition that is none. */
        NOT_ZFS,
        /* A volume with no system on it, which nothing starts from. */
        NO_SYSTEM;

        /** The refusal the table gave for a change, as a disk tool says it. */
        public static Refusal of(final DiskLayout.Refusal refusal) {
            return switch (refusal) {
                case TABLE_FULL -> TABLE_FULL;
                case NO_ROOM -> NO_ROOM;
                case NO_SUCH_PARTITION -> NO_SUCH_PARTITION;
                case TOO_BIG_FOR_ITS_FILE_SYSTEM -> TOO_BIG_FOR_ITS_FILE_SYSTEM;
                case BELOW_WHAT_IT_HOLDS -> BELOW_WHAT_IT_HOLDS;
                case GROWS_ONLY_UNMOUNTED -> GROWS_ONLY_UNMOUNTED;
                case SHRINKS_ONLY_UNMOUNTED -> SHRINKS_ONLY_UNMOUNTED;
                case NEVER_SHRINKS -> NEVER_SHRINKS;
                case NOT_EMPTY -> NOT_EMPTY;
            };
        }
    }
}
