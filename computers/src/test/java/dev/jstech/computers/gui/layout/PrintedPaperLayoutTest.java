/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class PrintedPaperLayoutTest {

    @Test
    void portrait_holdsAPageOfPrint() {
        for (final boolean fanfold : new boolean[] {false, true}) {
            final GuiLayout l = PrintedPaperLayout.layout(PrintedPaperLayout.portrait(fanfold),
                    PrintLayout.PORTRAIT_COLUMNS, PrintLayout.PORTRAIT_LINES);
            assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void landscape_holdsAPageOfPrint() {
        for (final boolean fanfold : new boolean[] {false, true}) {
            final GuiLayout l = PrintedPaperLayout.layout(PrintedPaperLayout.landscape(fanfold),
                    PrintLayout.LANDSCAPE_COLUMNS, PrintLayout.LANDSCAPE_LINES);
            assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void fanfold_isWiderForItsTractorStrips() {
        assertTrue(PrintedPaperLayout.portrait(true).width() > PrintedPaperLayout.portrait(false).width());
    }
}
