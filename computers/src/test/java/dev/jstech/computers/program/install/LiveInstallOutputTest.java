/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tools print what the real ones print.
 *
 * <p>Held against a real run rather than against itself: the figures below are the ones a real
 * {@code mke2fs 1.47.1} put on the screen for a filesystem of this size, so a change that makes the arithmetic
 * look plausible but wrong fails here rather than in front of somebody who has installed Gentoo before.
 */
class LiveInstallOutputTest {

    /**
     * The disk of the real run these figures come from: 24,747,008 blocks of four kilobytes.
     *
     * <p>Given in megabytes because that is what a disk in this game is measured in, and it is the size that
     * produces exactly that block count.
     */
    private static final long REAL_RUN_MB = 24_747_008L * 4096L / (1024L * 1024L);

    @Test
    void mke2fs_namesItselfTheWayTheRealOneDoes() {
        final List<String> out = LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 1);
        assertEquals("mke2fs 1.47.1 (20-May-2024)", out.get(0));
    }

    @Test
    void mke2fs_countsTheBlocksAndInodesOfTheRealRun() {
        final List<String> out = LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 1);
        assertEquals("Creating filesystem with 24747008 4k blocks and 6193152 inodes", out.get(1));
    }

    @Test
    void mke2fs_putsItsSuperblockBackupsWhereTheRealOnePutThem() {
        final String out = String.join("\n", LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 1));
        assertTrue(out.contains("Superblock backups stored on blocks:"), out);
        /*
         * The first nine of the real run's list, which are group 1 and the groups whose number is a power of
         * three, five or seven: the rule, seen from the outside.
         */
        assertTrue(out.contains("32768, 98304, 163840, 229376, 294912, 819200, 884736, 1605632, 2654208"), out);
        // And the last of them, which is where the list stops because the next group is off the end of the disk.
        assertTrue(out.contains("23887872"), out);
    }

    @Test
    void mke2fs_sizesItsJournalTheWayTheRealRunDid() {
        final String out = String.join("\n", LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 1));
        assertTrue(out.contains("Creating journal (131072 blocks): done"), out);
    }

    @Test
    void mke2fs_endsOnTheFourLinesTheRealOneEndsOn() {
        final String out = String.join("\n", LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 1));
        assertTrue(out.contains("Allocating group tables: done"), out);
        assertTrue(out.contains("Writing inode tables: done"), out);
        assertTrue(out.contains("Writing superblocks and filesystem accounting information: done"), out);
    }

    /**
     * A filesystem's identifier is shaped like one, and the same disk is always given the same one.
     *
     * <p>A real one is random. Random is the one thing this cannot be: a world reloaded would come back with
     * a different filesystem than the one that was made.
     */
    @Test
    void mke2fs_givesTheSameDiskTheSameIdentifierEveryTime() {
        final String first = uuidOf(LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 7));
        final String again = uuidOf(LiveInstallOutput.mke2fs("sda3", REAL_RUN_MB, 7));
        final String other = uuidOf(LiveInstallOutput.mke2fs("sdb1", REAL_RUN_MB, 7));
        assertEquals(first, again);
        assertTrue(!first.equals(other), "a different device is given a different one");
        assertTrue(first.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), first);
    }

    /** A filesystem too small to be worth journalling is made without one, as the real tool makes it. */
    @Test
    void mke2fs_aTinyFilesystem_isMadeWithoutAJournal() {
        final String out = String.join("\n", LiveInstallOutput.mke2fs("sda", 4, 1));
        assertTrue(!out.contains("Creating journal"), out);
    }

    @Test
    void mkfsFat_saysItsNameAndVersionAndNothingElse() {
        assertEquals(List.of("mkfs.fat 4.2 (2021-01-31)"), LiveInstallOutput.mkfsFat());
    }

    @Test
    void fdisk_opensWithTheWarningTheRealOneOpensWith() {
        final List<String> out = LiveInstallOutput.fdisk("sda", "500 GB");
        assertEquals("Welcome to fdisk (util-linux 2.40.2).", out.get(0));
        assertEquals("Changes will remain in memory only, until you decide to write them.", out.get(1));
        assertEquals("Be careful before using the write command.", out.get(2));
        assertTrue(String.join("\n", out).contains("Disk /dev/sda: 500 GB"));
        assertEquals("Command (m for help):", out.get(out.size() - 1));
    }

    /**
     * A FAT filesystem is named by a serial number, not by an identifier, because that is all it holds.
     *
     * <p>A table that names it the way it names the root filesystem is a table somebody wrote from memory.
     */
    @Test
    void shortUuid_isTheFourByteSerialThatFilesystemReallyKeeps() {
        final String serial = LiveInstallOutput.shortUuid("sda1", 512);
        assertTrue(serial.matches("[0-9A-F]{4}-[0-9A-F]{4}"), serial);
        assertEquals(serial, LiveInstallOutput.shortUuid("sda1", 512));
        assertTrue(!serial.equals(LiveInstallOutput.shortUuid("sda2", 512)));
    }

    @Test
    void lsblk_hasTheSevenColumnsTheRealListingHas() {
        final String header = LiveInstallOutput.lsblkHeader();
        assertTrue(header.startsWith("NAME"), header);
        for (final String column : new String[]{"MAJ:MIN", "RM", "SIZE", "RO", "TYPE", "MOUNTPOINTS"}) {
            assertTrue(header.contains(column), column + " is missing from: " + header);
        }
    }

    @Test
    void lsblk_linesUpItsColumnsWhateverTheNameIsCalled() {
        final String disk = LiveInstallOutput.lsblkRow("sda", 8, 0, 102_400, "disk", "");
        final String part = LiveInstallOutput.lsblkRow("`-sda1", 8, 1, 512, "part", "/mnt/boot");
        assertTrue(disk.startsWith("sda       8:0"), disk);
        assertTrue(disk.contains("100G"), disk);
        assertTrue(part.startsWith("`-sda1    8:1"), part);
        assertTrue(part.endsWith("/mnt/boot"), part);
        // The size column is right-aligned, so the figures stack whatever their width.
        assertEquals(disk.indexOf("100G") + 4, part.indexOf("512M") + 4, disk + "\n" + part);
    }

    @Test
    void lsblkSize_picksTheLargestUnitThatStillSaysSomething() {
        assertEquals("512M", LiveInstallOutput.lsblkSize(512));
        assertEquals("1023M", LiveInstallOutput.lsblkSize(1023));
        assertEquals("1G", LiveInstallOutput.lsblkSize(1024));
        assertEquals("99.5G", LiveInstallOutput.lsblkSize(101_888));
        assertEquals("2T", LiveInstallOutput.lsblkSize(2 * 1024 * 1024));
    }

    /**
     * The configuration generator names every image it found, and says when it was not allowed to look.
     *
     * <p>Both halves matter to somebody whose new system is not on the menu: the first tells them whether
     * the kernel was installed where it is looked for, and the second tells them why the system they
     * already had is missing from it.
     */
    @Test
    void grubMkconfig_namesWhatItFoundAndWarnsAboutWhatItDidNotLookFor() {
        final List<String> out = LiveInstallOutput.grubMkconfig(List.of("/boot/vmlinuz-linux"), false);
        final String all = String.join("\n", out);
        assertEquals("Generating grub configuration file ...", out.get(0));
        assertTrue(all.contains("Found linux image: /boot/vmlinuz-linux"), all);
        assertTrue(all.contains("Found initrd image: /boot/initramfs-linux.img"), all);
        assertTrue(all.contains("Warning: os-prober will not be executed"), all);
        assertEquals("done", out.get(out.size() - 1));
    }

    @Test
    void grubMkconfig_whenItIsAllowedToLook_saysNothingAboutNotLooking() {
        final String all = String.join("\n",
                LiveInstallOutput.grubMkconfig(List.of("/boot/vmlinuz-linux"), true));
        assertTrue(!all.contains("os-prober"), all);
    }

    private static String uuidOf(final List<String> out) {
        for (final String line : out) {
            if (line.startsWith("Filesystem UUID: ")) {
                return line.substring("Filesystem UUID: ".length());
            }
        }
        return "";
    }
}
