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

import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class SoundfoundryStandardLayoutTest {

    private static final int[][] SIZES = {
            {SoundfoundryStandardLayout.MIN_W, SoundfoundryStandardLayout.MIN_H},
            {SoundfoundryStandardLayout.DEFAULT_W, SoundfoundryStandardLayout.DEFAULT_H},
            {960, 540}};

    @Test
    void layout_isCleanFromTheSmallestWindowToTheWholeDesktop() {
        for (final int[] size : SIZES) {
            assertClean(SoundfoundryStandardLayout.layout(size[0], size[1]), size);
            assertClean(SoundfoundryStandardLayout.albumLayout(size[0], size[1]), size);
            assertClean(SoundfoundryStandardLayout.localLayout(size[0], size[1], true), size);
            assertClean(SoundfoundryStandardLayout.localLayout(size[0], size[1], false), size);
        }
    }

    @Test
    void cardsAcross_fitsFourAlbumsOnTheDefaultWindowAndMoreOnAWiderOne() {
        final int page = SoundfoundryStandardLayout.page(SoundfoundryStandardLayout.DEFAULT_W,
                SoundfoundryStandardLayout.DEFAULT_H).w();
        assertEquals(4, SoundfoundryStandardLayout.cardsAcross(page), "the default window shows four, as drawn");
        assertEquals(3, SoundfoundryStandardLayout.cardsAcross(page - 114));
        assertTrue(SoundfoundryStandardLayout.cardsAcross(960 - SoundfoundryStandardLayout.SIDE_W) > 4);
    }

    @Test
    void secondColumn_isWhereTheDownloadedListStartsOnTheDefaultWindow() {
        assertEquals(250, SoundfoundryStandardLayout.secondColumn(480));
    }

    @Test
    void serverBox_sitsAtTheSidebarsFootAboveTheBar() {
        assertEquals(new Rect(8, 314, 134, 34), SoundfoundryStandardLayout.serverBox(410));
        assertEquals(14, SoundfoundryStandardLayout.playlistsShown(410));
    }

    @Test
    void positionWidth_narrowsOnANarrowWindowAndStopsAtItsFullLength() {
        assertEquals(260, SoundfoundryStandardLayout.positionWidth(SoundfoundryStandardLayout.DEFAULT_W));
        assertEquals(260, SoundfoundryStandardLayout.positionWidth(1200));
        assertEquals(150, SoundfoundryStandardLayout.positionWidth(SoundfoundryStandardLayout.MIN_W));
    }

    @Test
    void rowAt_findsTheRowUnderAPointAndNothingPastTheLast() {
        assertEquals(0, SoundfoundryStandardLayout.rowAt(168, 168, 13, 5));
        assertEquals(2, SoundfoundryStandardLayout.rowAt(168 + 2 * 13 + 4, 168, 13, 5));
        assertEquals(-1, SoundfoundryStandardLayout.rowAt(150, 168, 13, 5));
        assertEquals(-1, SoundfoundryStandardLayout.rowAt(168 + 5 * 13, 168, 13, 5));
    }

    private static void assertClean(final GuiLayout layout, final int[] size) {
        assertTrue(layout.isClean(), () -> size[0] + "x" + size[1] + ": overlaps " + layout.overlaps()
                + ", out of bounds " + layout.outOfBounds());
    }
}
