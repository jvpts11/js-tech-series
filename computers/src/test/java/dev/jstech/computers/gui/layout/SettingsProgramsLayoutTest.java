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

class SettingsProgramsLayoutTest {

    /* The page in the narrowest Settings window and in one of its usual size, both of the usual height. */
    private static final int NARROWEST = 143;
    private static final int USUAL = 169;
    private static final int HEIGHT = 212;
    /* The words as the font measures them: the heading, a count of two figures, the buttons, an update's line. */
    private static final int HEADING_W = 48;
    private static final int COUNT_W = 96;
    private static final int UNINSTALL_W = 42;
    private static final int UPDATE_W = 34;
    private static final int BADGE_W = 132;

    @Test
    void layout_isCleanAsTheMockShowsIt() {
        final boolean[] updated = {false, true, true, false, false};
        final GuiLayout layout = SettingsProgramsLayout.layout(USUAL, HEIGHT, HEADING_W, COUNT_W, updated, UNINSTALL_W,
                UPDATE_W, BADGE_W);
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanInTheNarrowestWindowWithEveryProgramUpdated() {
        final boolean[] updated = {true, true, true, true, true, true, true, true, true};
        final GuiLayout layout = SettingsProgramsLayout.layout(NARROWEST, HEIGHT, HEADING_W, COUNT_W, updated,
                UNINSTALL_W, UPDATE_W, BADGE_W * 2);
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void countX_standsClearOfTheHeadingWhenThePageIsTooNarrow() {
        assertEquals(USUAL - COUNT_W, SettingsProgramsLayout.countX(USUAL, HEADING_W, COUNT_W));
        assertEquals(HEADING_W + SettingsProgramsLayout.GAP,
                SettingsProgramsLayout.countX(NARROWEST, HEADING_W, COUNT_W + 20));
    }

    @Test
    void nameRoom_endsBeforeTheFirstButton() {
        final int uninstall = SettingsProgramsLayout.buttonWidth(UNINSTALL_W);
        final int update = SettingsProgramsLayout.buttonWidth(UPDATE_W);
        assertEquals(USUAL - uninstall - SettingsProgramsLayout.GAP,
                SettingsProgramsLayout.nameRoom(USUAL, uninstall, update, false));
        assertEquals(SettingsProgramsLayout.updateX(USUAL, uninstall, update) - SettingsProgramsLayout.GAP,
                SettingsProgramsLayout.nameRoom(USUAL, uninstall, update, true));
    }

    @Test
    void rowHeight_endsTheUpdatesLineAboveTheNextRowsButtons() {
        assertTrue(SettingsProgramsLayout.rowHeight(true) + SettingsProgramsLayout.BUTTON_Y
                >= SettingsProgramsLayout.BADGE_Y + SettingsProgramsLayout.smallLineHeight());
        assertEquals(SettingsProgramsLayout.ROW_H, SettingsProgramsLayout.rowHeight(false));
    }
}
