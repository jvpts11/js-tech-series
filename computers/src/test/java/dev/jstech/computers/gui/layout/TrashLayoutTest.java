/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrashLayoutTest {

    /** The content of the smallest window, where everything is tightest. */
    private static final int W = TrashLayout.MIN_W - TrashLayout.FRAME_W;
    private static final int H = TrashLayout.MIN_H - TrashLayout.FRAME_H;

    /** "Empty the Recycle Bin", the longest task, as the pane writes it: 109 units at 0.85. */
    private static final int LONGEST_TASK_W = 93;

    /** "Empty Trash" as the button writes it: 62 units. */
    private static final int EMPTY_TRASH_W = 62;

    /** "Selected", the widest menu title: 43 units. */
    private static final int SELECTED_W = 43;

    /** "Desktop", the longest place, after its icon: 39 units. */
    private static final int DESKTOP_W = 39;

    @Test
    void framesLayout_isClean() {
        assertClean(TrashLayout.framesLayout(W, H));
        assertClean(TrashLayout.framesLayout(TrashLayout.DEFAULT_W - TrashLayout.FRAME_W,
                TrashLayout.DEFAULT_H - TrashLayout.FRAME_H));
    }

    @Test
    void linuxLayout_isClean() {
        assertClean(TrashLayout.linuxLayout(W, H));
    }

    @Test
    void cdeLayout_isClean() {
        assertClean(TrashLayout.cdeLayout(W, H));
    }

    @Test
    void window_fitsTheGlassAboveCdesPanel() {
        assertTrue(TrashLayout.DEFAULT_W <= 384);
        assertTrue(TrashLayout.DEFAULT_H <= 256 - CdeFrontPanelLayout.BAND_H);
    }

    @Test
    void tasks_holdTheLongestTaskInsideTheirBox() {
        assertTrue(TrashLayout.task(0).w() >= LONGEST_TASK_W, "task width " + TrashLayout.task(0).w());
        final Rect box = TrashLayout.tasksBox();
        final Rect last = TrashLayout.task(1);
        assertTrue(last.y() + last.h() <= box.y() + box.h());
    }

    @Test
    void labels_fitWhatHoldsThem() {
        assertTrue(TrashLayout.emptyButton().w() >= EMPTY_TRASH_W + 4);
        assertTrue(TrashLayout.menu(1).w() >= SELECTED_W + 4);
        assertTrue(3 + TrashLayout.ICON_W + 3 + DESKTOP_W <= TrashLayout.PLACES_W);
    }

    @Test
    void list_leavesTheNameColumnRoomForAName() {
        assertTrue(TrashLayout.placeX(W) - TrashLayout.nameX() >= 60, "name column " + (TrashLayout.placeX(W)
                - TrashLayout.nameX()));
        assertTrue(TrashLayout.sizeRight(W) - TrashLayout.SIZE_COL_W > TrashLayout.placeX(W));
    }

    @Test
    void cell_laysObjectsOutInRowsAndScrolls() {
        final Rect view = TrashLayout.linuxView(W, H);
        final int cols = TrashLayout.columns(view);
        assertTrue(cols >= 3, "columns " + cols);
        assertEquals(TrashLayout.cell(view, 0, 0).y(), TrashLayout.cell(view, cols - 1, 0).y());
        assertEquals(TrashLayout.cell(view, 0, 0).y() + TrashLayout.CELL_H, TrashLayout.cell(view, cols, 0).y());
        assertEquals(TrashLayout.cell(view, 0, 0).y(), TrashLayout.cell(view, cols, 1).y());
        final Rect lastShown = TrashLayout.cell(view, cols * TrashLayout.rowsShown(view) - 1, 0);
        assertTrue(lastShown.y() + lastShown.h() <= view.y() + view.h());
    }

    private static void assertClean(final GuiLayout l) {
        assertTrue(l.overlaps().isEmpty(), l.overlaps().toString());
        assertTrue(l.outOfBounds().isEmpty(), l.outOfBounds().toString());
    }
}
