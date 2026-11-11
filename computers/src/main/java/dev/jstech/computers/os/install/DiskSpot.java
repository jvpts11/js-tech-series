/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import org.jetbrains.annotations.Nullable;

/**
 * One row of an installer's disk step (computers/docs/design/storage-devices.md): a partition, or a stretch of the disk
 * no partition takes, as the real installers listed them.
 *
 * @param number  the partition's number, or {@link #FREE} for free space
 * @param startMb where it starts on the disk, in megabytes
 * @param sizeMb  how big it is, in megabytes
 * @param freeMb  what is free in it, in megabytes: all of it for free space
 * @param fs      its file system by its serialized name, or empty for free space and a raw partition
 * @param holds   the system on it by the name a person reads, or empty when it holds none
 * @param label   the volume's own label, or empty
 * @param role    what the partition is for
 * @param active  whether the Master Boot Record starts it
 */
public record DiskSpot(int number, long startMb, long sizeMb, long freeMb, String fs, String holds, String label,
                       PartitionRole role, boolean active) {

    /** The number free space goes by, which no partition has. */
    public static final int FREE = 0;

    public DiskSpot {
        fs = fs == null ? "" : fs;
        holds = holds == null ? "" : holds;
        label = label == null ? "" : label;
        freeMb = Math.max(0L, Math.min(freeMb, sizeMb));
    }

    /** A stretch of free space. */
    public static DiskSpot free(final long startMb, final long sizeMb) {
        return new DiskSpot(FREE, startMb, sizeMb, sizeMb, "", "", "", PartitionRole.DATA, false);
    }

    /** Whether it is free space rather than a partition. */
    public boolean isFree() {
        return this.number == FREE;
    }

    /** Whether a system is on it. */
    public boolean hasSystem() {
        return !this.holds.isEmpty();
    }

    /** Its file system, or null for free space and a raw partition. */
    @Nullable
    public FileSystemType fileSystem() {
        return this.fs.isEmpty() ? null : FileSystemType.byName(this.fs);
    }

    /** Whether it is a swap partition. */
    public boolean swap() {
        return this.fileSystem() == FileSystemType.SWAP;
    }

    /** Whether it is where a system could live: a data partition with a file system that holds files. */
    public boolean holdsData() {
        final FileSystemType type = this.fileSystem();
        return !this.isFree() && this.role == PartitionRole.DATA && type != null && type.holdsData();
    }

    /** What it holds, in megabytes. */
    public long usedMb() {
        return this.sizeMb - this.freeMb;
    }

    /** Where it ends, in megabytes. */
    public long endMb() {
        return this.startMb + this.sizeMb;
    }

    /** The partition as its table has it; free space has no entry. */
    public PartitionEntry entry() {
        return new PartitionEntry(this.number, this.startMb, this.sizeMb, this.fileSystem(), this.role, this.active);
    }
}
