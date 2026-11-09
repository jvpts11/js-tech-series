/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsckLinesTest {

    @Test
    void summary_saysWhatCameBackAndTheBadSectorsOnLinux() {
        final DefragState.Disk disk = new DefragState.Disk(1, "sdb1", 0, "ext4", 1_000L, 400L, 600, 0, 9, false, 0L,
                10L, 5);
        final List<String> lines = FsckLines.summary(DefragLook.LINUX, disk,
                new DefragState.Result(1, 412L, 391L, 21L, 12, 9));
        assertEquals("sdb1: 412 corrupted items: 391 recovered, 21 lost", lines.get(0));
        assertEquals("sdb1: bad sectors 12 -> 9", lines.get(1));
        assertTrue(lines.contains("sdb1: ***** FILE SYSTEM WAS MODIFIED *****"));
    }

    @Test
    void summary_onAWholeDiskSaysNothingWasModified() {
        final List<String> lines = FsckLines.summary(DefragLook.FREEBSD, null,
                new DefragState.Result(0, 0L, 0L, 0L, 0, 0));
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().endsWith("0.0% fragmentation)"));
    }

    @Test
    void summary_onQuinceNamesTheVolumeAndSaysWhetherItWasRepaired() {
        assertEquals(List.of("** The volume Quince HD appears to be OK."), FsckLines.summary(DefragLook.QUINCE, null,
                new DefragState.Result(0, 0L, 0L, 0L, 0, 0)));
        final List<String> lines = FsckLines.summary(DefragLook.QUINCE, null,
                new DefragState.Result(1, 7L, 7L, 0L, 0, 0));
        assertEquals("** The volume Untitled was repaired successfully.", lines.getLast());
        assertEquals(List.of("** /dev/rdisk0s2", "** Checking HFS Plus volume."),
                FsckLines.opening(DefragLook.QUINCE, 0));
        assertEquals(5, FsckLines.phases(DefragLook.QUINCE).size());
    }

    @Test
    void phasesBy_printsTheFirstAtOnceAndTheRestAsItGoes() {
        assertEquals(1, FsckLines.phasesBy(0));
        assertEquals(3, FsckLines.phasesBy(45));
        assertEquals(5, FsckLines.phasesBy(100));
        assertEquals(5, FsckLines.phases(DefragLook.UNIX).size());
    }
}
