/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.ColorContrast;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CdePaletteTest {

    @Test
    void all_offersEightUnderNamesOfTheirOwnWithTheDefaultFirst() {
        assertEquals(8, CdePalette.ALL.size());
        assertSame(CdePalette.DEFAULT, CdePalette.ALL.get(0));
        final Set<String> names = new HashSet<>();
        for (final CdePalette palette : CdePalette.ALL) {
            assertTrue(names.add(palette.name()), palette.name() + " is listed twice");
        }
    }

    @Test
    void named_findsAPaletteWhateverCaseItIsAskedForIn() {
        assertEquals("Desert", CdePalette.named("desert").name());
        assertEquals("SeaFoam", CdePalette.named("SEAFOAM").name());
    }

    @Test
    void named_givesTheDefaultForANameNobodyHas() {
        assertSame(CdePalette.DEFAULT, CdePalette.named("Mauve"));
        assertSame(CdePalette.DEFAULT, CdePalette.named(""));
    }

    @Test
    void ink_readsOnTheWindowGreyAndOnAWellInEveryPalette() {
        for (final CdePalette palette : CdePalette.ALL) {
            assertTrue(ColorContrast.ratio(palette.ink(), palette.window()) >= 4.5,
                    palette.name() + ": text on the window grey is " + ColorContrast.ratio(palette.ink(),
                            palette.window()));
            assertTrue(ColorContrast.ratio(palette.ink(), palette.inset()) >= 4.5,
                    palette.name() + ": text in a well is " + ColorContrast.ratio(palette.ink(), palette.inset()));
        }
    }

    @Test
    void activeTitle_readsInWhiteInEveryPalette() {
        for (final CdePalette palette : CdePalette.ALL) {
            assertTrue(ColorContrast.ratio(palette.activeInk(), palette.active()) >= 3.0,
                    palette.name() + ": a white title on the active colour is "
                            + ColorContrast.ratio(palette.activeInk(), palette.active()));
        }
    }

    @Test
    void relief_isLighterAboveAndDarkerBelowTheGreyItStandsOn() {
        for (final CdePalette palette : CdePalette.ALL) {
            assertTrue(ColorContrast.luminance(palette.light()) > ColorContrast.luminance(palette.window()),
                    palette.name() + ": the lit edge is not lighter than the grey");
            assertTrue(ColorContrast.luminance(palette.shade()) < ColorContrast.luminance(palette.window()),
                    palette.name() + ": the shaded edge is not darker than the grey");
        }
    }

    @Test
    void activeColour_standsApartFromTheGreyOfAnIdleTitle() {
        for (final CdePalette palette : CdePalette.ALL) {
            assertTrue(ColorContrast.ratio(palette.active(), palette.window()) >= 1.5,
                    palette.name() + ": the window in front would not read as in front");
        }
    }
}
