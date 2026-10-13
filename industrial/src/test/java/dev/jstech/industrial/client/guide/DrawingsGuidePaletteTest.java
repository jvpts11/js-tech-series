/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.gui.ColorContrast;
import org.junit.jupiter.api.Test;

class DrawingsGuidePaletteTest {

    /** What body text has to reach against its ground. */
    private static final double BODY = 4.5;
    /** What secondary text (page heads and feet, dots, passing remarks) has to reach. */
    private static final double SECONDARY = 3.0;

    private static final GuidePalettes.Binder DRAWINGS = DrawingsGuidePalette.DRAWINGS.declared();

    @Test
    void ink_readsOnThePaper() {
        final double ratio = ColorContrast.ratio(DRAWINGS.ink(), DRAWINGS.paper());
        assertTrue(ratio >= BODY, "ink reads at " + ratio + " on the paper");
    }

    @Test
    void faint_readsOnThePaper() {
        final double ratio = ColorContrast.ratio(DRAWINGS.faint(), DRAWINGS.paper());
        assertTrue(ratio >= SECONDARY, "faint reads at " + ratio + " on the paper");
    }

    @Test
    void warning_readsOnThePaper() {
        final double ratio = ColorContrast.ratio(DRAWINGS.warning(), DRAWINGS.paper());
        assertTrue(ratio >= BODY, "warning reads at " + ratio + " on the paper");
    }
}
