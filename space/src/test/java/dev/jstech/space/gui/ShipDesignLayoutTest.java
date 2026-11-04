/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShipDesignLayoutTest {

    @Test
    void layout_keepsEveryPanelAndLineInsideTheFrameAndThePanelsApart() {
        final GuiLayout layout = ShipDesignLayout.layout();
        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
        assertTrue(layout.isClean());
    }

    @Test
    void layout_isTheGameInterfaceAtNineteenTwentyByTenEighty() {
        assertEquals(1920 / 4, ShipDesignLayout.WIDTH);
        assertEquals(1080 / 4, ShipDesignLayout.HEIGHT);
    }

    @Test
    void layout_endsTheCardsAboveTheChosenPartAndItsNoteAboveThePanelsFoot() {
        final int cards = ShipDesignLayout.CARD_COLUMNS * ShipDesignLayout.CARD_ROWS;
        assertTrue(ShipDesignLayout.cardY(cards - 1) + ShipDesignLayout.CARD_H < ShipDesignLayout.MORE_Y);
        assertTrue(ShipDesignLayout.MORE_Y + ShipDesignLayout.NOTE_STEP <= ShipDesignLayout.CHOSEN_Y);
        assertTrue(ShipDesignLayout.figureY(ShipDesignLayout.FIGURE_ROWS - 1) + ShipDesignLayout.FIGURE_STEP
                <= ShipDesignLayout.NOTE_Y);
        assertTrue(ShipDesignLayout.NOTE_Y + ShipDesignLayout.NOTE_ROWS * ShipDesignLayout.NOTE_STEP
                <= ShipDesignLayout.PANEL_Y1);
        assertTrue(ShipDesignLayout.tabX(ShipDesignLayout.TABS - 1) + ShipDesignLayout.TAB_W
                <= ShipDesignLayout.SEARCH_X1);
        assertTrue(ShipDesignLayout.cardX(1) + ShipDesignLayout.CARD_W <= ShipDesignLayout.SEARCH_X1);
    }

    @Test
    void layout_stacksTheRightPanelsBlocksOneUnderTheOther() {
        assertTrue(ShipDesignLayout.CHECK_Y0 + ShipDesignLayout.CHECK_ROWS * ShipDesignLayout.CHECK_STEP
                < ShipDesignLayout.SHIP_Y);
        assertTrue(ShipDesignLayout.shipY(ShipDesignLayout.SHIP_ROWS - 1) + ShipDesignLayout.SHIP_STEP
                <= ShipDesignLayout.DOCK_Y);
        assertTrue(ShipDesignLayout.DOCK_Y + 36 + 9 < ShipDesignLayout.PANEL_Y1);
    }

    @Test
    void layout_keepsTheToolsAndTheChosenOnesNameInTheTop() {
        assertTrue(ShipDesignLayout.toolX(ShipDesignLayout.TOOLS - 1) + ShipDesignLayout.TOOL_W
                < ShipDesignLayout.MIDDLE_X1);
        assertTrue(ShipDesignLayout.TOOLS_Y0 + ShipDesignLayout.TOOL_H < ShipDesignLayout.TOP_Y1);
        assertTrue(ShipDesignLayout.TAG_Y1 < ShipDesignLayout.TOOLS_Y0);
    }

    @Test
    void onPanel_tellsThePanelsFromTheOpenMiddle() {
        assertTrue(ShipDesignLayout.onPanel(20, 120));
        assertTrue(ShipDesignLayout.onPanel(400, 120));
        assertTrue(ShipDesignLayout.onPanel(240, 20));
        assertTrue(ShipDesignLayout.onPanel(240, 260));
        assertFalse(ShipDesignLayout.onPanel(240, 135));
    }
}
