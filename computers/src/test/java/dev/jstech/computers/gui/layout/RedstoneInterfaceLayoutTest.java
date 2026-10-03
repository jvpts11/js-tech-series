/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedstoneInterfaceLayoutTest {

    @Test
    void layout_nothingOverlapsOrLeavesTheScreen() {
        final GuiLayout l = RedstoneInterfaceLayout.layout();
        assertTrue(l.overlaps().isEmpty(), () -> "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), () -> "out of bounds: " + l.outOfBounds());
        assertTrue(l.isClean());
    }

    @Test
    void cellAt_findsEachStrengthAndNothingBetweenOrBeside() {
        for (int strength = 0; strength < RedstoneInterfaceLayout.CELLS; strength++) {
            assertEquals(strength, RedstoneInterfaceLayout.cellAt(RedstoneInterfaceLayout.cellX(strength) + 4,
                    RedstoneInterfaceLayout.CELLS_Y + 5));
        }
        // The gap after a cell, the space past the last, and the rows above and below are no cell.
        assertEquals(-1, RedstoneInterfaceLayout.cellAt(RedstoneInterfaceLayout.cellX(3)
                + RedstoneInterfaceLayout.CELL_W, RedstoneInterfaceLayout.CELLS_Y + 5));
        assertEquals(-1, RedstoneInterfaceLayout.cellAt(RedstoneInterfaceLayout.NUMBER_X,
                RedstoneInterfaceLayout.CELLS_Y + 5));
        assertEquals(-1, RedstoneInterfaceLayout.cellAt(RedstoneInterfaceLayout.cellX(5),
                RedstoneInterfaceLayout.CELLS_Y - 1));
        assertEquals(-1, RedstoneInterfaceLayout.cellAt(RedstoneInterfaceLayout.cellX(5),
                RedstoneInterfaceLayout.CELLS_Y + RedstoneInterfaceLayout.CELL_H));
    }

    @Test
    void cells_endBeforeTheNumber() {
        assertTrue(RedstoneInterfaceLayout.cellX(RedstoneInterfaceLayout.CELLS - 1) + RedstoneInterfaceLayout.CELL_W
                < RedstoneInterfaceLayout.NUMBER_X);
    }
}
