/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PanelShapeTest {

    @Test
    void of_joinsAFullRectangle() {
        final PanelShape shape = PanelShape.of(rectangle(-1, 0, 3, 2));
        assertEquals(new PanelShape(-1, 0, 3, 2), shape);
    }

    @Test
    void of_leavesOneMonitorAlone() {
        assertNull(PanelShape.of(Set.of(PanelShape.cell(0, 0))));
    }

    @Test
    void of_refusesAGroupWithAGap() {
        final Set<Long> cells = rectangle(0, 0, 3, 2);
        cells.remove(PanelShape.cell(1, 1));
        assertNull(PanelShape.of(cells));
    }

    @Test
    void of_refusesAnLShape() {
        final Set<Long> cells = rectangle(0, 0, 2, 1);
        cells.add(PanelShape.cell(0, 1));
        assertNull(PanelShape.of(cells));
    }

    @Test
    void of_takesTheLargestScreenAllowed() {
        assertEquals(new PanelShape(0, 0, 8, 6), PanelShape.of(rectangle(0, 0, 8, 6)));
    }

    @Test
    void of_refusesAScreenTooWideOrTooTall() {
        assertNull(PanelShape.of(rectangle(0, 0, 9, 1)), "nine wide is past the limit");
        assertNull(PanelShape.of(rectangle(0, 0, 1, 7)), "seven tall is past the limit");
        assertNull(PanelShape.of(rectangle(0, 0, 6, 8)), "six wide and eight tall is the limit turned over");
    }

    @Test
    void cell_packsNegativeOffsets() {
        final long cell = PanelShape.cell(-5, -3);
        assertEquals(-5, PanelShape.u(cell));
        assertEquals(-3, PanelShape.v(cell));
    }

    private static Set<Long> rectangle(final int left, final int bottom, final int width, final int height) {
        final Set<Long> cells = new HashSet<>();
        for (int u = 0; u < width; u++) {
            for (int v = 0; v < height; v++) {
                cells.add(PanelShape.cell(left + u, bottom + v));
            }
        }
        return cells;
    }
}
