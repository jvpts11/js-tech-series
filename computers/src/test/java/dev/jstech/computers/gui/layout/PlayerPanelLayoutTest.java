/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class PlayerPanelLayoutTest {

    /* The smallest desktop a monitor shows, its panel a taskbar tall, and a menu as full as a small one gets. */
    private static final int WIDTH = 320;
    private static final int HEIGHT = 200;
    private static final int BAR_H = 24;
    private static final int PROGRAMS = 8;
    private static final int WIDEST = 16;
    private static final int CLOCK = 8;

    @Test
    void layout_isCleanWithThePanelAlongTheTop() {
        final GuiLayout layout = PlayerPanelLayout.layout(WIDTH, HEIGHT, BAR_H, true, PROGRAMS, WIDEST, CLOCK);
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanWithThePanelAlongTheFoot() {
        final GuiLayout layout = PlayerPanelLayout.layout(WIDTH, HEIGHT, BAR_H, false, PROGRAMS, WIDEST, CLOCK);
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void rowAt_findsEachProgramAndShutDown() {
        final int menuY = 25;
        assertEquals(0, PlayerPanelLayout.rowAt(menuY, 3, PlayerPanelLayout.rowY(menuY, 0)));
        assertEquals(2, PlayerPanelLayout.rowAt(menuY, 3, PlayerPanelLayout.rowY(menuY, 2) + 12));
        assertEquals(3, PlayerPanelLayout.rowAt(menuY, 3, PlayerPanelLayout.shutDownY(menuY, 3)));
    }

    @Test
    void rowAt_findsNothingOnTheRuleOrTheRoomAround() {
        final int menuY = 25;
        assertEquals(-1, PlayerPanelLayout.rowAt(menuY, 3, menuY));
        assertEquals(-1, PlayerPanelLayout.rowAt(menuY, 3, PlayerPanelLayout.rowY(menuY, 3) + 1));
        assertEquals(-1, PlayerPanelLayout.rowAt(menuY, 3, PlayerPanelLayout.shutDownY(menuY, 3)
                + PlayerPanelLayout.ROW_H));
    }

    @Test
    void menuWidth_neverNarrowerThanItsLeast() {
        assertEquals(PlayerPanelLayout.MENU_MIN_W, PlayerPanelLayout.menuWidth(1));
        assertTrue(PlayerPanelLayout.menuWidth(200) > 200);
    }
}
