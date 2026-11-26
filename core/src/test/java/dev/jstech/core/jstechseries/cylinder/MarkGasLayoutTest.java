/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class MarkGasLayoutTest {

    @Test
    void layout_keepsEveryPieceApartAndInside() {
        final GuiLayout layout = MarkGasLayout.layout();

        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void rows_fitInsideTheList() {
        final int last = MarkGasLayout.ROW_Y + MarkGasLayout.ROWS * MarkGasLayout.ROW_PITCH;

        assertTrue(last <= MarkGasLayout.LIST_Y + MarkGasLayout.LIST_HEIGHT, "the last row ends at " + last);
    }

    @Test
    void rowAt_findsEachRowAndNothingOutsideTheList() {
        for (int row = 0; row < MarkGasLayout.ROWS; row++) {
            assertEquals(row, MarkGasLayout.rowAt(MarkGasLayout.NAME_X, MarkGasLayout.ROW_Y + row
                    * MarkGasLayout.ROW_PITCH + 8));
        }
        assertEquals(-1, MarkGasLayout.rowAt(MarkGasLayout.NAME_X, MarkGasLayout.SEARCH_Y + 4));
        assertEquals(-1, MarkGasLayout.rowAt(MarkGasLayout.NAME_X, MarkGasLayout.BUTTON_Y + 4));
        assertEquals(-1, MarkGasLayout.rowAt(2, MarkGasLayout.ROW_Y + 8));
    }
}
