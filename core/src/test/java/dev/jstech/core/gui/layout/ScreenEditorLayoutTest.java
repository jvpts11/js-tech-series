/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenEditorLayoutTest {

    @Test
    void panel_isCleanAtItsWidest() {
        final GuiLayout layout = ScreenEditorLayout.panel(ScreenEditorLayout.PANEL_WIDTH);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void panel_isCleanAtItsNarrowest() {
        final GuiLayout layout = ScreenEditorLayout.panel(ScreenEditorLayout.MIN_WIDTH);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void panelWidth_endsBeforeTheScreen() {
        assertEquals(ScreenEditorLayout.PANEL_WIDTH, ScreenEditorLayout.panelWidth(300));
        assertEquals(152 - ScreenEditorLayout.GAP, ScreenEditorLayout.PANEL_X + ScreenEditorLayout.panelWidth(152),
                "a test machine's screen on the game's 480 wide screen: the panel ends before it");
        assertEquals(ScreenEditorLayout.MIN_WIDTH, ScreenEditorLayout.panelWidth(40));
    }

    @Test
    void addWidth_startsAfterTheScreen() {
        final int width = 480;
        final int right = 328;
        assertTrue(width - ScreenEditorLayout.PAD - ScreenEditorLayout.addWidth(width, right)
                >= right + ScreenEditorLayout.GAP, "the list of kinds starts after the window");
        assertEquals(ScreenEditorLayout.ADD_WIDTH, ScreenEditorLayout.addWidth(640, right));
    }

    @Test
    void bar_isCleanOnTheGamesSmallestScreen() {
        final GuiLayout layout = ScreenEditorLayout.bar(427, ScreenEditorLayout.BAR_WIDTHS);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void bar_isCleanWithLongerLabels() {
        // How many letters each label has in the series' longest translation, at the game's six pixels a letter.
        final int[] letters = {8, 7, 13, 12, 6, 10};
        final int[] widths = new int[letters.length];
        for (int i = 0; i < letters.length; i++) {
            widths[i] = ScreenEditorLayout.barWidth(i, Math.round(letters[i] * GuiLayout.GLYPH_WIDTH));
        }
        final GuiLayout layout = ScreenEditorLayout.bar(427, widths);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void barWidth_growsForALabelWiderThanItsLeast() {
        assertEquals(ScreenEditorLayout.BAR_WIDTHS[0], ScreenEditorLayout.barWidth(0, 10));
        assertEquals(60 + 2 * ScreenEditorLayout.LABEL_ROOM, ScreenEditorLayout.barWidth(0, 60));
    }

    @Test
    void bar_endsTheRoomInsideTheScreensRightEdge() {
        final int[] widths = ScreenEditorLayout.BAR_WIDTHS;
        final int last = widths.length - 1;
        assertEquals(640 - ScreenEditorLayout.PAD, ScreenEditorLayout.barX(640, widths, last) + widths[last]);
    }
}
