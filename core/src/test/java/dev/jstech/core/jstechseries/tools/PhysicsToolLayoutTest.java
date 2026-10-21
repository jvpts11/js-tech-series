/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class PhysicsToolLayoutTest {

    @Test
    void layout_keepsEveryPartApartAndInside() {
        final GuiLayout layout = PhysicsToolLayout.layout();
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void knobX_runsFromTheSliderStartToItsEnd() {
        assertEquals(PhysicsToolLayout.SLIDER_X, PhysicsToolLayout.knobX(0));
        assertEquals(PhysicsToolLayout.SLIDER_X + PhysicsToolLayout.SLIDER_WIDTH - PhysicsToolLayout.KNOB_WIDTH,
                PhysicsToolLayout.knobX(100));
        assertEquals(PhysicsToolLayout.knobX(100), PhysicsToolLayout.knobX(250));
    }

    @Test
    void top_standsThePanelAboveTheFoot() {
        assertEquals(240 - PhysicsToolLayout.ABOVE_FOOT - PhysicsToolLayout.HEIGHT, PhysicsToolLayout.top(240));
    }
}
