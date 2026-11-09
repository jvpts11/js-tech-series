/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;

/**
 * How a system shows its Defrag, the program every computer comes with (computers/docs/design/storage.md): DEFRAG's
 * map of clusters on MC-DOS, the Disk Defragmenter of each Frames up to 7, Optimize Drives on 10 and 11, fsck on
 * the Unix systems, which is what repairs a disk there, and Disk Utility's First Aid on Quince's.
 *
 * <p>Pure: no game types.
 */
public enum DefragLook implements IStableName {

    /** MC-DOS's DEFRAG, the blue screen with the map of the disk's clusters; MC-NET runs the same. */
    DOS("dos"),
    /** Frames 95's Disk Defragmenter: the grid of coloured blocks. */
    FRAMES_95("frames_95"),
    /** Frames XP's Disk Defragmenter: the volumes and the two bars, before and after. */
    FRAMES_XP("frames_xp"),
    /** Frames 7's Disk Defragmenter: the schedule and the state of each disk. */
    FRAMES_7("frames_7"),
    /** Frames 10's Optimize Drives. */
    FRAMES_10("frames_10"),
    /** Frames 11's Optimize Drives. */
    FRAMES_11("frames_11"),
    /** fsck on Linux. */
    LINUX("linux"),
    /** fsck on FreeBSD. */
    FREEBSD("freebsd"),
    /** fsck on UNIX System V. */
    UNIX("unix"),
    /** Disk Utility's First Aid on Quince's systems, which prints what fsck_hfs says (computers/docs/design/quince.md). */
    QUINCE("quince");

    private static final StableNames<DefragLook> NAMES = StableNames.of(DefragLook.class);

    private final String serializedName;

    DefragLook(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** How a system of {@code platform} at {@code familyRank} in its family shows its Defrag. */
    public static DefragLook of(final Platform platform, final int familyRank) {
        return switch (platform) {
            case MC_DOS, MC_NET -> DOS;
            case FRAMES -> switch (familyRank) {
                case 1 -> FRAMES_95;
                case 2 -> FRAMES_XP;
                case 3 -> FRAMES_7;
                case 4 -> FRAMES_10;
                default -> FRAMES_11;
            };
            case LINUX -> LINUX;
            case FREEBSD -> FREEBSD;
            case UNIX -> UNIX;
            case QUINCE -> QUINCE;
        };
    }

    /** The look called {@code name}; an unknown one is DEFRAG's. */
    public static DefragLook byName(final String name) {
        return NAMES.byName(name, DOS);
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Whether it is a window on a Frames desktop. */
    public boolean window() {
        return this == FRAMES_95 || this == FRAMES_XP || this == FRAMES_7 || this == FRAMES_10 || this == FRAMES_11;
    }

    /** Whether it is fsck at a Unix prompt. */
    public boolean fsck() {
        return this == LINUX || this == FREEBSD || this == UNIX;
    }

    /** Whether it runs on a schedule of its own, as Frames 7, 10 and 11 do once a week. */
    public boolean scheduled() {
        return this == FRAMES_7 || this == FRAMES_10 || this == FRAMES_11;
    }

    /**
     * What the system calls its {@code index}-th disk, the system disk first: a drive letter on MC-DOS and Frames, the
     * first partition's device on the Unix systems ({@code sdb1}, {@code ada1p1}, {@code 1s1}).
     */
    public String drive(final int index) {
        return switch (this) {
            case LINUX -> "sd" + (char) ('a' + index) + "1";
            case FREEBSD -> "ada" + index + "p1";
            case UNIX -> index + "s1";
            case QUINCE -> "disk" + index + "s2";
            default -> (char) ('C' + index) + ":";
        };
    }

    /** The device a Unix's fsck is pointed at for that disk, as its own tree names it. */
    public String device(final int index) {
        return switch (this) {
            case UNIX -> "/dev/dsk/" + drive(index);
            default -> "/dev/" + drive(index);
        };
    }

    /**
     * Which disk a name stands for, the way the system would read it ({@code C:}, {@code d}, {@code /dev/sdb1},
     * {@code ada1p1}, {@code /dev/dsk/1s1}); -1 when it names none of the first {@code disks}.
     */
    public int diskNamed(final String name, final int disks) {
        final String wanted = name == null ? "" : name.trim();
        for (int i = 0; i < disks; i++) {
            final String drive = drive(i);
            if (wanted.equalsIgnoreCase(drive) || wanted.equalsIgnoreCase(device(i))
                    || !fsck() && wanted.length() == 1 && Character.toUpperCase(wanted.charAt(0)) == drive.charAt(0)) {
                return i;
            }
        }
        return -1;
    }
}
