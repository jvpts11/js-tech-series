/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemPortLayoutTest {

    @Test
    void layout_keepsTheDisplayTheHoldAndTheInventoryApartAndInsideTheScreen() {
        final GuiLayout layout = ItemPortLayout.layout();
        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
        assertTrue(layout.isClean());
    }

    @Test
    void display_showsFourRowsOfTheList() {
        assertEquals(4, ItemPortLayout.ROWS_SHOWN);
        assertTrue(ItemPortLayout.LIST_Y + ItemPortLayout.ROWS_SHOWN * ItemPortLayout.ROW <= ItemPortLayout.DISPLAY_H);
    }

    @Test
    void place_laysTheHoldInTwoRowsOfNineUnderTheDisplay() {
        assertArrayEquals(new int[] {8, 119}, ItemPortLayout.place(0));
        assertArrayEquals(new int[] {152, 137}, ItemPortLayout.place(17));
        assertTrue(ItemPortLayout.DISPLAY_Y + ItemPortLayout.DISPLAY_H < ItemPortLayout.HOLD_LABEL_Y);
        assertTrue(ItemPortLayout.place(17)[1] + 18 <= ItemPortLayout.INVENTORY_LABEL_Y);
    }
}
