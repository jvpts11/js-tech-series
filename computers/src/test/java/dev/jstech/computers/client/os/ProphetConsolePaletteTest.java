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

/** Every text the Prophet Reactive Console draws in its own colours stands out from what it is drawn on. */
class ProphetConsolePaletteTest {

    private static final double BODY = 4.5;
    private static final double SECONDARY = 3.0;

    @Test
    void colours_keepTheStatusBarAndTheGraphReadable() {
        final ProphetConsolePalette.Colours c = ProphetConsolePalette.PALETTE.declared();
        assertReadable(c.statusText(), c.status(), BODY);
        assertReadable(c.graphText(), c.graph(), BODY);
        assertReadable(c.graphDim(), c.graph(), BODY);
        assertReadable(c.level(), c.graph(), SECONDARY);
    }

    @Test
    void colours_keepTheChipsReadableOnTheGraphsGround() {
        final ProphetConsolePalette.Colours c = ProphetConsolePalette.PALETTE.declared();
        assertReadable(c.holding(), c.graph(), BODY);
        assertReadable(c.working(), c.graph(), BODY);
        assertReadable(c.trouble(), c.graph(), BODY);
        assertReadable(c.waiting(), c.graph(), BODY);
        assertReadable(c.over(), c.graph(), BODY);
    }

    private static void assertReadable(final int fg, final int bg, final double min) {
        final double ratio = ColorContrast.ratio(fg, bg);
        assertTrue(ratio >= min, String.format("contrast %.2f below %.1f for fg=%06X bg=%06X", ratio, min,
                fg & 0xFFFFFF, bg & 0xFFFFFF));
    }
}
