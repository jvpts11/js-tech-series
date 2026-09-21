/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.gui.ColorContrast;
import org.junit.jupiter.api.Test;

class TermPaletteTest {

    /** What a line of text has to reach against its ground to be read without effort. */
    private static final double READABLE = 4.5;

    /** The darkest ground a terminal window is drawn on. */
    private static final int DARK_GLASS = 0xFF000000;

    @Test
    void onPaper_everyStyleReadsOnThePaper() {
        for (final CliStyle style : CliStyle.values()) {
            final double ratio = ColorContrast.ratio(TermPalette.onPaper(style), TermPalette.PAPER);
            assertTrue(ratio >= READABLE, style + " reads at " + ratio + " on the paper");
        }
    }

    @Test
    void onPaper_keepsTheStylesApartThatADarkGlassKeepsApart() {
        assertNotEquals(TermPalette.onPaper(CliStyle.OK), TermPalette.onPaper(CliStyle.ERROR));
        assertNotEquals(TermPalette.onPaper(CliStyle.WARN), TermPalette.onPaper(CliStyle.ERROR));
        assertNotEquals(TermPalette.onPaper(CliStyle.DIM), TermPalette.onPaper(CliStyle.PROMPT));
    }

    @Test
    void inksFor_answersThePaperInksOnALightGroundAndTheOthersOnADarkOne() {
        assertEquals(TermPalette.onPaper(CliStyle.ERROR),
                TermPalette.inksFor(TermPalette.PAPER).applyAsInt(CliStyle.ERROR));
        assertEquals(TermPalette.colorOf(CliStyle.ERROR),
                TermPalette.inksFor(DARK_GLASS).applyAsInt(CliStyle.ERROR));
    }
}
