/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The figures a filesystem is made with, worked out from the size of the disk by the rules the real tool uses.
 *
 * <p>Worked out rather than written down, so the numbers on the glass are the numbers of the disk in the
 * machine: its block count, its inode count with the rounding that makes the real figure look the way it
 * does, where its spare superblocks land, how big its journal is. Somebody who has made a filesystem before
 * reads those without thinking, and would notice the one that was wrong.
 *
 * @param blocks  how many four-kilobyte blocks the filesystem has
 * @param groups  how many block groups those come to
 * @param inodes  how many inodes it is made with
 * @param journal how many blocks its journal takes, or none on a filesystem too small to be worth one
 * @param backups the blocks the spare superblocks are stored on, in order
 */
public record Ext4Figures(long blocks, long groups, long inodes, long journal, List<Long> backups) {

    private static final int BLOCK_BYTES = 4096;
    private static final int BLOCKS_PER_GROUP = 32768;

    /** One inode for every this many bytes, which is what decides how many a filesystem gets. */
    private static final int BYTES_PER_INODE = 16384;

    /** The figures for a filesystem over a device of that many megabytes. */
    public static Ext4Figures of(final long sizeMb) {
        final long blocks = Math.max(1L, sizeMb * (1024L * 1024L) / BLOCK_BYTES);
        final long groups = Math.max(1L, (blocks + BLOCKS_PER_GROUP - 1) / BLOCKS_PER_GROUP);
        return new Ext4Figures(blocks, groups, inodes(blocks, groups), journalBlocks(blocks), backups(blocks));
    }

    /**
     * The identifier a filesystem is given, worked out from the disk it is on rather than at random.
     *
     * <p>A real one is random, and random is the one thing this cannot be: the same machine formatting the
     * same disk twice has to read the same, or a tool played again after a load would print a different
     * filesystem from the one it printed before.
     */
    public static String uuid(final String device, final long seed) {
        final StringBuilder out = new StringBuilder(36);
        long state = state(device, seed);
        for (int i = 0; i < 32; i++) {
            if (i == 8 || i == 12 || i == 16 || i == 20) {
                out.append('-');
            }
            state = next(state);
            out.append(Character.forDigit((int) (state >>> 59) & 0xF, 16));
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    /** The four-byte serial a FAT filesystem has instead, in upper case with a dash down the middle. */
    public static String serial(final String device, final long seed) {
        final StringBuilder out = new StringBuilder(9);
        long state = state(device, seed);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                out.append('-');
            }
            state = next(state);
            out.append(Character.forDigit((int) (state >>> 59) & 0xF, 16));
        }
        return out.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * One for every sixteen kilobytes, shared out over the groups and rounded up to the power of two the real
     * tool rounds a group's share to, which is why the printed figure is never the plain division.
     */
    private static long inodes(final long blocks, final long groups) {
        final long wanted = Math.max(1L, blocks * BLOCK_BYTES / BYTES_PER_INODE);
        final long perGroup = (wanted + groups - 1) / groups;
        long rounded = 1L;
        while (rounded < perGroup) {
            rounded <<= 1;
        }
        return rounded * groups;
    }

    /** Grows in steps with the filesystem, as the real tool's does; none at all on a very small one. */
    private static long journalBlocks(final long blocks) {
        if (blocks < 2048) {
            return 0;
        }
        if (blocks < 32_768) {
            return 1024;
        }
        if (blocks < 256L * 1024) {
            return 4096;
        }
        if (blocks < 512L * 1024) {
            return 8192;
        }
        if (blocks < 1024L * 1024) {
            return 16_384;
        }
        if (blocks < 4L * 1024 * 1024) {
            return 32_768;
        }
        if (blocks < 16L * 1024 * 1024) {
            return 65_536;
        }
        return 131_072;
    }

    /** Group one, and every group whose number is a power of three, five or seven, while they fit. */
    private static List<Long> backups(final long blocks) {
        final List<Long> groups = new ArrayList<>();
        groups.add(1L);
        for (final long base : new long[]{3, 5, 7}) {
            for (long power = base; power * BLOCKS_PER_GROUP < blocks; power *= base) {
                groups.add(power);
            }
        }
        final List<Long> out = new ArrayList<>();
        for (final long group : groups.stream().distinct().sorted().toList()) {
            if (group * BLOCKS_PER_GROUP < blocks) {
                out.add(group * BLOCKS_PER_GROUP);
            }
        }
        return List.copyOf(out);
    }

    private static long state(final String device, final long seed) {
        return seed * 1_099_511_628_211L + device.hashCode();
    }

    private static long next(final long state) {
        return state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
    }
}
