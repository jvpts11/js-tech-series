/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class FileSystemTypeTest {

    @Test
    void installedBy_formatsAsEachRealInstallerDid() {
        assertEquals(FileSystemType.FAT16, FileSystemType.installedBy(Platform.MC_DOS, 0, false, "mc_dos",
                HardwareEra.VINTAGE));
        assertEquals(FileSystemType.FAT32, FileSystemType.installedBy(Platform.MC_DOS, 0, false, "mc_dos",
                HardwareEra.LEGACY));
        assertEquals(FileSystemType.FAT32, FileSystemType.installedBy(Platform.FRAMES, 1, false, "frames_95",
                HardwareEra.LEGACY));
        assertEquals(FileSystemType.NTFS, FileSystemType.installedBy(Platform.FRAMES, 2, false, "frames_xp",
                HardwareEra.LEGACY));
        assertEquals(FileSystemType.UFS, FileSystemType.installedBy(Platform.UNIX, 0, false, "unix",
                HardwareEra.VINTAGE));
        assertEquals(FileSystemType.UFS, FileSystemType.installedBy(Platform.FREEBSD, 0, false, "freebsd",
                HardwareEra.STANDARD));
    }

    @Test
    void installedBy_givesEachQuinceSystemTheVolumeFormatOfItsDay() {
        assertEquals(FileSystemType.HFS, FileSystemType.installedBy(Platform.QUINCE, QuinceFamily.QUINCE_SYSTEM,
                false, "quince_system", HardwareEra.VINTAGE));
        assertEquals(FileSystemType.HFS_PLUS, FileSystemType.installedBy(Platform.QUINCE, QuinceFamily.OS_9, false,
                "quince_os_9", HardwareEra.LEGACY));
        assertEquals(FileSystemType.HFS_PLUS_JOURNALED, FileSystemType.installedBy(Platform.QUINCE,
                QuinceFamily.OS_X, false, "quince_os_x", HardwareEra.LEGACY));
        assertEquals(FileSystemType.APFS, FileSystemType.installedBy(Platform.QUINCE, QuinceFamily.QUINCE_OS, false,
                "quinceos", HardwareEra.ADVANCED));
        assertEquals("HFS+ (Journaled)", FileSystemType.HFS_PLUS_JOURNALED.displayName());
    }

    @Test
    void installedBy_givesLinuxItsEraAndFedoraBtrfs() {
        assertEquals(FileSystemType.EXT3, FileSystemType.installedBy(Platform.LINUX, 0, false, "debian",
                HardwareEra.LEGACY));
        assertEquals(FileSystemType.EXT4, FileSystemType.installedBy(Platform.LINUX, 0, false, "debian",
                HardwareEra.TRANSITION));
        assertEquals(FileSystemType.EXT4, FileSystemType.installedBy(Platform.LINUX, 0, false, "fedora",
                HardwareEra.TRANSITION));
        assertEquals(FileSystemType.BTRFS, FileSystemType.installedBy(Platform.LINUX, 0, false, "fedora",
                HardwareEra.STANDARD));
        assertEquals(FileSystemType.EXT4, FileSystemType.installedBy(Platform.LINUX, 0, true, "gentoo",
                HardwareEra.LEGACY), "the root the player formats by hand is ext4");
    }

    @Test
    void ticksFor_longCheckReadsASecondForEverySixtyFourItemsFromFiveSecondsToAMinute() {
        assertEquals(100, FileSystemType.Recovery.LONG_CHECK.ticksFor(0L));
        assertEquals(100, FileSystemType.Recovery.LONG_CHECK.ticksFor(64L * 5));
        assertEquals(200, FileSystemType.Recovery.LONG_CHECK.ticksFor(64L * 10));
        assertEquals(1_200, FileSystemType.Recovery.LONG_CHECK.ticksFor(64L * 600));
        assertEquals(1_200, FileSystemType.Recovery.LONG_CHECK.ticksFor(Long.MAX_VALUE / 100));
    }

    @Test
    void ticksFor_aJournalTakesTwoSecondsAndCopyOnWriteNone() {
        assertEquals(40, FileSystemType.Recovery.JOURNAL.ticksFor(1_000_000L));
        assertEquals(0, FileSystemType.Recovery.COPY_ON_WRITE.ticksFor(1_000_000L));
    }

    @Test
    void byName_readsEachBackAndAnUnknownAsTheOldest() {
        for (final FileSystemType type : FileSystemType.values()) {
            assertEquals(type, FileSystemType.byName(type.serializedName()));
        }
        assertEquals(FileSystemType.FAT16, FileSystemType.byName("reiserfs"));
    }
}
