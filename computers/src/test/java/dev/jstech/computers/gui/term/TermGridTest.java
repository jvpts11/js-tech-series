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

    /** A wider glass, as a monitor's is on a big window. */
    private static final int WIDE_GLASS = 540;
    /** The display scale a machine has when nobody set one: three quarters, as a desktop is drawn. */
    private static final float DEFAULT = TermGrid.scaleOf(0);

    @Test
    void scaleOf_isTheMachinesSettingOrThreeQuartersWhenItHasNone() {
        assertEquals(0.75f, TermGrid.scaleOf(0), 1e-6f);
        assertEquals(0.5f, TermGrid.scaleOf(50), 1e-6f);
        assertEquals(1.0f, TermGrid.scaleOf(100), 1e-6f);
    }

    @Test
    void fit_followsTheMachinesDisplayScaleWithinHalfAScreenPixel() {
        for (final int percent : new int[] {50, 60, 75, 90, 100}) {
            for (int gui = 2; gui <= 6; gui++) {
                final float wanted = Math.min(TermGrid.scaleOf(percent), WIDE_GLASS / 480f);
                final float drawn = TermGrid.fit(WIDE_GLASS, gui, 80, TermGrid.scaleOf(percent)).scale();
                assertTrue(Math.abs(drawn - wanted) * gui <= 0.5f + 1e-4f,
                        percent + "% at GUI scale " + gui + " drew " + drawn);
            }
        }
    }

    @Test
    void fit_drawsCrispAtTheNearestWholeScreenPixelThatFits() {
        assertFit(TermGrid.fit(GLASS, 4, 80, DEFAULT), 0, 0.75f, "three screen pixels each at GUI scale 4");
        assertFit(TermGrid.fit(GLASS, 3, 80, DEFAULT), 0, 2f / 3f, "two each at GUI scale 3, the nearest to 2.25");
        assertFit(TermGrid.fit(WIDE_GLASS, 3, 80, 1.0f), 0, 1.0f, "three each at full size on a wide glass");
    }

    @Test
    void fit_keepsTheScaleWhereTheNearestWholePixelWouldNotFit() {
        assertFit(TermGrid.fit(GLASS, 2, 80, DEFAULT), 0, 0.75f, "a pixel and a half each at GUI scale 2, not two");
    }

    @Test
    void fit_drawsTheGameFontOnCellsSixWideAndTenTall() {
        assertEquals(List.of(new TermGrid.Cell(6, 10)), TermGrid.SIZES);
        assertEquals(new TermGrid.Cell(TermGrid.CELL, TermGrid.ROW), TermGrid.fit(GLASS, 3, 80, DEFAULT).cell());
    }

    @Test
    void fit_alwaysFitsTheColumns() {
        for (final int glass : new int[] {300, GLASS, WIDE_GLASS}) {
            for (int gui = 1; gui <= 8; gui++) {
                for (final int percent : new int[] {50, 75, 100}) {
                    final TermGrid.Fit fit = TermGrid.fit(glass, gui, 80, TermGrid.scaleOf(percent));
                    assertTrue(80 * fit.cell().width() * fit.scale() <= glass + 1e-3,
                            "eighty columns fit a glass " + glass + " wide at " + percent + "%, GUI scale " + gui);
                }
            }
        }
    }

    @Test
    void fit_drawsSmallerOnlyWhenTheColumnsWouldNotFit() {
        final float drawn = TermGrid.fit(300, 1, 80, DEFAULT).scale();
        assertEquals(300 / 480f, drawn, 1e-6f, "a narrow glass shrinks the text to its columns");
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

    @Test
    void clip_neverReturnsMoreCellsThanItWasGiven() {
        assertEquals("a", TermGrid.clip("abcdef", 1));
        assertEquals("abc", TermGrid.clip("abcdef", 3));
        assertEquals("a...", TermGrid.clip("abcdef", 4));
    }
}
