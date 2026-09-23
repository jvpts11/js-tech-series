/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.gui.ColorContrast;
import org.junit.jupiter.api.Test;

class TermPaletteTest {

    /** What a line of text has to reach against its ground to be read without effort. */
    private static final double READABLE = 4.5;

    /** The inks each set ships with, which is what a player without a resource pack reads. */
    private static final TermInks GLASS = TermPalette.GLASS.declared();
    private static final TermInks PAPER = TermPalette.PAPER.declared();

    @Test
    void paper_everyStyleReadsOnThePaper() {
        for (final CliStyle style : CliStyle.values()) {
            final double ratio = ColorContrast.ratio(PAPER.of(style), PAPER.ground());
            assertTrue(ratio >= READABLE, style + " reads at " + ratio + " on the paper");
        }
    }

    @Test
    void paper_keepsTheStylesApartThatADarkGlassKeepsApart() {
        assertNotEquals(PAPER.of(CliStyle.OK), PAPER.of(CliStyle.ERROR));
        assertNotEquals(PAPER.of(CliStyle.WARN), PAPER.of(CliStyle.ERROR));
        assertNotEquals(PAPER.of(CliStyle.DIM), PAPER.of(CliStyle.PROMPT));
    }

    @Test
    void inksFor_answersThePaperInksOnALightGroundAndTheOthersOnADarkOne() {
        assertEquals(TermPalette.onPaper(CliStyle.ERROR),
                TermPalette.inksFor(PAPER.ground()).applyAsInt(CliStyle.ERROR));
        assertEquals(TermPalette.colorOf(CliStyle.ERROR),
                TermPalette.inksFor(GLASS.ground()).applyAsInt(CliStyle.ERROR));
    }

    @Test
    void lightGround_tellsThePaperFromTheGlass() {
        assertTrue(TermPalette.lightGround(PAPER.ground()));
        assertFalse(TermPalette.lightGround(GLASS.ground()));
    }

    @Test
    void selectionOn_washesInTheInkOfTheGround() {
        assertEquals(PAPER.selection(), TermPalette.selectionOn(PAPER.ground()));
        assertEquals(GLASS.selection(), TermPalette.selectionOn(GLASS.ground()));
    }
}
