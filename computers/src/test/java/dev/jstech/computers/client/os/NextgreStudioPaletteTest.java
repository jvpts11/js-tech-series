/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.ColorContrast;
import org.junit.jupiter.api.Test;

/** Every text the Nextgre Planner Studio draws in its own colours stands out from what it is drawn on. */
class NextgreStudioPaletteTest {

    private static final double BODY = 4.5;
    private static final double SECONDARY = 3.0;

    @Test
    void colours_keepTheBoxesReadable() {
        final NextgreStudioPalette.Colours c = NextgreStudioPalette.PALETTE.declared();
        assertReadable(c.boxText(), c.box(), BODY);
        assertReadable(c.boxDim(), c.box(), BODY);
        assertReadable(c.running(), c.box(), SECONDARY);
        assertReadable(c.good(), c.box(), SECONDARY);
        assertReadable(c.bad(), c.box(), SECONDARY);
        assertReadable(c.noteText(), c.note(), BODY);
    }

    @Test
    void colours_keepTheBarsAndChipsReadable() {
        final NextgreStudioPalette.Colours c = NextgreStudioPalette.PALETTE.declared();
        assertReadable(c.statusText(), c.status(), BODY);
        assertReadable(c.chipText(), c.chip(), BODY);
        assertReadable(c.boxText(), c.chosen(), BODY);
    }

    private static void assertReadable(final int fg, final int bg, final double min) {
        final double ratio = ColorContrast.ratio(fg, bg);
        assertTrue(ratio >= min, String.format("contrast %.2f below %.1f for fg=%06X bg=%06X", ratio, min,
                fg & 0xFFFFFF, bg & 0xFFFFFF));
    }
}
