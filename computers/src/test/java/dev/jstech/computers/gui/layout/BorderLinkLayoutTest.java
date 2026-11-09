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

class BorderLinkLayoutTest {

    @Test
    void layout_isCleanWithTheStatusBarAndWithout() {
        for (final boolean statusBar : new boolean[] {false, true}) {
            final GuiLayout layout = BorderLinkLayout.layout(statusBar);
            assertTrue(layout.isClean(), statusBar + ": " + layout.overlaps() + " " + layout.outOfBounds());
        }
    }

    @Test
    void layout_keepsTheButtonsClearOfTheSwitches() {
        assertTrue(BorderLinkLayout.SWITCHES_Y + BorderLinkLayout.CHECK_H + 5 <= BorderLinkLayout.BUTTONS_Y);
        assertTrue(BorderLinkLayout.LIST_Y + BorderLinkLayout.LIST_H < BorderLinkLayout.DETAIL_Y);
    }
}
