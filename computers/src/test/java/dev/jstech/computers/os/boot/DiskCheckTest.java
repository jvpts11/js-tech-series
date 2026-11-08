/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiskCheckTest {

    @Test
    void look_isEachSystemsOwnAndNoneForMcDos() {
        assertEquals(DiskCheck.Look.SCANDISK, DiskCheck.Look.of(Platform.FRAMES, 1));
        assertEquals(DiskCheck.Look.CHKDSK_XP, DiskCheck.Look.of(Platform.FRAMES, 2));
        assertEquals(DiskCheck.Look.CHKDSK_7, DiskCheck.Look.of(Platform.FRAMES, 3));
        assertEquals(DiskCheck.Look.FRAMES_10, DiskCheck.Look.of(Platform.FRAMES, 4));
        assertEquals(DiskCheck.Look.FRAMES_11, DiskCheck.Look.of(Platform.FRAMES, 5));
        assertEquals(DiskCheck.Look.LINUX, DiskCheck.Look.of(Platform.LINUX, 0));
        assertEquals(DiskCheck.Look.FREEBSD, DiskCheck.Look.of(Platform.FREEBSD, 0));
        assertEquals(DiskCheck.Look.UNIX, DiskCheck.Look.of(Platform.UNIX, 0));
        assertEquals(DiskCheck.Look.NONE, DiskCheck.Look.of(Platform.MC_DOS, 0));
        assertEquals(DiskCheck.Look.NONE, DiskCheck.Look.of(Platform.MC_NET, 0));
    }

    @Test
    void isEmpty_whenNothingIsShownOrItTakesNoTime() {
        assertTrue(DiskCheck.NONE.isEmpty());
        assertTrue(new DiskCheck(DiskCheck.Look.LINUX, FileSystemType.BTRFS, 10, 100, 1, 0).isEmpty(),
                "copy-on-write has nothing to check");
        assertFalse(new DiskCheck(DiskCheck.Look.LINUX, FileSystemType.EXT4, 10, 100, 1, 40).isEmpty());
    }

    @Test
    void consoleLines_linuxReplaysTheJournalLineByLine() {
        final DiskCheck check = new DiskCheck(DiskCheck.Look.LINUX, FileSystemType.EXT4, 2_048, 102_400, 5, 40);
        assertEquals(1, check.consoleLines(0).size(), "the kernel finds it first");
        final List<String> all = check.consoleLines(40);
        assertEquals(5, all.size());
        assertTrue(all.get(0).contains("EXT4-fs (sda1): INFO: recovery required on readonly filesystem"), all.get(0));
        assertEquals("/dev/sda1: recovering journal", all.get(2));
        assertEquals("/dev/sda1: clean, 16/6553600 files, 524288/26214400 blocks", all.get(3));
    }

    @Test
    void consoleLines_ufsSaysWhatItFoundOnlyAtTheEndOfItsPasses() {
        final DiskCheck check = new DiskCheck(DiskCheck.Look.FREEBSD, FileSystemType.UFS, 1, 2, 0, 100);
        final List<String> early = check.consoleLines(10);
        assertTrue(early.size() >= 5 && early.size() < 10, "the heading and the first passes; got " + early);
        final List<String> done = check.consoleLines(100);
        assertEquals("***** FILE SYSTEM MARKED CLEAN *****", done.getLast());
        assertTrue(done.contains("3 files, 256 used, 256 free (0 frags, 32 blocks, 0.0% fragmentation)"),
                done.toString());
    }

    @Test
    void consoleLines_systemVChecksItsRootInItsOwnWords() {
        final List<String> lines = new DiskCheck(DiskCheck.Look.UNIX, FileSystemType.UFS, 1, 2, 2, 100)
                .consoleLines(100);
        assertEquals("The / file system (/dev/root) is being checked.", lines.getFirst());
        assertTrue(lines.contains("32 files 2048 blocks 2048 free"), lines.toString());
        assertEquals("*** FILE SYSTEM WAS MODIFIED ***", lines.getLast());
    }

    @Test
    void consoleLines_framesHasNoConsole() {
        assertTrue(new DiskCheck(DiskCheck.Look.SCANDISK, FileSystemType.FAT32, 1, 2, 0, 100).consoleLines(50)
                .isEmpty());
    }

    @Test
    void through_runsFromNothingToAll() {
        final DiskCheck check = new DiskCheck(DiskCheck.Look.UNIX, FileSystemType.UFS, 1, 2, 0, 100);
        assertEquals(0.0, check.through(0));
        assertEquals(0.5, check.through(50));
        assertEquals(1.0, check.through(500));
    }
}
