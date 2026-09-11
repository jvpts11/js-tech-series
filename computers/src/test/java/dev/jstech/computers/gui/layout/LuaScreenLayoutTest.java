/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LuaScreenLayoutTest {

    @Test
    void inWindow_drawsAtFullSizeWhenTheWholeScreenFits() {
        final LuaScreenLayout.Placement at = LuaScreenLayout.inWindow(0, 0, LuaScreenLayout.fullWidth(),
                LuaScreenLayout.fullHeight(), 1);
        assertEquals(1f, at.scale());
        assertTrue(at.whole());
        assertEquals(LuaScreenLayout.PAD, at.x());
        assertEquals(LuaScreenLayout.PAD, at.y());
        assertEquals(306, at.width());
        assertEquals(171, at.height());
    }

    @Test
    void inWindow_stepsDownToThreeQuartersThenHalfNeverBetween() {
        assertEquals(0.75f, LuaScreenLayout.inWindow(0, 0, 300, 200, 1).scale());
        assertEquals(0.5f, LuaScreenLayout.inWindow(0, 0, 200, 120, 1).scale());
        assertEquals(0.5f, LuaScreenLayout.inWindow(0, 0, 120, 60, 1).scale());
    }

    @Test
    void inWindow_centresTheScreenInALargerWindow() {
        final LuaScreenLayout.Placement at = LuaScreenLayout.inWindow(10, 20, 406, 271, 1);
        assertEquals(10 + 50, at.x());
        assertEquals(20 + 50, at.y());
    }

    @Test
    void inPanel_drawsAtThreeQuartersAndFollowsTheCursor() {
        final LuaScreenLayout.Placement at = LuaScreenLayout.inPanel(0, 0, 400, 80, 19);
        assertEquals(0.75f, at.scale());
        assertEquals(10, at.rows());
        assertEquals(9, at.firstRow(), "the cursor at the bottom shows the last rows");
        assertTrue(!at.whole());
    }

    @Test
    void firstRow_keepsTheCursorInTheMiddleWithinTheScreen() {
        assertEquals(0, LuaScreenLayout.firstRow(10, 1));
        assertEquals(5, LuaScreenLayout.firstRow(10, 11));
        assertEquals(9, LuaScreenLayout.firstRow(10, 19));
        assertEquals(0, LuaScreenLayout.firstRow(19, 19));
    }

    @Test
    void cellAt_countsCellsFromOneAcrossTheRowsShowing() {
        final LuaScreenLayout.Placement at = new LuaScreenLayout.Placement(1f, 4, 4, 5, 10);
        assertArrayEquals(new int[] {1, 6}, at.cellAt(4, 4));
        assertArrayEquals(new int[] {51, 15}, at.cellAt(4 + 305, 4 + 89));
        assertArrayEquals(new int[] {3, 7}, at.cellAt(4 + 12.5, 4 + 9.5));
        assertNull(at.cellAt(3, 4));
        assertNull(at.cellAt(4 + 306, 4));
    }

    @Test
    void cellAt_scalesWithTheScreen() {
        final LuaScreenLayout.Placement at = new LuaScreenLayout.Placement(0.5f, 0, 0, 0, 19);
        assertArrayEquals(new int[] {2, 2}, at.cellAt(3, 4.5));
        assertArrayEquals(new int[] {51, 19}, at.cellAt(152.9, 85.4));
    }
}
