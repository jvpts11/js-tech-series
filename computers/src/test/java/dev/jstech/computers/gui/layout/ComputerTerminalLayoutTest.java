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

    /** How tall a line of the game's own font is, which is what every clearance here is measured against. */
    private static final int LINE_H = 9;

    /** The first line the panel writes, which every band drawn under it has to start below. */
    private static final int PANEL_TITLE_DY = 6;

    @Test
    void layout_isClean() {
        final GuiLayout l = ComputerTerminalLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void layout_fillsTheMonitorGlass() {
        // The space is the whole screen on every machine, which is what a system's interface is.
        assertEquals(384, ComputerTerminalLayout.WIDTH);
        assertEquals(256, ComputerTerminalLayout.HEIGHT);
    }

    @Test
    void grid_linesUpWithThePlayersOwnRows() {
        // Nine wide and in the same column, so a stack moves between them without the eye jumping.
        assertEquals(9, ComputerTerminalLayout.GRID_COLS);
        assertEquals(ComputerTerminalLayout.INV_X, ComputerTerminalLayout.GRID_X);
    }

    @Test
    void grid_stopsShortOfThePanelBesideIt() {
        final int gridRight = ComputerTerminalLayout.GRID_X
                + ComputerTerminalLayout.GRID_COLS * ComputerTerminalLayout.SLOT;
        assertTrue(gridRight <= ComputerTerminalLayout.PANE_X,
                "the grid runs to " + gridRight + " and the panel starts at " + ComputerTerminalLayout.PANE_X);
    }

    @Test
    void theDepositButton_clearsTheRuleAndThePlayersOwnRows() {
        /*
         * Sixteen pixels between the rule and the first row of slots, and a button in them. It shipped at
         * the rule's own line, drawn over it, which is exactly what it looked like.
         */
        assertTrue(ComputerTerminalLayout.DEPOSIT_Y > ComputerTerminalLayout.INV_LINE_Y + 1,
                "the deposit button sits on the rule above it");
        assertTrue(ComputerTerminalLayout.DEPOSIT_Y + ComputerTerminalLayout.DEPOSIT_H
                        < ComputerTerminalLayout.INV_Y,
                "the deposit button runs into the player's own slots");
    }

    @Test
    void aDiskRow_hasRoomForItsNameItsTrackAndItsHandle() {
        /*
         * A disk's name sits above its track and the handle stands proud of the track at each end, so a
         * pitch under all of that draws the next disk's name across the handle above it. It shipped at
         * fourteen against a row that needs twenty-one, and that is exactly what it did.
         */
        final int needed = ComputerTerminalLayout.SLIDER_LABEL_DY
                + ComputerTerminalLayout.SLIDER_TRACK_H
                + ComputerTerminalLayout.SLIDER_HANDLE_OVERHANG;
        assertTrue(ComputerTerminalLayout.SLIDER_ROW_PITCH >= needed,
                "a disk row needs " + needed + " pixels and the pitch is "
                        + ComputerTerminalLayout.SLIDER_ROW_PITCH);
    }

    @Test
    void aDiskName_endsBeforeTheHandleOnItsOwnRow() {
        /*
         * The name is written at the left edge of the panel and the handle runs the length of the track, so
         * a disk offering nothing puts its handle directly under its own name. The name has to finish above
         * the top of that handle, which stands proud of the track, or the two are drawn through each other.
         */
        assertTrue(ComputerTerminalLayout.SLIDER_LABEL_DY
                        >= LINE_H + ComputerTerminalLayout.SLIDER_HANDLE_OVERHANG,
                "a disk's name is drawn across its own handle");
    }

    @Test
    void theFirstDiskRow_startsBelowThePanelsTitle() {
        final int titleEnd = PANEL_TITLE_DY + LINE_H;
        final int firstName = ComputerTerminalLayout.SLIDER_TRACK0_DY
                - ComputerTerminalLayout.SLIDER_LABEL_DY;
        assertTrue(firstName > titleEnd,
                "the first disk's name is written into the panel's title, which ends at " + titleEnd);
    }

    @Test
    void theSliderBand_staysInsideThePanel() {
        final int lastTrack = ComputerTerminalLayout.SLIDER_TRACK0_DY
                + (ComputerTerminalLayout.sliderRows() - 1) * ComputerTerminalLayout.SLIDER_ROW_PITCH;
        assertTrue(ComputerTerminalLayout.sliderRows() >= 2,
                "even a personal computer has two disks to offer");
        assertTrue(lastTrack + ComputerTerminalLayout.SLIDER_TRACK_H
                        + ComputerTerminalLayout.SLIDER_HANDLE_OVERHANG <= ComputerTerminalLayout.PANE_H,
                "the last track runs past the foot of the panel, where nobody can reach it");
    }

    @Test
    void thePanel_stopsAboveTheRule() {
        assertTrue(ComputerTerminalLayout.PANE_Y + ComputerTerminalLayout.PANE_H
                        <= ComputerTerminalLayout.INV_LINE_Y,
                "the panel runs past the rule that separates the screen from the player's rows");
    }

    @Test
    void hotbar_sitsInsideTheGlass() {
        assertTrue(ComputerTerminalLayout.HOTBAR_Y + ComputerTerminalLayout.SLOT
                <= ComputerTerminalLayout.HEIGHT, "the hotbar runs past the bottom of the glass");
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
    void railHeight_isWhatIsLeftUnderTheStatusBar() {
        assertEquals(ComputerTerminalLayout.HEIGHT - ComputerTerminalLayout.TAB_Y0 - 2,
                ComputerTerminalLayout.railHeight());
    }

    @Test
    void rail_holdsEveryHeadingAMachineOffers() {
        // Ten on a Mainframe that can craft and be taught, which is the most any machine shows.
        assertTrue(ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.railHeight()) >= 10,
                "the rail holds only "
                        + ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.railHeight()) + " headings");
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
