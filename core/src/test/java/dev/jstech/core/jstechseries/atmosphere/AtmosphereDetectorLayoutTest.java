/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class AtmosphereDetectorLayoutTest {

    @Test
    void layout_hasNothingOverlappingNorOutside() {
        final GuiLayout layout = AtmosphereDetectorLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void onShow_answersWhereTheKeyIsDrawn() {
        assertTrue(AtmosphereDetectorLayout.onShow(AtmosphereDetectorLayout.SHOW_X + 2,
                AtmosphereDetectorLayout.SHOW_Y + 2));
        assertFalse(AtmosphereDetectorLayout.onShow(AtmosphereDetectorLayout.SHOW_X - 1,
                AtmosphereDetectorLayout.SHOW_Y + 2));
    }
}
