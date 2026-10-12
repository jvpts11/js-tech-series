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

class CrashLayoutTest {

    /** The stop page's code: version 3, the size the text it carries comes out at. */
    private static final int MODULES = 29;

    @Test
    void layout_isCleanWithTheLongestSentence() {
        final GuiLayout l = CrashLayout.layout(MODULES);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void frames95_isCleanWithAWideTitle() {
        final GuiLayout l = CrashLayout.frames95(60);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void qrSide_keepsTheLightMarginAReaderNeeds() {
        assertTrue(CrashLayout.QR_QUIET >= 2);
        assertTrue(CrashLayout.qrSide(MODULES) == (MODULES + 4) * CrashLayout.QR_MODULE);
    }

    @Test
    void consoleRows_fillTheGlass() {
        assertTrue(CrashLayout.consoleRows() * CrashLayout.ROW <= CrashLayout.HEIGHT - 2 * CrashLayout.CONSOLE_MARGIN);
        assertTrue(CrashLayout.consoleRows() >= 25, "a console holds at least the 25 rows of a text screen");
    }
}
