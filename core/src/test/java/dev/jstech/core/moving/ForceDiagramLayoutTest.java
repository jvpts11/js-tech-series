/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class ForceDiagramLayoutTest {

    @Test
    void layout_keepsEveryPartApartAndInside() {
        final GuiLayout layout = ForceDiagramLayout.layout();
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void inside_keepsTheButtonsAndRowsApartAndInside() {
        final GuiLayout layout = ForceDiagramLayout.inside();
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void rowAt_findsEachRowAndNoneBetweenThem() {
        final int x = ForceDiagramLayout.LIST_X + 10;
        for (int row = 0; row < ForceDiagramLayout.ROWS; row++) {
            assertEquals(row, ForceDiagramLayout.rowAt(x, ForceDiagramLayout.rowY(row) + 1));
        }
        assertEquals(-1, ForceDiagramLayout.rowAt(x, ForceDiagramLayout.rowY(0) + ForceDiagramLayout.ROW_HEIGHT));
        assertEquals(-1, ForceDiagramLayout.rowAt(ForceDiagramLayout.VIEW_X + 5, ForceDiagramLayout.rowY(0) + 1));
    }

    @Test
    void rows_fitTheList() {
        final int last = ForceDiagramLayout.rowY(ForceDiagramLayout.ROWS - 1) + ForceDiagramLayout.ROW_HEIGHT;
        assertTrue(last <= ForceDiagramLayout.LIST_Y + ForceDiagramLayout.LIST_HEIGHT, "the last row ends at " + last);
    }

    @Test
    void onView_isTheViewAlone() {
        assertTrue(ForceDiagramLayout.onView(ForceDiagramLayout.VIEW_X + 1, ForceDiagramLayout.VIEW_Y + 1));
        assertFalse(ForceDiagramLayout.onView(ForceDiagramLayout.LIST_X + 1, ForceDiagramLayout.LIST_Y + 1));
    }
}
