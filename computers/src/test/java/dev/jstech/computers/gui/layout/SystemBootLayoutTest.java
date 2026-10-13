/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class SystemBootLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = SystemBootLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void bar_standsClearOfTheGlassEdges() {
        assertTrue(SystemBootLayout.barX() > 0);
        assertTrue(SystemBootLayout.barX() + SystemBootLayout.BAR_W < SystemBootLayout.WIDTH);
        assertTrue(SystemBootLayout.barY() + SystemBootLayout.BAR_H < SystemBootLayout.HEIGHT);
    }
}
