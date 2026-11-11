/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.computers.os.FileSystemType;
import org.jetbrains.annotations.Nullable;

/**
 * One partition of a disk's table, where it lies and what it is: its number (the one the systems name it by, sda3 or
 * partition 3), where it starts and how big it is in megabytes, the file system it was formatted with (none while it is
 * raw, made but not yet formatted), what it is for, and whether it is the active one the Master Boot Record starts.
 * What it holds is kept apart, with the disk. Pure.
 *
 * @param number  its number, from one
 * @param startMb where it starts, in megabytes from the start of the disk
 * @param sizeMb  how big it is, in megabytes
 * @param fs      the file system it was formatted with, or null while it is raw
 * @param role    what it is for
 * @param active  whether it is the partition the Master Boot Record starts
 */
public record PartitionEntry(int number, long startMb, long sizeMb, @Nullable FileSystemType fs, PartitionRole role,
                             boolean active) {

    /** Where it ends, one past its last megabyte. */
    public long endMb() {
        return startMb + sizeMb;
    }

    /** Whether it holds a file system a system may read: formatted, not swap, and a data partition. */
    public boolean holdsData() {
        return fs != null && fs.holdsData() && role == PartitionRole.DATA;
    }

    /** Whether it is a swap partition. */
    public boolean swap() {
        return fs == FileSystemType.SWAP;
    }

    /** The same partition formatted with {@code to}, for {@code as}. */
    public PartitionEntry formatted(final FileSystemType to, final PartitionRole as) {
        return new PartitionEntry(number, startMb, sizeMb, to, as, active);
    }

    /** The same partition at {@code to} megabytes. */
    public PartitionEntry sized(final long to) {
        return new PartitionEntry(number, startMb, to, fs, role, active);
    }

    /** The same partition, active or not. */
    public PartitionEntry activated(final boolean to) {
        return new PartitionEntry(number, startMb, sizeMb, fs, role, to);
    }
}
