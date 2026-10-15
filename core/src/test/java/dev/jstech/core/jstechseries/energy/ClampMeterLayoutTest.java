/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class ClampMeterLayoutTest {

    @Test
    void layout_keepsEveryPieceApartAndInside() {
        final GuiLayout layout = ClampMeterLayout.layout();

        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void lines_fitTheDisplay() {
        assertTrue(ClampMeterLayout.MOST_LEAST_Y + 8 <= ClampMeterLayout.DISPLAY_BOTTOM);
        assertTrue(ClampMeterLayout.FACES_Y + 5 * ClampMeterLayout.FACE_PITCH + 8 <= ClampMeterLayout.DISPLAY_BOTTOM);
        assertTrue(ClampMeterLayout.DISPLAY_TOP + ClampMeterLayout.HEADER_HEIGHT < ClampMeterLayout.VALUE_Y);
    }

    @Test
    void onKey_andOnDial_findWhatIsClicked() {
        assertTrue(ClampMeterLayout.onKey(ClampMeterLayout.HOLD_X + 3, ClampMeterLayout.HOLD_Y + 3,
                ClampMeterLayout.HOLD_X, ClampMeterLayout.HOLD_Y));
        assertFalse(ClampMeterLayout.onKey(ClampMeterLayout.SHOW_X - 1, ClampMeterLayout.SHOW_Y,
                ClampMeterLayout.SHOW_X, ClampMeterLayout.SHOW_Y));
        assertTrue(ClampMeterLayout.onDial(ClampMeterLayout.DIAL_X + 5, ClampMeterLayout.DIAL_Y - 5));
        assertFalse(ClampMeterLayout.onDial(ClampMeterLayout.DIAL_X + 20, ClampMeterLayout.DIAL_Y));
    }
}
