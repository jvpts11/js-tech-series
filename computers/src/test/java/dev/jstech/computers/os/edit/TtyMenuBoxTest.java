/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TtyMenuBoxTest {

    private static final int CELL = 6;
    private static final int TEXT_ROW = 9;
    private static final int PAD = 3;
    private static final int ITEM_ROW = 11;

    @Test
    void of_startsAtColumnTwentyFourOneTextRowDown() {
        final TtyMenuBox box = TtyMenuBox.of(384, 200, CELL, TEXT_ROW, PAD, ITEM_ROW, 190, 6, 0);
        assertEquals(24 * CELL, box.x());
        assertEquals(TEXT_ROW, box.y());
    }

    @Test
    void of_clampsTheColumnSoTheBoxNeverRunsPastANarrowArea() {
        final TtyMenuBox box = TtyMenuBox.of(120, 200, CELL, TEXT_ROW, PAD, ITEM_ROW, 100, 6, 0);
        assertTrue(box.x() + box.width() <= 120, "the box must stay inside the area it is drawn over");
    }

    @Test
    void of_neverTallerThanTheAreaLeftBelowOneTextRow() {
        final TtyMenuBox box = TtyMenuBox.of(384, 40, CELL, TEXT_ROW, PAD, ITEM_ROW, 190, 6, 0);
        assertTrue(box.y() + box.height() <= 40, "a short desktop terminal must not have the menu spill past it");
    }

    @Test
    void of_showsAtLeastOneRowEvenWhenTheAreaIsTiny() {
        final TtyMenuBox box = TtyMenuBox.of(200, 20, CELL, TEXT_ROW, PAD, ITEM_ROW, 190, 6, 0);
        assertTrue(box.visibleRows() >= 1);
    }

    /** The smallest terminal, with the longest pt_br item name, still keeps the chosen row in view. */
    @Test
    void of_scrollsTheSelectedRowIntoViewWhenNotEveryItemFits() {
        final TtyMenuBox box = TtyMenuBox.of(200, 60, CELL, TEXT_ROW, PAD, ITEM_ROW, 190, 6, 5);
        assertTrue(box.visibleRows() < 6, "the case worth testing is one where not every item already fits");
        assertTrue(5 >= box.topRow() && 5 < box.topRow() + box.visibleRows(),
                "the selected row must be among the ones shown");
    }

    /** The last item chosen: the window must end exactly on it, never scrolled past what there is to show. */
    @Test
    void of_neverScrollsPastTheLastItem() {
        final TtyMenuBox box = TtyMenuBox.of(200, 60, CELL, TEXT_ROW, PAD, ITEM_ROW, 190, 6, 5);
        assertEquals(6, box.topRow() + box.visibleRows(),
                "the window must end exactly on the last item, not before or past it");
    }
}
