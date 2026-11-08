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

class DiskCheckLayoutTest {

    @Test
    void layout_scanDiskIsCleanOnTheGlass() {
        final GuiLayout l = DiskCheckLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void layout_barSitsBetweenTheListAndTheButtons() {
        assertTrue(DiskCheckLayout.BAR_Y > DiskCheckLayout.AREAS_Y + DiskCheckLayout.AREAS * DiskCheckLayout.ROW);
        assertTrue(DiskCheckLayout.BAR_Y + DiskCheckLayout.BAR_H < DiskCheckLayout.BUTTONS_Y);
    }

    @Test
    void consoleRows_fillTheGlass() {
        assertTrue(DiskCheckLayout.consoleRows() * DiskCheckLayout.ROW
                <= DiskCheckLayout.HEIGHT - 2 * DiskCheckLayout.CONSOLE_MARGIN);
        assertTrue(DiskCheckLayout.consoleRows() >= 25, "a console holds at least the 25 rows of a text screen");
    }
}
