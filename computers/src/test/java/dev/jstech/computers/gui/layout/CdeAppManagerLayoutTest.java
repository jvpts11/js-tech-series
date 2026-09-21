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

class CdeAppManagerLayoutTest {

    /** The content of the window of the groups and of the window of one group, at the sizes they open at. */
    private static final int GROUPS_W = 284;
    private static final int GROUPS_H = 62;
    private static final int GROUP_W = 292;
    private static final int GROUP_H = 128;

    @Test
    void layout_isCleanForTheFourGroups() {
        final GuiLayout l = CdeAppManagerLayout.layout(4, false, GROUPS_W, GROUPS_H);
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }

    @Test
    void layout_isCleanForAGroupThatFillsTwoRows() {
        final int count = CdeAppManagerLayout.perRow(GROUP_W) * 2;
        final GuiLayout l = CdeAppManagerLayout.layout(count, true, GROUP_W, GROUP_H);
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }

    @Test
    void well_startsUnderTheHeadOnlyWhenThereIsOne() {
        assertEquals(0, CdeAppManagerLayout.well(false, GROUP_W, GROUP_H).y());
        assertTrue(CdeAppManagerLayout.well(true, GROUP_W, GROUP_H).y() >= CdeAppManagerLayout.HEAD_H);
        final Rect well = CdeAppManagerLayout.well(true, GROUP_W, GROUP_H);
        assertEquals(GROUP_H, well.y() + well.h());
    }

    @Test
    void cell_startsAgainUnderneathWhenTheRowIsFull() {
        final int perRow = CdeAppManagerLayout.perRow(GROUP_W);
        final Rect first = CdeAppManagerLayout.cell(0, true, GROUP_W, GROUP_H);
        final Rect wrapped = CdeAppManagerLayout.cell(perRow, true, GROUP_W, GROUP_H);
        assertEquals(first.x(), wrapped.x());
        assertTrue(wrapped.y() >= first.y() + first.h());
    }

    @Test
    void picture_standsInsideItsCellOverTheName() {
        final Rect cell = CdeAppManagerLayout.cell(2, true, GROUP_W, GROUP_H);
        final Rect picture = CdeAppManagerLayout.picture(2, true, GROUP_W, GROUP_H);
        assertTrue(picture.x() >= cell.x() && picture.x() + picture.w() <= cell.x() + cell.w());
        assertEquals(cell.y(), picture.y());
        assertTrue(picture.y() + picture.h() + CdeAppManagerLayout.NAME_H <= cell.y() + cell.h());
    }

    @Test
    void indexAt_findsEveryIconAtTheMiddleOfItsPictureAndNoneOffThem() {
        final int count = CdeAppManagerLayout.perRow(GROUP_W) + 2;
        for (int i = 0; i < count; i++) {
            final Rect p = CdeAppManagerLayout.picture(i, true, GROUP_W, GROUP_H);
            assertEquals(i, CdeAppManagerLayout.indexAt(p.x() + p.w() / 2.0, p.y() + p.h() / 2.0, count, true,
                    GROUP_W, GROUP_H));
        }
        final Rect beyond = CdeAppManagerLayout.picture(count, true, GROUP_W, GROUP_H);
        assertEquals(-1, CdeAppManagerLayout.indexAt(beyond.x() + 2, beyond.y() + 2, count, true, GROUP_W, GROUP_H));
        assertEquals(-1, CdeAppManagerLayout.indexAt(2, 2, count, true, GROUP_W, GROUP_H));
    }

    @Test
    void perRow_isNeverLessThanOne() {
        assertEquals(1, CdeAppManagerLayout.perRow(10));
    }
}
