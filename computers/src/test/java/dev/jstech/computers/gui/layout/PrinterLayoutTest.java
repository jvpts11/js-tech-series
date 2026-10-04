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

class PrinterLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = PrinterLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void queue_fitsAboveTheOutput() {
        assertTrue(PrinterLayout.QUEUE_Y + PrinterLayout.QUEUE_H < PrinterLayout.OUT_Y);
    }

    @Test
    void window_fitsTheScreenBudget() {
        assertTrue(PrinterLayout.HEIGHT <= 256);
    }
}
