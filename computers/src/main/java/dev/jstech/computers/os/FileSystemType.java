/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * The file system a disk is formatted with, which decides how it gets back on its feet after a power loss: a file
 * system with no journal reads all it holds again, one with a journal replays it in seconds, and a copy-on-write one
 * has nothing to mend (computers/docs/design/storage-devices.md).
 *
 * <p>Pure: no game types. A disk carries no partitions yet, so a machine's disks are taken to be formatted the way its
 * system's installer formats its own ({@link #installedBy}).
 */
public enum FileSystemType implements IStableName {

    FAT16("fat16", "FAT16", Recovery.LONG_CHECK),
    FAT32("fat32", "FAT32", Recovery.LONG_CHECK),
    NTFS("ntfs", "NTFS", Recovery.JOURNAL),
    EXT3("ext3", "ext3", Recovery.JOURNAL),
    EXT4("ext4", "ext4", Recovery.JOURNAL),
    BTRFS("btrfs", "btrfs", Recovery.COPY_ON_WRITE),
    UFS("ufs", "UFS", Recovery.LONG_CHECK),
    /* Quince's own, which only its systems read and write (computers/docs/design/quince.md). */
    HFS("hfs", "HFS", Recovery.LONG_CHECK),
    HFS_PLUS("hfs_plus", "HFS+", Recovery.LONG_CHECK),
    HFS_PLUS_JOURNALED("hfs_plus_journaled", "HFS+ (Journaled)", Recovery.JOURNAL),
    APFS("apfs", "APFS", Recovery.COPY_ON_WRITE);

    private final String serializedName;
    private final String displayName;
    private final Recovery recovery;

    /* The one distribution whose installer formats btrfs, from the Standard on. */
    private static final String FEDORA = "fedora";
    private static final StableNames<FileSystemType> NAMES = StableNames.of(FileSystemType.class);

    FileSystemType(final String serializedName, final String displayName, final Recovery recovery) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.recovery = recovery;
    }

    /**
     * What the installer of a system formats with on a machine of {@code era}, as each real one did: FAT16 on MC-DOS
     * and MC-NET (FAT32 from the Legacy), FAT32 on Frames 95, NTFS on the later Frames, ext3 on a distribution of the
     * Legacy and ext4 later, btrfs on Fedora from the Standard, ext4 wherever the player formats the root by hand,
     * UFS on UNIX and the BSDs, and on Quince's systems HFS on the Quince System, HFS+ on Quince OS 9, HFS+ with its
     * journal from Quince OS X on and APFS on quinceOS.
     *
     * @param platform   the system's family
     * @param familyRank where it sits in its family, counting from one (Frames 95 is the first)
     * @param byHand     whether the player installs it by hand, typing the command that formats the root
     * @param name       the system's own name, its registry path ({@code fedora})
     * @param era        the era of the machine it is installed on; null for a machine with no board
     */
    public static FileSystemType installedBy(final Platform platform, final int familyRank, final boolean byHand,
                                             final String name, final Era era) {
        final boolean vintage = era == null || era.isAtMost(HardwareEra.VINTAGE);
        final boolean legacy = era == null || era.isAtMost(HardwareEra.LEGACY);
        return switch (platform) {
            case MC_DOS, MC_NET -> vintage ? FAT16 : FAT32;
            case FRAMES -> familyRank <= 1 ? FAT32 : NTFS;
            case LINUX -> {
                if (byHand) {
                    yield EXT4;
                }
                if (FEDORA.equals(name) && era != null && era.isAtLeast(HardwareEra.STANDARD)) {
                    yield BTRFS;
                }
                yield legacy ? EXT3 : EXT4;
            }
            case FREEBSD, OPENBSD, UNIX -> UFS;
            case QUINCE -> switch (familyRank) {
                case QuinceFamily.QUINCE_SYSTEM -> HFS;
                case QuinceFamily.OS_9 -> HFS_PLUS;
                case QuinceFamily.OS_X -> HFS_PLUS_JOURNALED;
                default -> APFS;
            };
        };
    }

    /** The file system called {@code name}; an unknown one reads as FAT16, the oldest. */
    public static FileSystemType byName(final String name) {
        return NAMES.byName(name, FAT16);
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Its name as the systems print it ({@code NTFS}, {@code ext4}). */
    public String displayName() {
        return this.displayName;
    }

    /** How it gets back on its feet after a power loss. */
    public Recovery recovery() {
        return this.recovery;
    }

    /** How a file system gets back on its feet after a power loss. */
    public enum Recovery {

        /** No journal: every allocation is read again, longer the more is in use. */
        LONG_CHECK,
        /** A journal: what was half written is replayed in a couple of seconds. */
        JOURNAL,
        /** Copy-on-write: nothing was ever half written over, so there is nothing to check. */
        COPY_ON_WRITE;

        /* A long check reads about this many items' weight a second, for no less and no longer than these. */
        private static final long ITEMS_A_SECOND = 64L;
        private static final int SHORTEST_CHECK_TICKS = 100;
        private static final int LONGEST_CHECK_TICKS = 1_200;
        private static final int JOURNAL_TICKS = 40;
        private static final long TICKS_A_SECOND = 20L;

        /**
         * How long getting back on its feet takes with {@code itemsInUse} items' weight in use, in ticks: a second for
         * every 64, from 5 seconds to a minute, without a journal; 2 seconds with one; none on copy-on-write.
         * Estimates.
         */
        public int ticksFor(final long itemsInUse) {
            return switch (this) {
                case LONG_CHECK -> (int) Math.max(SHORTEST_CHECK_TICKS,
                        Math.min(LONGEST_CHECK_TICKS, Math.max(0L, itemsInUse) * TICKS_A_SECOND / ITEMS_A_SECOND));
                case JOURNAL -> JOURNAL_TICKS;
                case COPY_ON_WRITE -> 0;
            };
        }
    }
}
