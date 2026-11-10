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

class PlayerPagesLayoutTest {

    @Test
    void boot_isClean() {
        final GuiLayout layout = PlayerPagesLayout.boot();
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void panic_isCleanWithTheLongestTrace() {
        final GuiLayout layout = PlayerPagesLayout.panic();
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void panic_endsTheTraceAboveTheFoot() {
        final int lastRow = PlayerPagesLayout.TRACE_Y + PlayerPagesLayout.MOST_TRACE_ROWS * PlayerPagesLayout.ROW;
        assertTrue(lastRow < PlayerPagesLayout.FOOT_Y, "the trace's last row ends above the restart line");
    }

    @Test
    void boot_putsTheBarUnderTheLogoAndTheLineUnderTheBar() {
        assertTrue(PlayerPagesLayout.BAR_Y > PlayerPagesLayout.LOGO_Y + PlayerPagesLayout.LOGO);
        assertTrue(PlayerPagesLayout.LINE_Y > PlayerPagesLayout.BAR_Y + PlayerPagesLayout.BAR_H);
    }
}
