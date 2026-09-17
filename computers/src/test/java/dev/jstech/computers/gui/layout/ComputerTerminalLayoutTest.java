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

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class ComputerTerminalLayoutTest {

    @Test
    void layout_isCleanForPersonalComputerHost() {
        final GuiLayout l = ComputerTerminalLayout.layout(false);
        assertTrue(l.overlaps().isEmpty(), "overlaps (PC): " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds (PC): " + l.outOfBounds());
    }

    @Test
    void layout_isCleanForMainframeHost() {
        final GuiLayout l = ComputerTerminalLayout.layout(true);
        assertTrue(l.overlaps().isEmpty(), "overlaps (Mainframe): " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds (Mainframe): " + l.outOfBounds());
    }

    @Test
    void visibleRows_holdsWhatTheRailHasRoomFor() {
        assertEquals(1, ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.TAB_H));
        assertEquals(4, ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.TAB_H * 4));
        // A part-height entry is not shown: half a tab is not a tab.
        assertEquals(4, ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.TAB_H * 4 + 13));
    }

    @Test
    void visibleRows_neverFallsToNone() {
        // A rail with room for nothing would show nothing at all, which is worse than showing one.
        assertEquals(1, ComputerTerminalLayout.visibleRows(0));
        assertEquals(1, ComputerTerminalLayout.visibleRows(-40));
    }

    @Test
    void railHeight_isWhatIsLeftAboveTheInventory() {
        assertEquals(148 - ComputerTerminalLayout.TAB_Y0 - 2, ComputerTerminalLayout.railHeight(148));
    }

    @Test
    void maxScroll_isNoneWhileEverythingFits() {
        assertEquals(0, ComputerTerminalLayout.maxScroll(4, 6));
        assertEquals(0, ComputerTerminalLayout.maxScroll(6, 6));
        assertEquals(2, ComputerTerminalLayout.maxScroll(8, 6));
    }

    @Test
    void clampScroll_bringsAPositionBackIntoRange() {
        assertEquals(0, ComputerTerminalLayout.clampScroll(-3, 8, 6));
        assertEquals(2, ComputerTerminalLayout.clampScroll(9, 8, 6));
        assertEquals(1, ComputerTerminalLayout.clampScroll(1, 8, 6));
        // Everything fits, so the only position is the top, whatever was asked for.
        assertEquals(0, ComputerTerminalLayout.clampScroll(3, 4, 6));
    }

    @Test
    void rowAt_findsTheRowTheSameGeometryDraws() {
        for (int row = 0; row < 5; row++) {
            final int top = ComputerTerminalLayout.rowY(row);
            assertEquals(row, ComputerTerminalLayout.rowAt(ComputerTerminalLayout.RAIL_X, top, 5),
                    "the top edge of row " + row);
            assertEquals(row, ComputerTerminalLayout.rowAt(
                    ComputerTerminalLayout.RAIL_X + ComputerTerminalLayout.RAIL_W - 1,
                    top + ComputerTerminalLayout.TAB_H - 1, 5), "the far corner of row " + row);
        }
    }

    @Test
    void rowAt_answersNoneOffTheRail() {
        final int inside = ComputerTerminalLayout.rowY(0) + 1;
        assertEquals(-1, ComputerTerminalLayout.rowAt(ComputerTerminalLayout.RAIL_X - 1, inside, 5));
        assertEquals(-1, ComputerTerminalLayout.rowAt(
                ComputerTerminalLayout.RAIL_X + ComputerTerminalLayout.RAIL_W, inside, 5));
        assertEquals(-1, ComputerTerminalLayout.rowAt(ComputerTerminalLayout.RAIL_X, 0, 5));
    }

    @Test
    void rowAt_answersNoneBelowTheLastShownRow() {
        // Two rows shown: a point in what would be the third belongs to nothing.
        assertEquals(-1, ComputerTerminalLayout.rowAt(
                ComputerTerminalLayout.RAIL_X, ComputerTerminalLayout.rowY(2) + 1, 2));
    }
}
