/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the tools of a by-hand installation really print.
 *
 * <p>The whole point of installing these two distributions by hand is that it is the real thing: the same
 * commands, in the same order, answering the same way. A tool that prints three summarised lines where the
 * real one prints a screenful is a tool somebody has described rather than one they have used, and the
 * difference is the difference between an installation and a cutscene about one.
 *
 * <p>So the text lives here, apart from the sequence that decides when a step is allowed. This class knows
 * nothing about order or state: it is asked what {@code mke2fs} says about a disk of that size and it answers,
 * the way the real one would. Every figure it prints is worked out from the disk in the machine, by the rules
 * the real tool uses, rather than written down: a filesystem's block count, its inode count, where its
 * superblock backups land, how big its journal is.
 *
 * <p>Pure text and arithmetic, no Minecraft, so every line of it can be held to the real thing in a test.
 */
public final class LiveInstallOutput {

    /** The version of each tool these media carry, printed the way each one prints it. */
    private static final String MKE2FS_VERSION = "mke2fs 1.47.1 (20-May-2024)";
    private static final String MKFS_FAT_VERSION = "mkfs.fat 4.2 (2021-01-31)";
    private static final String FDISK_VERSION = "fdisk (util-linux 2.40.2)";

    /** The block size a filesystem is made with, and how many blocks one group holds. */
    private static final int BLOCK_BYTES = 4096;
    private static final int BLOCKS_PER_GROUP = 32768;

    /** One inode for every this many bytes, which is what decides how many a filesystem gets. */
    private static final int BYTES_PER_INODE = 16384;

    /**
     * Where the backup superblocks land: the group numbers that are a power of 3, 5 or 7.
     *
     * <p>This is the real rule, not a list copied off one disk. A filesystem made with sparse superblocks
     * keeps a spare copy in group 1 and in every group whose number is a power of three, five or seven, and
     * that is why the numbers a real {@code mke2fs} prints look arbitrary until you know what they are.
     */
    private static final int[] BACKUP_GROUPS = backupGroups();

    private LiveInstallOutput() {
    }

    /**
     * What {@code mke2fs} prints while it makes a filesystem on a device of that size.
     *
     * @param device the device it is making it on, without {@code /dev/}
     * @param sizeMb how big that device is
     * @param seed   what the filesystem's identifier is worked out from, so the same disk in the same machine
     *               is always given the same one and a second disk is given another
     */
    public static List<String> mke2fs(final String device, final long sizeMb, final long seed) {
        final long blocks = Math.max(1L, sizeMb * (1024L * 1024L) / BLOCK_BYTES);
        final long groups = Math.max(1L, (blocks + BLOCKS_PER_GROUP - 1) / BLOCKS_PER_GROUP);
        final long inodes = inodes(blocks, groups);
        final List<String> out = new ArrayList<>();
        out.add(MKE2FS_VERSION);
        out.add("Creating filesystem with " + blocks + " 4k blocks and " + inodes + " inodes");
        out.add("Filesystem UUID: " + uuid(device, seed));
        final List<String> backups = backups(blocks);
        if (!backups.isEmpty()) {
            out.add("Superblock backups stored on blocks: ");
            out.addAll(backups);
        }
        out.add("");
        out.add("Allocating group tables: done                            ");
        out.add("Writing inode tables: done                            ");
        /* A filesystem too small to be worth journalling is made without one, and says nothing about it. */
        final long journal = journalBlocks(blocks);
        if (journal > 0) {
            out.add("Creating journal (" + journal + " blocks): done");
        }
        out.add("Writing superblocks and filesystem accounting information: done");
        out.add("");
        return List.copyOf(out);
    }

    /** What {@code mkfs.fat} prints, which is its name and its version and nothing else. */
    public static List<String> mkfsFat() {
        return List.of(MKFS_FAT_VERSION);
    }

    /** What {@code fdisk} says when it opens on a disk. */
    public static List<String> fdisk(final String device, final String size) {
        return List.of(
                "Welcome to " + FDISK_VERSION + ".",
                "Changes will remain in memory only, until you decide to write them.",
                "Be careful before using the write command.",
                "",
                "Disk /dev/" + device + ": " + size,
                "Units: sectors of 1 * 512 = 512 bytes",
                "Sector size (logical/physical): 512 bytes / 512 bytes",
                "I/O size (minimum/optimal): 512 bytes / 512 bytes",
                "",
                "Command (m for help):");
    }

    /**
     * The columns a listing of the machine's block devices has.
     *
     * <p>All seven of them, in the order the real tool puts them in, because the shape of that header is how
     * anybody reading a terminal knows at a glance which tool they are looking at the output of.
     */
    public static String lsblkHeader() {
        return String.format(Locale.ROOT, "%-9s %-7s %2s %6s %2s %-4s %s",
                "NAME", "MAJ:MIN", "RM", "SIZE", "RO", "TYPE", "MOUNTPOINTS");
    }

    /**
     * One device's line in that listing.
     *
     * @param name       what it is called, already carrying the branch drawing if it is a partition
     * @param major      the driver number, which is the same for every disk of a kind
     * @param minor      the device's own number under that driver
     * @param sizeMb     how big it is
     * @param type       what it is: a disk, a partition, a read-only medium
     * @param mountPoint where it is mounted, or empty when it is not
     */
    public static String lsblkRow(final String name, final int major, final int minor, final long sizeMb,
                                  final String type, final String mountPoint) {
        return String.format(Locale.ROOT, "%-9s %-7s %2d %6s %2d %-4s %s",
                name, major + ":" + minor, 0, lsblkSize(sizeMb), 0, type, mountPoint).stripTrailing();
    }

    /**
     * A size the way a device listing writes one: a whole number of the largest unit that fits, and one
     * decimal only when the whole number would lose something worth keeping.
     */
    public static String lsblkSize(final long sizeMb) {
        if (sizeMb >= 1024L * 1024) {
            return trimmed(sizeMb / (1024.0 * 1024.0)) + "T";
        }
        if (sizeMb >= 1024) {
            return trimmed(sizeMb / 1024.0) + "G";
        }
        return sizeMb + "M";
    }

    /**
     * What the bootloader's configuration generator prints while it looks for what it can start.
     *
     * <p>It names every image it finds, which is the only way anybody finds out that the reason their new
     * system is not on the menu is that the kernel was never installed where the generator looks.
     *
     * @param images  the kernel images it found, in the order it found them
     * @param prober  whether it was allowed to go looking for other systems on the machine's other disks
     */
    public static List<String> grubMkconfig(final List<String> images, final boolean prober) {
        final List<String> out = new ArrayList<>();
        out.add("Generating grub configuration file ...");
        for (final String image : images) {
            out.add("Found linux image: " + image);
            out.add("Found initrd image: " + image.replace("vmlinuz", "initramfs") + ".img");
        }
        if (!prober) {
            out.add("Warning: os-prober will not be executed to detect other bootable partitions.");
            out.add("Systems on them will not be added to the GRUB boot configuration.");
            out.add("Check GRUB_DISABLE_OS_PROBER documentation entry.");
        }
        out.add("done");
        return List.copyOf(out);
    }

    /**
     * The identifier a filesystem is given, worked out from the disk it is on rather than at random.
     *
     * <p>A real one is random, and random is the one thing this cannot be: the same machine formatting the
     * same disk twice has to read the same, or a world reloaded would come back with a different filesystem
     * than the one that was made. It is shaped exactly like the real thing, which is what anybody reading it
     * is looking at.
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

    /**
     * The identifier a FAT filesystem is given, which is a different and much shorter thing.
     *
     * <p>That filesystem has no room for a real identifier: it keeps a four-byte serial number, written in
     * upper case with a dash down the middle, and a table that names it the way it names an ext4 filesystem
     * is a table somebody wrote from memory.
     */
    public static String shortUuid(final String device, final long seed) {
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
     * How many inodes a filesystem of that many blocks gets.
     *
     * <p>One for every sixteen kilobytes, shared out over the groups and rounded up to the power of two the
     * real tool rounds a group's share to. It is the rounding that makes the printed figure look the way it
     * does: a disk that works out to 6,186,752 by the plain division is made with 6,193,152.
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

    /**
     * How big the journal is, in blocks, which grows in steps with the filesystem.
     *
     * <p>A filesystem too small to be worth journalling gets none, and the steps above that are the ones the
     * real tool uses, carried on past its old ceiling the way the current one does for the large disks that
     * did not exist when the table was written.
     */
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

    /** The backup superblock list, wrapped the way the real tool wraps it: eight to a line, indented. */
    private static List<String> backups(final long blocks) {
        final List<Long> at = new ArrayList<>();
        for (final int group : BACKUP_GROUPS) {
            final long block = (long) group * BLOCKS_PER_GROUP;
            if (block >= blocks) {
                break;
            }
            at.add(block);
        }
        if (at.isEmpty()) {
            return List.of();
        }
        final List<String> out = new ArrayList<>();
        final StringBuilder row = new StringBuilder("\t");
        for (int i = 0; i < at.size(); i++) {
            row.append(at.get(i));
            if (i < at.size() - 1) {
                row.append(", ");
            }
            if ((i + 1) % 9 == 0 && i < at.size() - 1) {
                out.add(row.toString());
                row.setLength(0);
                row.append("\t");
            }
        }
        if (row.length() > 1) {
            out.add(row.toString());
        }
        return out;
    }

    /** Group one, and every group whose number is a power of three, five or seven, in order. */
    private static int[] backupGroups() {
        final List<Integer> out = new ArrayList<>();
        out.add(1);
        for (final int base : new int[]{3, 5, 7}) {
            for (long power = base; power <= Integer.MAX_VALUE; power *= base) {
                out.add((int) power);
            }
        }
        return out.stream().distinct().sorted().mapToInt(Integer::intValue).toArray();
    }

    /** Where an identifier for that device on that machine starts from, so the same disk reads the same. */
    private static long state(final String device, final long seed) {
        return seed * 1_099_511_628_211L + device.hashCode();
    }

    private static long next(final long state) {
        return state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
    }

    /** A number with its decimal point dropped when there is nothing after it worth printing. */
    private static String trimmed(final double value) {
        final double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.floor(rounded)
                ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }
}
