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
    void layout_endsTheCardsAboveTheChosenPartAndItsNoteAboveThePanelsFoot() {
        final int cards = ShipDesignLayout.CARD_COLUMNS * ShipDesignLayout.CARD_ROWS;
        assertTrue(ShipDesignLayout.cardY(cards - 1) + ShipDesignLayout.CARD_H < ShipDesignLayout.CHOSEN_Y);
        assertTrue(ShipDesignLayout.NOTE_Y + ShipDesignLayout.NOTE_ROWS * ShipDesignLayout.NOTE_STEP
                <= ShipDesignLayout.MAKER_LINE_Y);
        assertTrue(ShipDesignLayout.MAKER_LINE_Y + 2 * ShipDesignLayout.NOTE_STEP < ShipDesignLayout.PANEL_Y1);
        assertTrue(ShipDesignLayout.tabX(ShipDesignLayout.TABS - 1) + ShipDesignLayout.TAB_W
                <= ShipDesignLayout.SEARCH_X1);
    }

    @Test
    void layout_stacksTheRightPanelsBlocksOneUnderTheOther() {
        assertTrue(ShipDesignLayout.CHECK_Y0 + ShipDesignLayout.CHECK_ROWS * ShipDesignLayout.CHECK_STEP
                < ShipDesignLayout.SHIP_Y);
        assertTrue(ShipDesignLayout.shipY(ShipDesignLayout.SHIP_ROWS - 1) + ShipDesignLayout.FIGURE_STEP
                < ShipDesignLayout.ROOM_Y);
        assertTrue(ShipDesignLayout.ROOM_Y + 14 + ShipDesignLayout.SLOT_H + 12 < ShipDesignLayout.FINISH_Y);
        assertTrue(ShipDesignLayout.FINISH_Y + 13 + ShipDesignLayout.SWATCH_H < ShipDesignLayout.DOCK_Y);
    }

    @Test
    void onPanel_tellsThePanelsFromTheOpenMiddle() {
        assertTrue(ShipDesignLayout.onPanel(20, 200));
        assertTrue(ShipDesignLayout.onPanel(700, 200));
        assertTrue(ShipDesignLayout.onPanel(400, 20));
        assertTrue(ShipDesignLayout.onPanel(400, 460));
        assertFalse(ShipDesignLayout.onPanel(427, 240));
    }
}
