/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TermGridTest {

    /** The glass a monitor's console has for its columns: the monitor's glass less its margins. */
    private static final int GLASS = 364;

    @Test
    void fit_givesEveryFontPixelAWholeNumberOfScreenPixels() {
        for (int gui = 2; gui <= 8; gui++) {
            final double screenPixels = TermGrid.fit(GLASS, gui, TermBuffer.MONITOR_COLUMNS).scale() * gui;
            assertEquals(Math.round(screenPixels), screenPixels, 1e-4, "at GUI scale " + gui);
        }
    }

    @Test
    void fit_picksTheSizeAndScaleThatDrawTheWidestCellThatStillFits() {
        assertFit(TermGrid.fit(GLASS, 2, 80), 1, 0.5f, "the 9x15 at one screen pixel each at GUI scale 2");
        assertFit(TermGrid.fit(GLASS, 3, 80), 0, 2f / 3f, "the 6x10 at two each at GUI scale 3");
        assertFit(TermGrid.fit(GLASS, 4, 80), 1, 0.5f, "the 9x15 at two each at GUI scale 4");
        assertFit(TermGrid.fit(GLASS, 5, 80), 2, 0.4f, "the 10x20 at two each at GUI scale 5");
    }

    @Test
    void fit_alwaysFitsTheColumnsAndNeverDrawsSmallerThanTheSmallSizeAtOnePixel() {
        for (int gui = 2; gui <= 8; gui++) {
            final TermGrid.Fit fit = TermGrid.fit(GLASS, gui, 80);
            final double cell = fit.cell().width() * fit.scale() * gui;
            assertTrue(80 * cell <= GLASS * gui + 1e-6, "eighty columns fit the glass at GUI scale " + gui);
            assertTrue(cell >= TermGrid.CELL, "no smaller than the small size at one pixel, at GUI scale " + gui);
        }
    }

    @Test
    void fit_prefersTheLargerSizeWhenTwoDrawCellsAsWide() {
        // At GUI scale 4 the 6x10 at three pixels and the 9x15 at two are both eighteen screen pixels across.
        assertEquals(1, TermGrid.fit(GLASS, 4, 80).size());
    }

    @Test
    void fit_drawsSmallerThanOnePixelOnlyWhenNothingElseFits() {
        final TermGrid.Fit fit = TermGrid.fit(GLASS, 1, 80);
        assertFit(fit, 0, GLASS / 480f, "a glass too small for one screen pixel each fits the small size");
    }

    @Test
    void cells_countsCharactersNotChars() {
        assertEquals(3, TermGrid.cells("abc"));
        assertEquals(2, TermGrid.cells(Character.toString(0x1D11E) + "a"), "a character beyond the basic plane");
    }

    @Test
    void first_takesAsManyCharactersAsTheCellsHold() {
        assertEquals("abc", TermGrid.first("abcdef", 3));
        assertEquals("ab", TermGrid.first("ab", 5));
        assertEquals("", TermGrid.first("abc", 0));
    }

    @Test
    void clip_endsInDotsOnlyWhereItHadToCut() {
        assertEquals("short", TermGrid.clip("short", 10));
        assertEquals("a long...", TermGrid.clip("a long name for a disk", 9));
        assertEquals(9, TermGrid.cells(TermGrid.clip("a long name for a disk", 9)));
    }

    @Test
    void wrap_breaksAtSpacesAndInsideAWordOnlyWhenItIsLongerThanALine() {
        assertEquals(List.of("the quick", "brown fox"), TermGrid.wrap("the quick brown fox", 10));
        assertEquals(List.of("abcde", "fghij", "k"), TermGrid.wrap("abcdefghijk", 5));
        assertEquals(List.of(""), TermGrid.wrap("", 5));
        assertEquals(List.of("a  b"), TermGrid.wrap("a  b", 10), "the spaces inside a line are kept");
    }

    private static void assertFit(final TermGrid.Fit fit, final int size, final float scale, final String what) {
        assertEquals(size, fit.size(), what);
        assertEquals(scale, fit.scale(), 1e-6f, what);
    }
}
