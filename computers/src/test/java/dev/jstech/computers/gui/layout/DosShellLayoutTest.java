/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DosShellLayoutTest {

    @Test
    void of_isTheShellsOwnScreenAtEightyByTwentyFive() {
        final DosShellLayout layout = DosShellLayout.of(80, 25, DosShellLayout.View.PROGRAMS_AND_FILES, true);
        assertEquals(4, layout.topTitle());
        assertEquals(5, layout.topFirst());
        assertEquals(8, layout.topRows());
        assertEquals(13, layout.bottomTitle());
        assertEquals(10, layout.bottomRows());
        assertEquals(24, layout.keyRow());
        assertEquals(39, layout.leftWidth());
        assertEquals(40, layout.split());
        assertEquals(40, layout.rightWidth());
    }

    @Test
    void of_keepsEveryAreaAboveTheKeyLineOnATallerGlass() {
        for (final DosShellLayout.View view : DosShellLayout.View.values()) {
            for (int rows = 12; rows <= 60; rows++) {
                final DosShellLayout layout = DosShellLayout.of(80, rows, view, true);
                final int lastTop = layout.topFirst() + layout.topRows() - 1;
                final int lastBottom = layout.bottomFirst() + layout.bottomRows() - 1;
                if (layout.showsFiles()) {
                    assertTrue(lastTop < layout.keyRow(), view + " at " + rows + ": the file list runs into the keys");
                    assertTrue(!layout.showsPrograms() || lastTop < layout.bottomTitle(),
                            view + " at " + rows + ": the file list runs into the program list");
                }
                if (layout.showsPrograms()) {
                    assertTrue(lastBottom < layout.keyRow(),
                            view + " at " + rows + ": the program list runs into the keys");
                }
            }
        }
    }

    @Test
    void of_givesTheWholeHeightToTheOneThingAViewShows() {
        final DosShellLayout files = DosShellLayout.of(80, 25, DosShellLayout.View.FILES, true);
        assertFalse(files.showsPrograms());
        assertEquals(files.keyRow(), files.topFirst() + files.topRows());
        final DosShellLayout programs = DosShellLayout.of(80, 25, DosShellLayout.View.PROGRAMS, true);
        assertFalse(programs.showsFiles());
        assertEquals(programs.keyRow(), programs.bottomFirst() + programs.bottomRows());
    }

    @Test
    void inAreas_tellWhichAreaACellIsIn() {
        final DosShellLayout layout = DosShellLayout.of(80, 25, DosShellLayout.View.PROGRAMS_AND_FILES, true);
        assertTrue(layout.inTree(2, 6));
        assertFalse(layout.inTree(39, 6));
        assertTrue(layout.inFiles(45, 6));
        assertTrue(layout.inPrograms(2, 15));
        assertTrue(layout.inTasks(45, 15));
        assertFalse(layout.inPrograms(2, 13));
        final DosShellLayout alone = DosShellLayout.of(80, 25, DosShellLayout.View.PROGRAMS_AND_FILES, false);
        assertTrue(alone.inPrograms(60, 15));
        assertFalse(alone.inTasks(60, 15));
    }
}
