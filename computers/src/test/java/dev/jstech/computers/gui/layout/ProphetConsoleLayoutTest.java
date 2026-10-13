/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProphetConsoleLayoutTest {

    @Test
    void layout_isCleanAtEverySize() {
        assertTrue(ProphetConsoleLayout.layout().isClean(), () -> ProphetConsoleLayout.layout().overlaps().toString());
        assertTrue(ProphetConsoleLayout.layout(ProphetConsoleLayout.MIN_W, ProphetConsoleLayout.MIN_H).isClean());
        assertTrue(ProphetConsoleLayout.layout(640, 420).isClean());
    }

    @Test
    void layout_leavesTheListRoomForAFewStatesAtItsSmallest() {
        final int rows = (ProphetConsoleLayout.listH(ProphetConsoleLayout.MIN_H) - ProphetConsoleLayout.COLUMNS_H)
                / ProphetConsoleLayout.ROW_H;

        assertTrue(rows >= 3, "only " + rows + " rows fit");
    }

    @Test
    void columns_coverTheListLeftToRight() {
        final int[] columns = ProphetConsoleLayout.columns(10, 300);

        assertEquals(10, columns[0]);
        for (int i = 1; i < columns.length; i++) {
            assertTrue(columns[i] > columns[i - 1]);
        }
        assertTrue(columns[columns.length - 1] < 310);
    }

    @Test
    void levelY_putsTheTopAtTheTopAndNothingAtTheBottom() {
        assertEquals(0, ProphetConsoleLayout.levelY(1000, 1000, 50));
        assertEquals(50, ProphetConsoleLayout.levelY(0, 1000, 50));
        assertEquals(25, ProphetConsoleLayout.levelY(500, 1000, 50));
        assertEquals(0, ProphetConsoleLayout.levelY(5000, 1000, 50), "a level past the top is drawn at the top");
    }

    @Test
    void graphTop_leavesRoomOverTheBandAndTheLevels() {
        assertEquals(1100L, ProphetConsoleLayout.graphTop(500, 1000, 400));
        assertEquals(1650L, ProphetConsoleLayout.graphTop(1000, Long.MAX_VALUE, 900));
        assertEquals(2200L, ProphetConsoleLayout.graphTop(500, 1000, 2000));
    }

    @Test
    void scaled_staysInsideTheAxisForLevelsNearTheLargestLong() {
        final long big = 900_000_000_000_000_000L;
        assertEquals(50, ProphetConsoleLayout.scaled(big, big, 50));
        assertEquals(25, ProphetConsoleLayout.scaled(big / 2, big, 50));
        assertEquals(0, ProphetConsoleLayout.scaled(-5, big, 50));
    }

    @Test
    void graphTop_stopsAtTheLargestLongInsteadOfWrapping() {
        assertEquals(Long.MAX_VALUE, ProphetConsoleLayout.graphTop(0, Long.MAX_VALUE - 1, 0));
        assertEquals(1_000_000_000_000_000_000L + 100_000_000_000_000_000L,
                ProphetConsoleLayout.graphTop(0, 1_000_000_000_000_000_000L, 0));
    }
}
