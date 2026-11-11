/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.FileSystemType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiskLayoutTest {

    private static final long TWO_TB = 2_097_152L;
    private static final long FOUR_TB = 4_194_304L;

    @Test
    void reachMb_masterBootRecordSeesTwoTerabytesOfABiggerDisk() {
        assertEquals(TWO_TB, DiskLayout.empty(PartitionScheme.MBR, FOUR_TB).reachMb());
        assertEquals(FOUR_TB, DiskLayout.empty(PartitionScheme.GPT, FOUR_TB).reachMb());
        assertEquals(20L, DiskLayout.empty(PartitionScheme.MBR, 20L).reachMb());
    }

    @Test
    void whole_isOnePartitionOverAllTheTableReaches() {
        final DiskLayout layout = DiskLayout.whole(PartitionScheme.MBR, FOUR_TB, FileSystemType.NTFS);
        assertEquals(1, layout.partitions().size());
        assertEquals(TWO_TB, layout.find(1).sizeMb());
        assertTrue(layout.find(1).active());
        assertTrue(layout.free().isEmpty());
    }

    @Test
    void create_takesTheLowestFreeNumberAndKeepsTheOrderOnDisk() {
        DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, 20L);
        layout = made(layout.create(0L, 8L, FileSystemType.FAT16, PartitionRole.DATA));
        layout = made(layout.create(8L, 10L, FileSystemType.UFS, PartitionRole.DATA));
        assertEquals(List.of(new DiskLayout.FreeSpace(18L, 2L)), layout.free());
        layout = made(layout.delete(1));
        final DiskLayout.Change again = layout.create(0L, 4L, FileSystemType.FAT16, PartitionRole.DATA);
        assertEquals(1, again.number());
        assertEquals(1, again.layout().partitions().getFirst().number());
    }

    @Test
    void create_refusesAFifthPartitionOnTheMasterBootRecord() {
        DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, 100L);
        for (int i = 0; i < 4; i++) {
            layout = made(layout.create(i * 10L, 10L, FileSystemType.FAT32, PartitionRole.DATA));
        }
        assertTrue(layout.full());
        assertEquals(DiskLayout.Refusal.TABLE_FULL,
                layout.create(40L, 10L, FileSystemType.FAT32, PartitionRole.DATA).refusal());
        DiskLayout gpt = DiskLayout.empty(PartitionScheme.GPT, 100L);
        for (int i = 0; i < 5; i++) {
            gpt = made(gpt.create(i * 10L, 10L, FileSystemType.NTFS, PartitionRole.DATA));
        }
        assertEquals(5, gpt.partitions().size());
    }

    @Test
    void create_refusesMoreThanTheFreeStretchAndFat16OverTwoGigabytes() {
        final DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, 4_096L);
        assertEquals(DiskLayout.Refusal.NO_ROOM,
                layout.create(0L, 5_000L, FileSystemType.FAT32, PartitionRole.DATA).refusal());
        assertEquals(DiskLayout.Refusal.NO_ROOM,
                layout.create(4_000L, 200L, FileSystemType.FAT32, PartitionRole.DATA).refusal());
        assertTrue(layout.create(10L, 100L, FileSystemType.FAT32, PartitionRole.DATA).ok());
        assertEquals(DiskLayout.Refusal.TOO_BIG_FOR_ITS_FILE_SYSTEM,
                layout.create(0L, 3_000L, FileSystemType.FAT16, PartitionRole.DATA).refusal());
        assertTrue(layout.create(0L, 3_000L, null, PartitionRole.DATA).ok());
    }

    @Test
    void resize_growsIntoTheStretchAfterAndNeverBelowWhatItHolds() {
        DiskLayout layout = DiskLayout.empty(PartitionScheme.GPT, 1_000L);
        layout = made(layout.create(0L, 400L, FileSystemType.NTFS, PartitionRole.DATA));
        layout = made(layout.create(500L, 400L, FileSystemType.NTFS, PartitionRole.DATA));
        assertEquals(100L, layout.roomAfter(layout.find(1)));
        assertEquals(500L, made(layout.resize(1, 500L, true, 10L)).find(1).sizeMb());
        assertEquals(DiskLayout.Refusal.NO_ROOM, layout.resize(1, 501L, true, 10L).refusal());
        assertEquals(DiskLayout.Refusal.BELOW_WHAT_IT_HOLDS, layout.resize(1, 100L, true, 200L).refusal());
        assertEquals(200L, made(layout.resize(1, 200L, true, 200L)).find(1).sizeMb());
    }

    @Test
    void resize_followsEachFileSystemsRules() {
        final DiskLayout ext4 = made(DiskLayout.empty(PartitionScheme.GPT, 1_000L)
                .create(0L, 500L, FileSystemType.EXT4, PartitionRole.DATA));
        assertTrue(ext4.resize(1, 600L, true, 10L).ok());
        assertEquals(DiskLayout.Refusal.SHRINKS_ONLY_UNMOUNTED, ext4.resize(1, 400L, true, 10L).refusal());
        assertTrue(ext4.resize(1, 400L, false, 10L).ok());
        final DiskLayout xfs = made(DiskLayout.empty(PartitionScheme.GPT, 1_000L)
                .create(0L, 500L, FileSystemType.XFS, PartitionRole.DATA));
        assertTrue(xfs.resize(1, 600L, true, 10L).ok());
        assertEquals(DiskLayout.Refusal.NEVER_SHRINKS, xfs.resize(1, 400L, false, 10L).refusal());
        final DiskLayout fat = made(DiskLayout.empty(PartitionScheme.MBR, 1_000L)
                .create(0L, 500L, FileSystemType.FAT32, PartitionRole.DATA));
        assertEquals(DiskLayout.Refusal.GROWS_ONLY_UNMOUNTED, fat.resize(1, 600L, true, 10L).refusal());
        assertEquals(DiskLayout.Refusal.SHRINKS_ONLY_UNMOUNTED, fat.resize(1, 400L, true, 10L).refusal());
        assertTrue(fat.resize(1, 600L, false, 10L).ok());
    }

    @Test
    void activate_marksOneAndClearsTheOthers() {
        DiskLayout layout = DiskLayout.empty(PartitionScheme.MBR, 20L);
        layout = made(layout.create(0L, 8L, FileSystemType.FAT16, PartitionRole.DATA));
        layout = made(layout.create(8L, 10L, FileSystemType.UFS, PartitionRole.DATA));
        layout = made(layout.activate(1));
        assertEquals(1, layout.active().number());
        layout = made(layout.activate(2));
        assertEquals(2, layout.active().number());
        assertFalse(layout.find(1).active());
        assertNull(DiskLayout.empty(PartitionScheme.MBR, 20L).active());
    }

    @Test
    void convert_onlyOnAnEmptyDisk() {
        final DiskLayout empty = DiskLayout.empty(PartitionScheme.MBR, FOUR_TB);
        assertEquals(PartitionScheme.GPT, made(empty.convert(PartitionScheme.GPT)).scheme());
        final DiskLayout holding = made(empty.create(0L, 10L, FileSystemType.NTFS, PartitionRole.DATA));
        assertEquals(DiskLayout.Refusal.NOT_EMPTY, holding.convert(PartitionScheme.GPT).refusal());
    }

    @Test
    void format_keepsWhereItLiesAndChecksItsFileSystemsLimit() {
        DiskLayout layout = made(DiskLayout.empty(PartitionScheme.MBR, 4_096L)
                .create(0L, 3_000L, FileSystemType.NTFS, PartitionRole.DATA));
        assertEquals(DiskLayout.Refusal.TOO_BIG_FOR_ITS_FILE_SYSTEM,
                layout.format(1, FileSystemType.FAT16, PartitionRole.DATA).refusal());
        layout = made(layout.format(1, FileSystemType.SWAP, PartitionRole.DATA));
        assertTrue(layout.find(1).swap());
        assertFalse(layout.find(1).holdsData());
        assertEquals(3_000L, layout.find(1).sizeMb());
    }

    private static DiskLayout made(final DiskLayout.Change change) {
        assertTrue(change.ok(), "refused: " + change.refusal());
        assertNotNull(change.layout());
        return change.layout();
    }
}
