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

class DockLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = DockLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void rows_endBeforeTheNote() {
        assertTrue(DockLayout.rowY(DockLayout.ROWS) <= DockLayout.NOTE_Y);
        assertTrue(DockLayout.NOTE_Y + 16 <= DockLayout.INV_LABEL_Y);
    }

    @Test
    void window_fitsTheScreenBudget() {
        assertTrue(DockLayout.HEIGHT <= 256);
    }
}
