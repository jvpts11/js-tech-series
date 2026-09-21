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

import dev.jstech.computers.gui.CdeBackdrop;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CdeStyleLayoutTest {

    /** The work area of the smallest desktop CDE is drawn on: the glass, less the band the Front Panel stands in. */
    private static final int WORK_H = 256 - CdeFrontPanelLayout.BAND_H;

    @Test
    void everyWindow_isCleanWithEverythingItLists() {
        assertClean(CdeStyleLayout.stripLayout(2));
        assertClean(CdeStyleLayout.colorLayout(CdePalette.ALL.size()));
        assertClean(CdeStyleLayout.backdropLayout(CdeBackdrop.values().length));
    }

    @Test
    void everyWindow_fitsTheWorkAreaOfTheGlass() {
        assertTrue(CdeStyleLayout.BACKDROP_H + CdeStyleLayout.FRAME_H <= WORK_H);
        assertTrue(CdeStyleLayout.COLOR_H + CdeStyleLayout.FRAME_H <= WORK_H);
        assertTrue(CdeStyleLayout.BACKDROP_W + CdeStyleLayout.FRAME_W <= 384);
    }

    @Test
    void lists_endAboveTheButtonsAndKeepEveryLineInside() {
        final Rect colors = CdeStyleLayout.list(true, CdePalette.ALL.size());
        assertTrue(colors.y() + colors.h() < CdeStyleLayout.button(true, 0).y() - 2);
        final Rect last = CdeStyleLayout.row(true, CdePalette.ALL.size() - 1);
        assertTrue(last.y() + last.h() <= colors.y() + colors.h());
    }

    @Test
    void rowAt_findsEveryLineAtItsMiddleAndNoneBelowTheLast() {
        final int count = CdePalette.ALL.size();
        for (int i = 0; i < count; i++) {
            final Rect r = CdeStyleLayout.row(true, i);
            assertEquals(i, CdeStyleLayout.rowAt(true, r.x() + r.w() / 2.0, r.y() + r.h() / 2.0, count));
        }
        final Rect beyond = CdeStyleLayout.row(true, count);
        assertEquals(-1, CdeStyleLayout.rowAt(true, beyond.x() + 2, beyond.y() + 2, count));
    }

    @Test
    void buttons_standSideBySideWithTheDefaultOneFirst() {
        for (final boolean colors : new boolean[] {true, false}) {
            final Rect first = CdeStyleLayout.button(colors, 0);
            final Rect second = CdeStyleLayout.button(colors, 1);
            assertEquals(first.y(), second.y());
            assertTrue(first.x() + first.w() < second.x());
            assertEquals(0, CdeStyleLayout.buttonAt(colors, first.x() + 2, first.y() + 2));
            assertEquals(1, CdeStyleLayout.buttonAt(colors, second.x() + 2, second.y() + 2));
            assertEquals(-1, CdeStyleLayout.buttonAt(colors, first.x() - 3, first.y() + 2));
        }
    }

    @Test
    void pageAt_findsBothPagesOfTheStrip() {
        for (int i = 0; i < 2; i++) {
            final Rect r = CdeStyleLayout.page(i);
            assertEquals(i, CdeStyleLayout.pageAt(r.x() + r.w() / 2.0, r.y() + r.h() / 2.0, 2));
        }
        assertEquals(-1, CdeStyleLayout.pageAt(1, 1, 2));
    }

    private static void assertClean(final GuiLayout l) {
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }
}
