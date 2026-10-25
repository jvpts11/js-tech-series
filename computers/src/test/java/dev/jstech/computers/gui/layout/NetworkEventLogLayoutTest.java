/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class NetworkEventLogLayoutTest {

    /** The Log tab at the window's default width and at its narrowest: the window less its padding. */
    private static final int DEFAULT_W = NetworkServicesLayout.DEFAULT_W - NetworkServicesLayout.WINDOW_PAD;
    private static final int NARROWEST = NetworkServicesLayout.MIN_W - NetworkServicesLayout.WINDOW_PAD;
    /** The tab's height at the window's default height and at its lowest, under the tabs and the network's line. */
    private static final int DEFAULT_H = NetworkServicesLayout.DEFAULT_H - 38;
    private static final int LOWEST = 150 - 38;

    @Test
    void events_isCleanAtTheDefaultSize() {
        final GuiLayout layout = NetworkEventLogLayout.events(DEFAULT_W, DEFAULT_H);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void events_isCleanAtTheNarrowestAndLowestWindow() {
        final GuiLayout layout = NetworkEventLogLayout.events(NARROWEST, LOWEST);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void filters_fitSideBySideInTheNarrowestWindow() {
        assertTrue(NetworkEventLogLayout.MIN_W <= NARROWEST, "the filters need " + NetworkEventLogLayout.MIN_W);
    }

    @Test
    void columns_holdTheLongestTimeLevelAndMachine() {
        assertTrue("Day 1234".length() * GuiLayout.GLYPH_WIDTH <= NetworkEventLogLayout.TIME_W, "the time");
        assertTrue("ERROR".length() * GuiLayout.GLYPH_WIDTH <= NetworkEventLogLayout.LEVEL_W, "the level");
        assertTrue("SRV-3fa2c1".length() * GuiLayout.GLYPH_WIDTH <= NetworkEventLogLayout.MACHINE_W, "the machine");
    }

    @Test
    void eventW_leavesWhatHappenedRoomAtTheNarrowest() {
        assertTrue(NetworkEventLogLayout.eventW(NARROWEST) >= 100, "what happened at the narrowest");
    }

    @Test
    void listH_showsRowsAboveTheFootAtTheLowest() {
        assertTrue(NetworkEventLogLayout.listH(LOWEST) / NetworkEventLogLayout.ROW_H >= 3, "rows at the lowest");
        assertTrue(NetworkEventLogLayout.LIST_Y + NetworkEventLogLayout.listH(LOWEST)
                <= NetworkEventLogLayout.footY(LOWEST), "the list ends before the count and the buttons");
    }
}
