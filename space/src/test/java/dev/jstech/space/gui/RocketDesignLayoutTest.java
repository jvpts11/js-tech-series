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
    void layout_endsTheRightPlatesLastKeyAboveItsBottomEdge() {
        final int last = RocketDesignLayout.KEY_Y0 + (RocketDesignLayout.KEYS - 1) * RocketDesignLayout.KEY_STEP;
        assertTrue(last + GuiLayout.LINE_HEIGHT < RocketDesignLayout.PLATE_Y1);
        assertTrue(RocketDesignLayout.cardY(RocketDesignLayout.CARD_COLUMNS * RocketDesignLayout.CARD_ROWS - 1)
                + RocketDesignLayout.CARD_H < RocketDesignLayout.SHEET_Y0);
    }

    @Test
    void onPlate_tellsThePlatesFromTheOpenMiddle() {
        assertTrue(RocketDesignLayout.onPlate(20, 200));
        assertTrue(RocketDesignLayout.onPlate(700, 200));
        assertTrue(RocketDesignLayout.onPlate(400, 20));
        assertTrue(RocketDesignLayout.onPlate(400, 460));
        assertFalse(RocketDesignLayout.onPlate(427, 240));
    }

    @Test
    void buttonX_setsTheButtonsSideBySideWithTheirGaps() {
        assertEquals(RocketDesignLayout.CONFIRM_X0, RocketDesignLayout.buttonX(0));
        assertEquals(RocketDesignLayout.buttonX(0) + RocketDesignLayout.CONFIRM_W + RocketDesignLayout.BUTTON_GAP,
                RocketDesignLayout.buttonX(1));
        assertTrue(RocketDesignLayout.buttonX(3) + RocketDesignLayout.buttonW(3) < RocketDesignLayout.HINT_X1);
    }
}
