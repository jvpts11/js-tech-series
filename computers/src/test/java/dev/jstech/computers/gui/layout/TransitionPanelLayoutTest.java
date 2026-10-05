/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitionPanelLayoutTest {

    private static final int[] WIDTHS = {320, 384, 480, 640};

    @Test
    void kde4_isCleanAtEveryWidth() {
        for (final int width : WIDTHS) {
            final GuiLayout l = TransitionPanelLayout.kde4(width, width - 110);
            assertTrue(l.isClean(), width + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void gnome2Foot_isCleanAtEveryWidth() {
        for (final int width : WIDTHS) {
            final GuiLayout l = TransitionPanelLayout.gnome2Foot(width);
            assertTrue(l.isClean(), width + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void gnome2Top_isCleanWithItsLongestWords() {
        /*
         * The menus at the width of their longest words in either language, two launchers, and the status area with
         * the clock, the day and the account, from the desktop's usual width (the glass drawn at three quarters) up.
         */
        for (final int width : new int[] {480, 512, 640}) {
            final GuiLayout l = TransitionPanelLayout.gnome2Top(width, 60, 40, 40, 2, 220);
            assertTrue(l.isClean(), width + ": " + l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void pager_holdsEveryWorkspaceInsideTheBar() {
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int y = TransitionPanelLayout.pagerCellY(i);
            assertTrue(y >= 0 && y + TransitionPanelLayout.PAGER_CELL_H <= TransitionPanelLayout.BAR_H, "cell " + i);
            assertTrue(TransitionPanelLayout.pagerCellX(i) + TransitionPanelLayout.PAGER_CELL_W
                    < TransitionPanelLayout.KDE4_TASKS_X, "cell " + i + " clears the task manager");
        }
    }

    @Test
    void switcher_standsBetweenTheWindowListAndTheTrash() {
        for (final int width : WIDTHS) {
            assertTrue(TransitionPanelLayout.gnome2TasksRight(width) < TransitionPanelLayout.switcherX(width));
            assertTrue(TransitionPanelLayout.switcherCellX(width, WorkspaceSet.COUNT - 1)
                    + TransitionPanelLayout.SWITCHER_CELL_W < TransitionPanelLayout.trashX(width));
        }
    }

    @Test
    void menus_followOneAnotherInTheirOrder() {
        final int[][] menus = TransitionPanelLayout.menus(60, 40, 40);
        assertTrue(menus[0][0] + menus[0][1] <= menus[1][0]);
        assertTrue(menus[1][0] + menus[1][1] <= menus[2][0]);
        assertTrue(menus[0][1] > TransitionPanelLayout.FOOT + 60, "Applications has room for its foot and its words");
    }
}
