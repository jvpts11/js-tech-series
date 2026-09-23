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

class CdeSchemeTest {

    @Test
    void all_offersEightUnderNamesOfTheirOwnWithTheDefaultFirst() {
        assertEquals(8, CdeScheme.ALL.size());
        assertSame(CdeScheme.DEFAULT, CdeScheme.ALL.get(0));
        final Set<String> names = new HashSet<>();
        for (final CdeScheme scheme : CdeScheme.ALL) {
            assertTrue(names.add(scheme.label()), scheme.label() + " is listed twice");
        }
    }

    @Test
    void all_declaresAPaletteForEachSchemeUnderAPathOfItsOwn() {
        final Set<String> ids = new HashSet<>();
        for (final CdeScheme scheme : CdeScheme.ALL) {
            assertTrue(scheme.palette().id().startsWith("jsc:cde/"), scheme.palette().id());
            assertTrue(ids.add(scheme.palette().id()), scheme.palette().id() + " is declared twice");
        }
    }

    @Test
    void named_findsASchemeWhateverCaseItIsAskedForIn() {
        assertSame(CdeScheme.DESERT, CdeScheme.named("desert"));
        assertSame(CdeScheme.SEA_FOAM, CdeScheme.named("SEAFOAM"));
    }

    @Test
    void named_givesTheDefaultForANameNobodyHas() {
        assertSame(CdeScheme.DEFAULT, CdeScheme.named("Mauve"));
        assertSame(CdeScheme.DEFAULT, CdeScheme.named(""));
    }

    @Test
    void ink_readsOnTheWindowGreyAndOnAWellInEveryScheme() {
        for (final CdeScheme scheme : CdeScheme.ALL) {
            final CdePalette palette = scheme.palette().declared();
            assertTrue(ColorContrast.ratio(palette.ink(), palette.window()) >= 4.5,
                    scheme.label() + ": text on the window grey is " + ColorContrast.ratio(palette.ink(),
                            palette.window()));
            assertTrue(ColorContrast.ratio(palette.ink(), palette.inset()) >= 4.5,
                    scheme.label() + ": text in a well is " + ColorContrast.ratio(palette.ink(), palette.inset()));
        }
    }

    @Test
    void activeTitle_readsInEveryScheme() {
        for (final CdeScheme scheme : CdeScheme.ALL) {
            final CdePalette palette = scheme.palette().declared();
            assertTrue(ColorContrast.ratio(palette.activeInk(), palette.active()) >= 3.0,
                    scheme.label() + ": a title on the active colour is "
                            + ColorContrast.ratio(palette.activeInk(), palette.active()));
        }
    }

    @Test
    void relief_isLighterAboveAndDarkerBelowTheGreyItStandsOn() {
        for (final CdeScheme scheme : CdeScheme.ALL) {
            final CdePalette palette = scheme.palette().declared();
            assertTrue(ColorContrast.luminance(palette.light()) > ColorContrast.luminance(palette.window()),
                    scheme.label() + ": the lit edge is not lighter than the grey");
            assertTrue(ColorContrast.luminance(palette.shade()) < ColorContrast.luminance(palette.window()),
                    scheme.label() + ": the shaded edge is not darker than the grey");
        }
    }

    @Test
    void activeColour_standsApartFromTheGreyOfAnIdleTitle() {
        for (final CdeScheme scheme : CdeScheme.ALL) {
            final CdePalette palette = scheme.palette().declared();
            assertTrue(ColorContrast.ratio(palette.active(), palette.window()) >= 1.5,
                    scheme.label() + ": the window in front would not read as in front");
        }
    }
}
