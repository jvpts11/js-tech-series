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

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class SoftwareManagerLayoutTest {

    @Test
    void front_holdsTenCategoriesAndThreeNewArrivalsWithoutAnOverlap() {
        assertClean(SoftwareManagerLayout.front());
    }

    @Test
    void page_hasRoomForItsFactsAndWhatItDoes() {
        assertClean(SoftwareManagerLayout.page());
    }

    @Test
    void list_fitsEveryRowAboveTheWindowsFoot() {
        assertClean(SoftwareManagerLayout.list());
        assertTrue(SoftwareManagerLayout.row(SoftwareManagerLayout.ROWS - 1).bottom() <= SoftwareManagerLayout.HEIGHT);
    }

    @Test
    void category_laysFiveToARowAndTheSixthUnderTheFirst() {
        assertEquals(SoftwareManagerLayout.category(0).x(), SoftwareManagerLayout.category(5).x());
        assertTrue(SoftwareManagerLayout.category(5).y() > SoftwareManagerLayout.category(4).bottom());
        assertTrue(SoftwareManagerLayout.category(4).right() <= SoftwareManagerLayout.FEATURED.right());
    }

    @Test
    void rowAt_findsTheRowUnderAPointAndNoneOutsideTheList() {
        final SoftwareManagerLayout.Rect third = SoftwareManagerLayout.row(2);
        assertEquals(2, SoftwareManagerLayout.rowAt(third.x() + 4, third.y() + 2));
        assertEquals(-1, SoftwareManagerLayout.rowAt(third.x() + 4, SoftwareManagerLayout.BAR_H - 2));
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
