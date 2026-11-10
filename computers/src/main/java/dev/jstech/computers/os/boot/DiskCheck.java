/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The check a system makes of its disks as it comes up after a sudden shutdown, at the head of its coming-up: ScanDisk
 * on Frames 95, chkdsk from Frames XP on, fsck on Linux, the BSDs and UNIX; MC-DOS and MC-NET check nothing, as the
 * real ones did not. How long it takes is its file system's ({@link FileSystemType.Recovery}); it erases nothing.
 *
 * <p>Pure: no game types. Worked out on the server from the machine's disks and carried to whoever watches.
 *
 * @param look       how the system shows it; {@link Look#NONE} when nothing is checked
 * @param type       the file system checked
 * @param usedMb     what the checked disks hold, in megabytes
 * @param capacityMb what they could hold, in megabytes
 * @param kinds      how many kinds of things they hold, which the check counts among its files
 * @param ticks      how long it takes
 */
public record DiskCheck(Look look, FileSystemType type, long usedMb, long capacityMb, int kinds, int ticks) {

    /** No check: a clean shutdown, a system that checks nothing, or a file system with nothing to mend. */
    public static final DiskCheck NONE = new DiskCheck(Look.NONE, FileSystemType.FAT16, 0L, 0L, 0, 0);

    /* The blocks each check counts in: ext's and UFS's fragments of 4 KiB, System V's of 512 bytes. */
    private static final long EXT_BLOCKS_A_MB = 256L;
    private static final long SYSTEM_V_BLOCKS_A_MB = 2_048L;
    private static final int UFS_FRAGS_A_BLOCK = 8;
    /* The inodes a fresh file system already holds for itself, and one inode for every 16 KiB. */
    private static final int EXT_RESERVED_INODES = 11;
    private static final int EXT_BLOCKS_AN_INODE = 4;
    private static final int UFS_SYSTEM_FILES = 3;
    private static final int SYSTEM_V_SYSTEM_FILES = 30;
    private static final int NTFS_SYSTEM_FILES = 16;
    /* The share of the check after which a long check prints what it found. */
    private static final double SUMMARY_FROM = 0.9;
    /* The label OpenBSD's disks carry, which its lines name a partition by beside the device. */
    public static final String OPENBSD_DUID = "3c1d5f2a8e9b7c41";

    public DiskCheck {
        look = look == null ? Look.NONE : look;
        type = type == null ? FileSystemType.FAT16 : type;
        usedMb = Math.max(0L, usedMb);
        capacityMb = Math.max(usedMb, capacityMb);
        kinds = Math.max(0, kinds);
        ticks = Math.max(0, ticks);
    }

    /** Whether there is a check to show: something was left unclean and the system checks it. */
    public boolean isEmpty() {
        return this.look == Look.NONE || this.ticks <= 0;
    }

    /** How far through the check {@code ticksDone} is, from 0 to 1. */
    public double through(final int ticksDone) {
        return this.ticks <= 0 ? 1.0 : Math.min(1.0, Math.max(0, ticksDone) / (double) this.ticks);
    }

    /**
     * What a Unix's console shows {@code ticksDone} into the check, in each kernel's own words, which no kernel ever
     * printed in anything but its own: the journal replayed on ext, the five passes of UFS and of System V's fsck.
     */
    public List<String> consoleLines(final int ticksDone) {
        final double done = through(ticksDone);
        return switch (this.look) {
            case LINUX -> shown(linux(), done, 0);
            case FREEBSD -> shown(freeBsd(done), done, 5);
            case OPENBSD -> shown(openBsd(done, "wd0a"), done, 5);
            case OPENBSD_SD -> shown(openBsd(done, "sd0a"), done, 5);
            case UNIX -> shown(systemV(done), done, 5);
            default -> List.of();
        };
    }

    /** The megabytes free on the checked disks. */
    public long freeMb() {
        return this.capacityMb - this.usedMb;
    }

    /**
     * How many files the check counts: one for each kind of thing held, besides those its file system keeps for
     * itself (NTFS's metadata files, ext's reserved inodes, UFS's and System V's own).
     */
    public int files() {
        return this.kinds + switch (this.look) {
            case LINUX -> EXT_RESERVED_INODES;
            case FREEBSD, OPENBSD, OPENBSD_SD -> UFS_SYSTEM_FILES;
            case UNIX -> SYSTEM_V_SYSTEM_FILES;
            default -> NTFS_SYSTEM_FILES;
        };
    }

    /* The journal replayed: the kernel finds it, systemd checks the root, and the root comes back clean. */
    private List<String> linux() {
        final String fs = this.type.displayName().toUpperCase(Locale.ROOT);
        final long blocks = this.capacityMb * EXT_BLOCKS_A_MB;
        final long inodes = Math.max(files(), blocks / EXT_BLOCKS_AN_INODE);
        return List.of("[    1.802201] " + fs + "-fs (sda1): INFO: recovery required on readonly filesystem",
                "Starting File System Check on /dev/sda1...", "/dev/sda1: recovering journal",
                "/dev/sda1: clean, " + files() + "/" + inodes + " files, "
                        + this.usedMb * EXT_BLOCKS_A_MB + "/" + blocks + " blocks",
                "[  OK  ] Finished File System Check on /dev/sda1.");
    }

    /* UFS read from end to end: the five phases, and what it found at the end of them. */
    private List<String> freeBsd(final double done) {
        final List<String> out = new ArrayList<>(List.of("WARNING: / was not properly dismounted",
                "Starting file system checks:", "** /dev/ada0p2", "** Last Mounted on /", "** Root file system",
                "** Phase 1 - Check Blocks and Sizes", "** Phase 2 - Check Pathnames",
                "** Phase 3 - Check Connectivity", "** Phase 4 - Check Reference Counts",
                "** Phase 5 - Check Cyl groups"));
        if (done >= SUMMARY_FROM) {
            final long free = freeMb() * EXT_BLOCKS_A_MB;
            out.add(files() + " files, " + this.usedMb * EXT_BLOCKS_A_MB + " used, " + free
                    + " free (" + free % UFS_FRAGS_A_BLOCK + " frags, " + free / UFS_FRAGS_A_BLOCK
                    + " blocks, 0.0% fragmentation)");
            out.add("");
            out.add("***** FILE SYSTEM MARKED CLEAN *****");
        }
        return out;
    }

    /*
     * OpenBSD's fsck of its root as rc runs it: each line opened with the partition and its label, the five phases of
     * UFS, what it found and the root marked clean.
     */
    private List<String> openBsd(final double done, final String root) {
        final String opened = "/dev/" + root + " (" + OPENBSD_DUID + ".a): ";
        final List<String> out = new ArrayList<>(List.of("Automatic boot in progress: starting file system checks.",
                opened + "** Phase 1 - Check Blocks and Sizes", opened + "** Phase 2 - Check Pathnames",
                opened + "** Phase 3 - Check Connectivity", opened + "** Phase 4 - Check Reference Counts",
                opened + "** Phase 5 - Check Cyl groups"));
        if (done >= SUMMARY_FROM) {
            final long free = freeMb() * EXT_BLOCKS_A_MB;
            out.add(opened + files() + " files, " + this.usedMb * EXT_BLOCKS_A_MB + " used, " + free + " free ("
                    + free % UFS_FRAGS_A_BLOCK + " frags, " + free / UFS_FRAGS_A_BLOCK
                    + " blocks, 0.0% fragmentation)");
            out.add(opened + "MARKED CLEAN");
        }
        return out;
    }

    /* System V's fsck of its root: the five phases, and what it found at the end of them. */
    private List<String> systemV(final double done) {
        final List<String> out = new ArrayList<>(List.of("The / file system (/dev/root) is being checked.", "",
                "/dev/root", "File System: root Volume: 1", "", "** Phase 1 - Check Blocks and Sizes",
                "** Phase 2 - Check Pathnames", "** Phase 3 - Check Connectivity",
                "** Phase 4 - Check Reference Counts", "** Phase 5 - Check Free List"));
        if (done >= SUMMARY_FROM) {
            out.add(files() + " files " + this.usedMb * SYSTEM_V_BLOCKS_A_MB
                    + " blocks " + freeMb() * SYSTEM_V_BLOCKS_A_MB + " free");
            out.add("");
            out.add("*** FILE SYSTEM WAS MODIFIED ***");
        }
        return out;
    }

    /*
     * The lines reached by {@code done}: the first {@code heading} at once, the rest one after another over the check,
     * and whatever was added at its end with them.
     */
    private static List<String> shown(final List<String> lines, final double done, final int heading) {
        final int stepped = lines.size() - heading;
        final int reached = heading + (int) Math.ceil(done * stepped);
        return List.copyOf(lines.subList(0, Math.max(1, Math.min(lines.size(), reached))));
    }

    /** How a system shows its check. */
    public enum Look implements IStableName {

        /** Nothing: the system checks nothing, or there is nothing to check. */
        NONE("none"),
        /** Frames 95's ScanDisk: the blue text screen, its list of areas and its bar. */
        SCANDISK("scandisk"),
        /** Frames XP's chkdsk, white on blue as its autochk ran. */
        CHKDSK_XP("chkdsk_xp"),
        /** Frames 7's chkdsk, white on black. */
        CHKDSK_7("chkdsk_7"),
        /** Frames 10's line under its mark, as it scans and repairs. */
        FRAMES_10("frames_10"),
        /** Frames 11's line under its mark. */
        FRAMES_11("frames_11"),
        /** fsck on the Linux console. */
        LINUX("linux"),
        /** fsck on FreeBSD's console. */
        FREEBSD("freebsd"),
        /** fsck on OpenBSD's console, its root on an IDE disk ({@code wd0}). */
        OPENBSD("openbsd"),
        /** The same, its root on a SATA disk ({@code sd0}). */
        OPENBSD_SD("openbsd_sd"),
        /** fsck on UNIX System V's console. */
        UNIX("unix");

        private final String serializedName;

        private static final StableNames<Look> NAMES = StableNames.of(Look.class);

        Look(final String serializedName) {
            this.serializedName = serializedName;
        }

        /** How a system of {@code platform} at {@code familyRank} in its family shows its check. */
        public static Look of(final Platform platform, final int familyRank) {
            return switch (platform) {
                case FRAMES -> switch (familyRank) {
                    case 1 -> SCANDISK;
                    case 2 -> CHKDSK_XP;
                    case 3 -> CHKDSK_7;
                    case 4 -> FRAMES_10;
                    default -> FRAMES_11;
                };
                case LINUX -> LINUX;
                case FREEBSD -> FREEBSD;
                case OPENBSD -> OPENBSD;
                case UNIX -> UNIX;
                // Quince's start shows its own picture and no check of its disk.
                case MC_DOS, MC_NET, QUINCE -> NONE;
            };
        }

        /** The look called {@code name}; an unknown one shows nothing. */
        public static Look byName(final String name) {
            return NAMES.byName(name, NONE);
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
