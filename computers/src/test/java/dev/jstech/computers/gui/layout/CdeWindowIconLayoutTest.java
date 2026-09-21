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

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CdeWindowIconLayoutTest {

    private static final int WIDTH = 512;
    private static final int HEIGHT = 291;

    @Test
    void layout_isCleanWithMoreIconsThanOneRowHolds() {
        final int count = CdeWindowIconLayout.perRow(WIDTH) * 2 + 1;
        final GuiLayout l = CdeWindowIconLayout.layout(count, WIDTH, HEIGHT);
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }

    @Test
    void cell_startsAtTheTopLeftAndRunsToTheRight() {
        final Rect first = CdeWindowIconLayout.cell(0, WIDTH, 0);
        final Rect second = CdeWindowIconLayout.cell(1, WIDTH, 0);
        assertTrue(first.x() < WIDTH / 8 && first.y() < HEIGHT / 8, "the first icon stands at the top left");
        assertEquals(first.y(), second.y());
        assertTrue(second.x() >= first.x() + first.w());
    }

    @Test
    void cell_startsAgainUnderneathWhenTheRowIsFull() {
        final int perRow = CdeWindowIconLayout.perRow(WIDTH);
        final Rect first = CdeWindowIconLayout.cell(0, WIDTH, 0);
        final Rect wrapped = CdeWindowIconLayout.cell(perRow, WIDTH, 0);
        assertEquals(first.x(), wrapped.x());
        assertTrue(wrapped.y() >= first.y() + first.h());
    }

    @Test
    void cell_followsTheTopOfTheWorkArea() {
        assertEquals(CdeWindowIconLayout.cell(0, WIDTH, 0).y() + 24, CdeWindowIconLayout.cell(0, WIDTH, 24).y());
    }

    @Test
    void tileAndName_standInsideTheirCellWithTheTileAbove() {
        final Rect cell = CdeWindowIconLayout.cell(3, WIDTH, 0);
        final Rect tile = CdeWindowIconLayout.tile(3, WIDTH, 0);
        final Rect name = CdeWindowIconLayout.name(3, WIDTH, 0);
        assertTrue(tile.x() >= cell.x() && tile.x() + tile.w() <= cell.x() + cell.w());
        assertEquals(cell.y(), tile.y());
        assertTrue(name.y() >= tile.y() + tile.h());
        assertEquals(cell.y() + cell.h(), name.y() + name.h());
    }

    @Test
    void indexAt_findsEveryIconAtTheMiddleOfItsTile() {
        final int count = CdeWindowIconLayout.perRow(WIDTH) + 3;
        for (int i = 0; i < count; i++) {
            final Rect tile = CdeWindowIconLayout.tile(i, WIDTH, 24);
            assertEquals(i, CdeWindowIconLayout.indexAt(tile.x() + tile.w() / 2.0, tile.y() + tile.h() / 2.0,
                    count, WIDTH, 24));
        }
    }

    @Test
    void indexAt_isNoIconOffTheGridOrPastTheLastOne() {
        final Rect first = CdeWindowIconLayout.cell(0, WIDTH, 0);
        final Rect second = CdeWindowIconLayout.cell(1, WIDTH, 0);
        assertEquals(-1, CdeWindowIconLayout.indexAt(first.x() - 1, first.y() + 2, 2, WIDTH, 0));
        assertEquals(-1, CdeWindowIconLayout.indexAt(first.x() + 2, first.y() - 1, 2, WIDTH, 0));
        assertEquals(-1, CdeWindowIconLayout.indexAt(first.x() + 2, first.y() + first.h() + 1, 2, WIDTH, 0));
        assertEquals(-1, CdeWindowIconLayout.indexAt(second.x() + 2, second.y() + 2, 1, WIDTH, 0));
    }

    @Test
    void perRow_isNeverLessThanOne() {
        assertEquals(1, CdeWindowIconLayout.perRow(10));
    }
}
