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
 * The file system a partition is formatted with (computers/docs/design/storage-devices.md): a real property of the
 * partition, which decides who reads it, how it gets back on its feet after a power loss (a file system with no journal
 * reads all it holds again, one with a journal replays it in seconds, a copy-on-write one has nothing to mend), how it
 * may be resized, and what only the modern ones do: snapshots, and the mirrors of ZFS. Swap is here too, as what a
 * swap partition is formatted with, though it keeps nothing a system reads.
 *
 * <p>Pure: no game types.
 */
public enum FileSystemType implements IStableName {

    FAT12("fat12", "FAT12", Recovery.LONG_CHECK, HardwareEra.VINTAGE, Readers.EVERY_SYSTEM, Resize.UNMOUNTED),
    FAT16("fat16", "FAT16", Recovery.LONG_CHECK, HardwareEra.VINTAGE, Readers.EVERY_SYSTEM, Resize.UNMOUNTED),
    FAT32("fat32", "FAT32", Recovery.LONG_CHECK, HardwareEra.LEGACY, Readers.EVERY_SYSTEM, Resize.UNMOUNTED),
    NTFS("ntfs", "NTFS", Recovery.JOURNAL, HardwareEra.LEGACY, Readers.FRAMES_THEN_LINUX_FROM_TRANSITION,
            Resize.MOUNTED),
    EXFAT("exfat", "exFAT", Recovery.LONG_CHECK, HardwareEra.TRANSITION, Readers.FRAMES_THEN_LINUX_FROM_ADVANCED,
            Resize.UNMOUNTED),
    EXT2("ext2", "ext2", Recovery.LONG_CHECK, HardwareEra.LEGACY, Readers.LINUX, Resize.GROWS_MOUNTED),
    EXT3("ext3", "ext3", Recovery.JOURNAL, HardwareEra.LEGACY, Readers.LINUX, Resize.GROWS_MOUNTED),
    EXT4("ext4", "ext4", Recovery.JOURNAL, HardwareEra.TRANSITION, Readers.LINUX, Resize.GROWS_MOUNTED),
    XFS("xfs", "XFS", Recovery.JOURNAL, HardwareEra.TRANSITION, Readers.LINUX, Resize.GROWS_ONLY),
    BTRFS("btrfs", "btrfs", Recovery.COPY_ON_WRITE, HardwareEra.STANDARD, Readers.LINUX, Resize.MOUNTED),
    UFS("ufs", "UFS", Recovery.LONG_CHECK, HardwareEra.VINTAGE, Readers.UNIXES, Resize.UNMOUNTED),
    ZFS("zfs", "ZFS", Recovery.COPY_ON_WRITE, HardwareEra.TRANSITION, Readers.FREEBSD, Resize.GROWS_ONLY),
    /* Quince's own, which only its systems read and write (computers/docs/design/quince.md). */
    HFS("hfs", "HFS", Recovery.LONG_CHECK, HardwareEra.VINTAGE, Readers.QUINCE, Resize.UNMOUNTED),
    HFS_PLUS("hfs_plus", "HFS+", Recovery.LONG_CHECK, HardwareEra.LEGACY, Readers.QUINCE, Resize.UNMOUNTED),
    HFS_PLUS_JOURNALED("hfs_plus_journaled", "HFS+ (Journaled)", Recovery.JOURNAL, HardwareEra.TRANSITION,
            Readers.QUINCE, Resize.UNMOUNTED),
    APFS("apfs", "APFS", Recovery.COPY_ON_WRITE, HardwareEra.ADVANCED, Readers.QUINCE, Resize.UNMOUNTED),
    /* What a swap partition holds: the part of the programs that did not fit in memory, which no system reads. */
    SWAP("swap", "swap", Recovery.NOTHING_KEPT, HardwareEra.VINTAGE, Readers.NO_SYSTEM, Resize.UNMOUNTED);

    private final String serializedName;
    private final String displayName;
    private final Recovery recovery;
    private final Era since;
    private final Readers readers;
    private final Resize resize;

    /* The one distribution whose installer formats btrfs, from the Standard on. */
    private static final String FEDORA = "fedora";
    /* A FAT16 partition holds 2 GB at most, and a FAT12 one the 32 MB of the floppy disks' day. */
    private static final long FAT16_MOST_MB = 2_048L;
    private static final long FAT12_MOST_MB = 32L;
    /* No file on a FAT file system is over 4 GB, so a system's image does not fit on one. */
    private static final long FAT_MOST_FILE_BYTES = 4L * 1024L * 1024L * 1024L - 1L;
    private static final StableNames<FileSystemType> NAMES = StableNames.of(FileSystemType.class);

    FileSystemType(final String serializedName, final String displayName, final Recovery recovery,
                   final Era since, final Readers readers, final Resize resize) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.recovery = recovery;
        this.since = since;
        this.readers = readers;
        this.resize = resize;
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

    /**
     * The file system of the kind a player's kernel chose (computers/docs/design/player-systems.md) on a machine of
     * {@code era}: of the DOS kind, FAT as MC-DOS formats; of the POSIX kind, ext as a distribution does; and a flat
     * one with no folders, FAT16, the plainest.
     */
    public static FileSystemType chosen(final String kind, final Era era) {
        final boolean vintage = era == null || era.isAtMost(HardwareEra.VINTAGE);
        final boolean legacy = era == null || era.isAtMost(HardwareEra.LEGACY);
        return switch (kind) {
            case "dos" -> vintage ? FAT16 : FAT32;
            case "posix" -> legacy ? EXT3 : EXT4;
            default -> FAT16;
        };
    }

    /**
     * What a disk of {@code era} that nothing has formatted for a system holds its items and files on: FAT, which
     * every system reads, FAT16 in the Vintage and FAT32 after.
     */
    public static FileSystemType plainFor(final Era era) {
        return era == null || era.isAtMost(HardwareEra.VINTAGE) ? FAT16 : FAT32;
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

    /** The era it came in, the first a machine may format with it. */
    public Era since() {
        return this.since;
    }

    /** Whether a machine of {@code era} may format a partition with it. */
    public boolean formattableIn(final Era era) {
        return era != null && era.isAtLeast(this.since);
    }

    /**
     * Whether a system of {@code platform} on a machine of {@code era} reads and writes it: FAT every system, NTFS
     * Frames and Linux from the Transition, exFAT Frames and Linux from the Advanced, the ext family, XFS and btrfs
     * Linux, UFS UNIX and the BSDs, ZFS FreeBSD, Quince's own Quince. Swap no system reads.
     */
    public boolean readBy(final Platform platform, final Era era) {
        return switch (this.readers) {
            case EVERY_SYSTEM -> true;
            case FRAMES_THEN_LINUX_FROM_TRANSITION -> platform == Platform.FRAMES
                    || platform == Platform.LINUX && era != null && era.isAtLeast(HardwareEra.TRANSITION);
            case FRAMES_THEN_LINUX_FROM_ADVANCED -> platform == Platform.FRAMES
                    || platform == Platform.LINUX && era != null && era.isAtLeast(HardwareEra.ADVANCED);
            case LINUX -> platform == Platform.LINUX;
            case UNIXES -> platform == Platform.UNIX || platform == Platform.FREEBSD || platform == Platform.OPENBSD;
            case FREEBSD -> platform == Platform.FREEBSD;
            case QUINCE -> platform == Platform.QUINCE;
            case NO_SYSTEM -> false;
        };
    }

    /**
     * Whether a system of {@code platform} on a machine of {@code era} installs onto a partition of it as it stands,
     * keeping what it holds: Frames on FAT and NTFS, MC-DOS and MC-NET on FAT, Linux on the ext family, XFS and btrfs,
     * FreeBSD on UFS and ZFS, UNIX and OpenBSD on UFS, Quince on its own. Anything else its installer formats first.
     */
    public boolean carriesSystem(final Platform platform, final Era era) {
        if (!this.readBy(platform, era)) {
            return false;
        }
        return switch (platform) {
            case FRAMES -> this == FAT16 || this == FAT32 || this == NTFS;
            case MC_DOS, MC_NET -> this == FAT12 || this == FAT16 || this == FAT32;
            case LINUX -> this == EXT2 || this == EXT3 || this == EXT4 || this == XFS || this == BTRFS;
            case FREEBSD, OPENBSD, UNIX -> this == UFS || this == ZFS;
            case QUINCE -> this.readers == Readers.QUINCE;
        };
    }

    /**
     * Whether a partition of it may grow ({@code grow}) or shrink while {@code mounted}: NTFS and btrfs both while the
     * system runs; ext grows mounted and shrinks only unmounted; XFS and ZFS only grow; the others resize only
     * unmounted.
     */
    public boolean resizes(final boolean grow, final boolean mounted) {
        return switch (this.resize) {
            case MOUNTED -> true;
            case GROWS_MOUNTED -> grow || !mounted;
            case GROWS_ONLY -> grow;
            case UNMOUNTED -> !mounted;
        };
    }

    /** Whether it keeps snapshots: btrfs, ZFS and APFS. */
    public boolean snapshots() {
        return this == BTRFS || this == ZFS || this == APFS;
    }

    /** Whether two disks may hold one pool of it, each repairing the other: ZFS's mirrors. */
    public boolean mirrors() {
        return this == ZFS;
    }

    /** Whether it keeps a journal, which brings it back in seconds after a power loss. */
    public boolean journaled() {
        return this.recovery == Recovery.JOURNAL;
    }

    /** Whether it holds files and items at all: everything but swap. */
    public boolean holdsData() {
        return this != SWAP;
    }

    /** The largest partition it is made on, in megabytes, or {@link Long#MAX_VALUE} when it has no such limit. */
    public long mostPartitionMb() {
        return switch (this) {
            case FAT12 -> FAT12_MOST_MB;
            case FAT16 -> FAT16_MOST_MB;
            default -> Long.MAX_VALUE;
        };
    }

    /** The largest file it keeps, in bytes, or {@link Long#MAX_VALUE} when it has no such limit. */
    public long mostFileBytes() {
        return switch (this) {
            case FAT12, FAT16, FAT32 -> FAT_MOST_FILE_BYTES;
            default -> Long.MAX_VALUE;
        };
    }

    /** How it gets back on its feet after a power loss. */
    public enum Recovery {

        /** No journal: every allocation is read again, longer the more is in use. */
        LONG_CHECK,
        /** A journal: what was half written is replayed in a couple of seconds. */
        JOURNAL,
        /** Copy-on-write: nothing was ever half written over, so there is nothing to check. */
        COPY_ON_WRITE,
        /** Swap: it keeps nothing across a start, so there is nothing to check either. */
        NOTHING_KEPT;

        /* A long check reads about this many items' weight a second, for no less and no longer than these. */
        private static final long ITEMS_A_SECOND = 64L;
        private static final int SHORTEST_CHECK_TICKS = 100;
        private static final int LONGEST_CHECK_TICKS = 1_200;
        private static final int JOURNAL_TICKS = 40;
        private static final long TICKS_A_SECOND = 20L;

        /**
         * How long getting back on its feet takes with {@code itemsInUse} items' weight in use, in ticks: a second for
         * every 64, from 5 seconds to a minute, without a journal; 2 seconds with one; none on copy-on-write or swap.
         * Estimates.
         */
        public int ticksFor(final long itemsInUse) {
            return switch (this) {
                case LONG_CHECK -> (int) Math.max(SHORTEST_CHECK_TICKS,
                        Math.min(LONGEST_CHECK_TICKS, Math.max(0L, itemsInUse) * TICKS_A_SECOND / ITEMS_A_SECOND));
                case JOURNAL -> JOURNAL_TICKS;
                case COPY_ON_WRITE, NOTHING_KEPT -> 0;
            };
        }
    }

    /* Which systems read a file system. */
    private enum Readers {
        EVERY_SYSTEM, FRAMES_THEN_LINUX_FROM_TRANSITION, FRAMES_THEN_LINUX_FROM_ADVANCED, LINUX, UNIXES, FREEBSD,
        QUINCE, NO_SYSTEM
    }

    /* How a partition of a file system may change size. */
    private enum Resize {
        /* Grows and shrinks while the system runs. */
        MOUNTED,
        /* Grows while mounted, shrinks only unmounted. */
        GROWS_MOUNTED,
        /* Only grows. */
        GROWS_ONLY,
        /* Changes size only unmounted. */
        UNMOUNTED
    }
}
