/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.ColorContrast;
import org.junit.jupiter.api.Test;

class InkPaletteTest {

    /**
     * The floor a piece of source has to clear against the paper it is on. Below this the colour is
     * there but the player cannot read it, which is the bug this test exists to stop.
     */
    private static final double READABLE = 3.5;

    /** A line number or a rule may be quieter than code, but it still has to be seen. */
    private static final double VISIBLE = 2.0;

    /** The colours each set ships with, which is what a player without a resource pack reads. */
    private static final InkPalette LIGHT = InkPalette.LIGHT.declared();
    private static final InkPalette DARK = InkPalette.DARK.declared();
    private static final InkPalette GLASS = InkPalette.GLASS.declared();

    private static void assertReadable(final InkPalette palette, final String where) {
        for (final CodeRuns.Ink ink : CodeRuns.Ink.values()) {
            final double ratio = ColorContrast.ratio(palette.of(ink), palette.ground());
            assertTrue(ratio >= READABLE,
                    where + ": " + ink + " reads at " + String.format("%.2f", ratio) + " on its ground");
        }
    }

    @Test
    void of_isReadableOnTheLightGround() {
        assertReadable(LIGHT, "light");
    }

    @Test
    void of_isReadableOnTheDarkGround() {
        assertReadable(DARK, "dark");
    }

    /** A monitor's bare glass is black, and an editor that takes it over stays on that black. */
    @Test
    void glass_isBlackAndEverythingOnItReads() {
        assertEquals(0xFF000000, GLASS.ground());
        assertReadable(GLASS, "glass");
        assertTrue(ColorContrast.ratio(GLASS.gutterText(), GLASS.gutter()) >= VISIBLE);
        assertTrue(ColorContrast.ratio(GLASS.caret(), GLASS.ground()) >= READABLE);
        // An editor writes its title and its keys the other way round: the ground's colour on the text's.
        assertTrue(ColorContrast.ratio(GLASS.ground(), GLASS.plain()) >= READABLE);
    }

    @Test
    void gutterText_isVisibleOnTheGutterOfBothPalettes() {
        assertTrue(ColorContrast.ratio(LIGHT.gutterText(), LIGHT.gutter()) >= VISIBLE,
                "light gutter numbers are lost on the gutter");
        assertTrue(ColorContrast.ratio(DARK.gutterText(), DARK.gutter()) >= VISIBLE,
                "dark gutter numbers are lost on the gutter");
    }

    @Test
    void caret_standsOutAgainstTheGround() {
        assertTrue(ColorContrast.ratio(LIGHT.caret(), LIGHT.ground()) >= READABLE);
        assertTrue(ColorContrast.ratio(DARK.caret(), DARK.ground()) >= READABLE);
    }

    @Test
    void currentLine_isTellableFromTheGroundWithoutDrowningTheCodeOnIt() {
        for (final InkPalette palette : new InkPalette[] {LIGHT, DARK}) {
            assertTrue(palette.currentLine() != palette.ground(), "the current line is invisible");
            for (final CodeRuns.Ink ink : CodeRuns.Ink.values()) {
                assertTrue(ColorContrast.ratio(palette.of(ink), palette.currentLine()) >= READABLE,
                        ink + " is lost on the highlighted line");
            }
        }
    }

    @Test
    void forGround_picksThePaletteThatMatchesTheWindow() {
        assertSame(InkPalette.DARK, InkPalette.forGround(true));
        assertSame(InkPalette.LIGHT, InkPalette.forGround(false));
    }

    @Test
    void of_answersForEveryInkTheRunBuilderCanProduce() {
        for (final CodeRuns.Ink ink : CodeRuns.Ink.values()) {
            assertTrue((LIGHT.of(ink) >>> 24) == 0xFF, ink + " is not opaque on the light palette");
            assertTrue((DARK.of(ink) >>> 24) == 0xFF, ink + " is not opaque on the dark palette");
        }
    }
}
