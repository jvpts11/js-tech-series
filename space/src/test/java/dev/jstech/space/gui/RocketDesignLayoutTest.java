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

class RocketDesignLayoutTest {

    @Test
    void layout_keepsEveryPlateAndLineInsideTheFrameAndThePlatesApart() {
        final GuiLayout layout = RocketDesignLayout.layout();
        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
        assertTrue(layout.isClean());
    }

    @Test
    void layout_isTheGameInterfaceAtNineteenTwentyByTenEighty() {
        assertEquals(1920 / 4, RocketDesignLayout.WIDTH);
        assertEquals(1080 / 4, RocketDesignLayout.HEIGHT);
    }

    @Test
    void layout_endsTheBinsAboveTheSheetAndTheSheetsRowsAboveItsNote() {
        assertTrue(RocketDesignLayout.cardY(RocketDesignLayout.CARD_COLUMNS * RocketDesignLayout.CARD_ROWS - 1)
                + RocketDesignLayout.CARD_H < RocketDesignLayout.MORE_Y);
        assertTrue(RocketDesignLayout.MORE_Y + 8 <= RocketDesignLayout.SHEET_Y0);
        assertTrue(RocketDesignLayout.SHEET_ROW_Y0 + RocketDesignLayout.SHEET_ROWS * RocketDesignLayout.SHEET_ROW_STEP
                <= RocketDesignLayout.SHEET_Y1 - 8);
    }

    @Test
    void layout_keepsTheChecksAndTheSignAboveTheRightPlatesFoot() {
        assertTrue(RocketDesignLayout.CHECK_Y0 + RocketDesignLayout.CHECK_LINES * RocketDesignLayout.CHECK_LINE + 2
                + RocketDesignLayout.STATUS_H < RocketDesignLayout.PLATE_Y1 - 4);
        assertTrue(RocketDesignLayout.REACH_Y0 + RocketDesignLayout.REACHES * RocketDesignLayout.REACH_STEP
                <= RocketDesignLayout.CHECKS_Y);
    }

    @Test
    void onPlate_tellsThePlatesFromTheOpenMiddle() {
        assertTrue(RocketDesignLayout.onPlate(20, 120));
        assertTrue(RocketDesignLayout.onPlate(400, 120));
        assertTrue(RocketDesignLayout.onPlate(240, 20));
        assertTrue(RocketDesignLayout.onPlate(240, 260));
        assertTrue(RocketDesignLayout.onPlate(240, 245));
        assertFalse(RocketDesignLayout.onPlate(240, 135));
    }

    @Test
    void buttonX_setsTheButtonsSideBySideWithTheirGaps() {
        assertEquals(RocketDesignLayout.CONFIRM_X0, RocketDesignLayout.buttonX(0));
        assertEquals(RocketDesignLayout.buttonX(0) + RocketDesignLayout.CONFIRM_W + RocketDesignLayout.BUTTON_GAP,
                RocketDesignLayout.buttonX(1));
        assertTrue(RocketDesignLayout.buttonX(3) + RocketDesignLayout.buttonW(3) < RocketDesignLayout.MIDDLE_X1);
    }
}
