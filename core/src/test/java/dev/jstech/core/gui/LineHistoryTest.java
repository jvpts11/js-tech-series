/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The arrow keys at a command line: older, newer, and back to the empty line the walk began from. */
class LineHistoryTest {

    private LineHistory history;

    @BeforeEach
    void setUp() {
        history = new LineHistory();
    }

    @Test
    void recall_withNothingKeptChangesNothing() {
        assertEquals(Optional.empty(), history.recall(-1));
        assertEquals(Optional.empty(), history.recall(1));
    }

    @Test
    void recall_walksToOlderLinesAndStopsAtTheOldest() {
        history.add("ls");
        history.add("cd /mnt");
        assertEquals(Optional.of("cd /mnt"), history.recall(-1));
        assertEquals(Optional.of("ls"), history.recall(-1));
        assertEquals(Optional.of("ls"), history.recall(-1));
    }

    @Test
    void recall_pastTheNewestIsTheEmptyLineAndTheWalkStartsOver() {
        history.add("ls");
        history.add("cd /mnt");
        history.recall(-1);
        assertEquals(Optional.of(""), history.recall(1));
        assertEquals(Optional.of("cd /mnt"), history.recall(-1));
    }

    @Test
    void recall_downwardsWithNoWalkBegunIsTheEmptyLine() {
        history.add("ls");
        assertEquals(Optional.of(""), history.recall(1));
    }

    @Test
    void add_keepsALineTypedTwiceRunningOnce_andNeverAnEmptyOne() {
        history.add("ls");
        history.add("ls");
        history.add("");
        assertEquals(Optional.of("ls"), history.recall(-1));
        assertEquals(Optional.of("ls"), history.recall(-1));
        assertEquals(Optional.of(""), history.recall(1));
    }

    @Test
    void add_endsTheWalk_soTheNextUpIsTheNewestLineAgain() {
        history.add("ls");
        history.add("pwd");
        history.recall(-1);
        history.recall(-1);
        history.add("whoami");
        assertEquals(Optional.of("whoami"), history.recall(-1));
    }

    @Test
    void rest_endsTheWalkAndKeepsNothing() {
        history.add("ls");
        history.add("pwd");
        history.recall(-1);
        history.recall(-1);
        history.rest();
        assertEquals(Optional.of("pwd"), history.recall(-1));
    }

    @Test
    void replaceWith_takesTheLinesAMachineRemembers() {
        history.add("stale");
        history.replaceWith(List.of("mount /dev/sda2 /mnt", "lsblk"));
        assertEquals(Optional.of("lsblk"), history.recall(-1));
        assertEquals(Optional.of("mount /dev/sda2 /mnt"), history.recall(-1));
        assertEquals(Optional.of("mount /dev/sda2 /mnt"), history.recall(-1));
    }
}
