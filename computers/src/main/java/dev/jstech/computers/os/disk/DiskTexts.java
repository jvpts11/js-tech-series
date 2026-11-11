/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the disk tools say, as each player reads it: the name of a partition table's scheme, and why a change was
 * refused, the same words in Disk Management, the KDE Partition Manager, GNOME Disks and at the prompt.
 */
@TextHolder
public final class DiskTexts {

    static final TextKey MBR = TextKey.of("jsc.disk.scheme.mbr", "Master Boot Record");
    static final TextKey GPT = TextKey.of("jsc.disk.scheme.gpt", "GUID Partition Table");

    private static final TextKey TABLE_FULL = TextKey.of("jsc.disk.refusal.table_full",
            "This disk already has four partitions, all an MBR disk can hold.");
    private static final TextKey NO_ROOM = TextKey.of("jsc.disk.refusal.no_room",
            "There is not enough free space there.");
    private static final TextKey NO_SUCH_PARTITION = TextKey.of("jsc.disk.refusal.no_such_partition",
            "There is no such partition.");
    private static final TextKey NO_SUCH_DISK = TextKey.of("jsc.disk.refusal.no_such_disk", "There is no such disk.");
    private static final TextKey TOO_BIG = TextKey.of("jsc.disk.refusal.too_big",
            "That file system cannot be made on a partition so big.");
    private static final TextKey BELOW_WHAT_IT_HOLDS = TextKey.of("jsc.disk.refusal.below_what_it_holds",
            "A volume never shrinks below what it holds.");
    private static final TextKey GROWS_ONLY_UNMOUNTED = TextKey.of("jsc.disk.refusal.grows_only_unmounted",
            "Its file system grows only unmounted.");
    private static final TextKey SHRINKS_ONLY_UNMOUNTED = TextKey.of("jsc.disk.refusal.shrinks_only_unmounted",
            "It is mounted: its file system grows while mounted, but shrinks only unmounted, from a live medium or"
                    + " another system.");
    private static final TextKey NEVER_SHRINKS = TextKey.of("jsc.disk.refusal.never_shrinks",
            "Its file system grows, and never shrinks.");
    private static final TextKey NOT_EMPTY = TextKey.of("jsc.disk.refusal.not_empty",
            "The disk is not empty. Only an empty disk can be converted.");
    private static final TextKey MOUNTED = TextKey.of("jsc.disk.refusal.mounted",
            "It is in use by the running system.");
    private static final TextKey NOT_MBR = TextKey.of("jsc.disk.refusal.not_mbr",
            "Only a disk with a Master Boot Record has an active partition.");
    private static final TextKey NOT_IN_THIS_ERA = TextKey.of("jsc.disk.refusal.not_in_this_era",
            "This machine cannot make that file system yet.");
    private static final TextKey NO_SNAPSHOTS = TextKey.of("jsc.disk.refusal.no_snapshots",
            "Its file system keeps no snapshots: btrfs, ZFS and APFS do.");
    private static final TextKey SNAPSHOT_EXISTS = TextKey.of("jsc.disk.refusal.snapshot_exists",
            "A snapshot of that name is already there.");
    private static final TextKey NO_SUCH_SNAPSHOT = TextKey.of("jsc.disk.refusal.no_such_snapshot",
            "There is no such snapshot.");
    private static final TextKey NEWER_SNAPSHOTS = TextKey.of("jsc.disk.refusal.newer_snapshots",
            "Later snapshots are kept: rolling back to this one lets them go.");
    private static final TextKey NOT_ZFS = TextKey.of("jsc.disk.refusal.not_zfs", "It is no ZFS pool.");

    private DiskTexts() {
    }

    /** Why a disk tool's change was refused, as every tool says it. */
    public static Text reason(final DiskReport.Refusal refusal) {
        return switch (refusal) {
            case TABLE_FULL -> TABLE_FULL.text();
            case NO_ROOM -> NO_ROOM.text();
            case NO_SUCH_PARTITION -> NO_SUCH_PARTITION.text();
            case NO_SUCH_DISK -> NO_SUCH_DISK.text();
            case TOO_BIG_FOR_ITS_FILE_SYSTEM -> TOO_BIG.text();
            case BELOW_WHAT_IT_HOLDS -> BELOW_WHAT_IT_HOLDS.text();
            case GROWS_ONLY_UNMOUNTED -> GROWS_ONLY_UNMOUNTED.text();
            case SHRINKS_ONLY_UNMOUNTED -> SHRINKS_ONLY_UNMOUNTED.text();
            case NEVER_SHRINKS -> NEVER_SHRINKS.text();
            case NOT_EMPTY -> NOT_EMPTY.text();
            case MOUNTED -> MOUNTED.text();
            case NOT_MBR -> NOT_MBR.text();
            case NOT_IN_THIS_ERA -> NOT_IN_THIS_ERA.text();
            case NO_SNAPSHOTS -> NO_SNAPSHOTS.text();
            case SNAPSHOT_EXISTS -> SNAPSHOT_EXISTS.text();
            case NO_SUCH_SNAPSHOT -> NO_SUCH_SNAPSHOT.text();
            case NEWER_SNAPSHOTS -> NEWER_SNAPSHOTS.text();
            case NOT_ZFS -> NOT_ZFS.text();
        };
    }
}
