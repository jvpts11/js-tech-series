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
import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CdeStyleLayoutTest {

    /** The work area of the smallest desktop CDE is drawn on: the glass, less the band the Front Panel stands in. */
    private static final int WORK_H = 256 - CdeFrontPanelLayout.BAND_H;

    @Test
    void everyWindow_isCleanWithEverythingItLists() {
        assertClean(CdeStyleLayout.stripLayout(3));
        assertClean(CdeStyleLayout.colorLayout(CdeScheme.ALL.size()));
        assertClean(CdeStyleLayout.backdropLayout(CdeBackdrop.values().length));
        assertClean(CdeStyleLayout.audioLayout());
    }

    @Test
    void everyWindow_fitsTheWorkAreaOfTheGlass() {
        assertTrue(CdeStyleLayout.BACKDROP_H + CdeStyleLayout.FRAME_H <= WORK_H);
        assertTrue(CdeStyleLayout.COLOR_H + CdeStyleLayout.FRAME_H <= WORK_H);
        assertTrue(CdeStyleLayout.AUDIO_H + CdeStyleLayout.FRAME_H <= WORK_H);
        assertTrue(CdeStyleLayout.BACKDROP_W + CdeStyleLayout.FRAME_W <= 384);
    }

    @Test
    void audio_keepsItsOutputsAboveTheRuleAndItsButtonsApart() {
        final Rect speakers = CdeStyleLayout.audioOutput(1);
        assertTrue(speakers.y() + speakers.h() <= CdeStyleLayout.audioRule(), "the outputs end above the rule");
        assertTrue(speakers.x() + speakers.w() <= CdeStyleLayout.AUDIO_W, "the second output stays inside");
        final Rect ok = CdeStyleLayout.audioButton(0);
        final Rect cancel = CdeStyleLayout.audioButton(1);
        assertTrue(CdeStyleLayout.audioRule() < ok.y() - 2, "the rule runs above the buttons' rings");
        assertEquals(0, CdeStyleLayout.audioButtonAt(ok.x() + 2, ok.y() + 2));
        assertEquals(1, CdeStyleLayout.audioButtonAt(cancel.x() + 2, cancel.y() + 2));
    }

    @Test
    void lists_endAboveTheButtonsAndKeepEveryLineInside() {
        final Rect colors = CdeStyleLayout.list(true, CdeScheme.ALL.size());
        assertTrue(colors.y() + colors.h() < CdeStyleLayout.button(true, 0).y() - 2);
        final Rect last = CdeStyleLayout.row(true, CdeScheme.ALL.size() - 1);
        assertTrue(last.y() + last.h() <= colors.y() + colors.h());
    }

    @Test
    void rowAt_findsEveryLineAtItsMiddleAndNoneBelowTheLast() {
        final int count = CdeScheme.ALL.size();
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
    void pageAt_findsEveryPageOfTheStrip() {
        for (int i = 0; i < 3; i++) {
            final Rect r = CdeStyleLayout.page(i);
            assertEquals(i, CdeStyleLayout.pageAt(r.x() + r.w() / 2.0, r.y() + r.h() / 2.0, 3));
            assertTrue(r.x() + r.w() <= CdeStyleLayout.STRIP_W, "page " + i + " stays on the strip");
        }
        assertEquals(-1, CdeStyleLayout.pageAt(1, 1, 3));
    }

    private static void assertClean(final GuiLayout l) {
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }
}
