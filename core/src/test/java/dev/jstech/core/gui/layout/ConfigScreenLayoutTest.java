/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigScreenLayoutTest {

    /* The English labels' widths in the game's font: Defaults, Undo, Redo; Export, Import, Done. */
    private static final List<Integer> LEFT = List.of(43, 24, 24);
    private static final List<Integer> RIGHT = List.of(34, 34, 24);
    /* "12 changed, 3 on restart", and the filter's and the presets' labels. */
    private static final int STATUS = 118;
    private static final int FILTER = 66;
    private static final int PRESETS = 50;

    @Test
    void layout_fitsOnTheSmallestScreenWithABanner() {
        assertClean(layout(ConfigScreenLayout.SMALLEST_WIDTH, ConfigScreenLayout.SMALLEST_HEIGHT, true, 3));
    }

    @Test
    void layout_fitsOnTypicalScreens() {
        for (final int[] size : new int[][] {{427, 240}, {480, 270}, {640, 360}, {960, 540}}) {
            assertClean(layout(size[0], size[1], false, 2));
            assertClean(layout(size[0], size[1], true, 3));
        }
    }

    @Test
    void footer_putsWhatWaitsOnItsOwnRowOnlyWhenItDoesNotFit() {
        final ConfigScreenLayout.Footer wide = ConfigScreenLayout.footer(480, 270, LEFT, RIGHT, STATUS);
        assertEquals(ConfigScreenLayout.FOOTER, wide.height());
        final ConfigScreenLayout.Footer narrow = ConfigScreenLayout.footer(320, 240, LEFT, RIGHT, STATUS);
        assertEquals(ConfigScreenLayout.TALL_FOOTER, narrow.height());
        assertTrue(narrow.statusY() + ConfigScreenLayout.LINE <= narrow.buttonsY());
    }

    @Test
    void topRow_dropsTheFiltersLabelBeforeTheSearchGetsTooNarrow() {
        assertTrue(ConfigScreenLayout.topRow(480, FILTER, PRESETS).labelled());
        final ConfigScreenLayout.TopRow narrow = ConfigScreenLayout.topRow(320, FILTER, PRESETS);
        assertFalse(narrow.labelled());
        assertTrue(narrow.search().width() >= ConfigScreenLayout.NARROWEST_SEARCH);
        assertEquals(ConfigScreenLayout.CHECKBOX, narrow.filter().width());
    }

    @Test
    void railWidth_followsTheScreenBetweenItsBounds() {
        assertEquals(ConfigScreenLayout.NARROW_RAIL, ConfigScreenLayout.railWidth(320));
        assertEquals(124, ConfigScreenLayout.railWidth(480));
        assertEquals(ConfigScreenLayout.WIDE_RAIL, ConfigScreenLayout.railWidth(960));
    }

    @Test
    void nameWidth_leavesEveryNameRoomOnTheSmallestScreen() {
        assertTrue(ConfigScreenLayout.nameWidth(ConfigScreenLayout.SMALLEST_WIDTH, true) >= 60,
                () -> "a name has " + ConfigScreenLayout.nameWidth(ConfigScreenLayout.SMALLEST_WIDTH, true));
    }

    @Test
    void nameLines_breaksALongNameIntoTwoBetweenWords() {
        // Six texels a letter, as the game's font draws most of them.
        assertEquals(List.of("Teammates can", "hurt each other"),
                ConfigScreenLayout.nameLines("Teammates can hurt each other", 100, text -> 6 * text.length()));
        assertEquals(List.of("Short"), ConfigScreenLayout.nameLines("Short", 100, text -> 6 * text.length()));
    }

    @Test
    void nameLines_leavesWhatTwoLinesCannotHoldOnTheSecond() {
        final List<String> lines = ConfigScreenLayout.nameLines("one two three four five six", 30,
                text -> 6 * text.length());
        assertEquals(ConfigScreenLayout.NAME_LINES, lines.size());
        assertEquals("one", lines.get(0));
        assertEquals("two three four five six", lines.get(1));
    }

    @Test
    void rowHeight_growsALineForANameInTwo() {
        assertEquals(ConfigScreenLayout.ROW + ConfigScreenLayout.LINE, ConfigScreenLayout.rowHeight(false, 2));
        assertEquals(ConfigScreenLayout.NOTED_ROW, ConfigScreenLayout.rowHeight(true, 1));
        assertEquals(ConfigScreenLayout.ROW + ConfigScreenLayout.LINE, ConfigScreenLayout.rowHeight(false, 5),
                "never past the lines a name is given");
    }

    @Test
    void rowHeight_growsWithALargerText() {
        final int line = ConfigScreenLayout.LINE;
        assertEquals(ConfigScreenLayout.rowHeight(false, 2), ConfigScreenLayout.rowHeight(false, 2, line),
                "the normal text keeps the row as it is");
        assertEquals(ConfigScreenLayout.ROW + 4, ConfigScreenLayout.rowHeight(false, 1, line + 4));
        assertEquals(ConfigScreenLayout.ROW + line + 2 * 4 + 4, ConfigScreenLayout.rowHeight(true, 2, line + 4)
                - ConfigScreenLayout.NOTED_ROW + ConfigScreenLayout.ROW, "each line of the name and the note grow");
    }

    @Test
    void tabs_areCentredWithoutOverlapping() {
        for (final int width : new int[] {320, 480}) {
            final int last = ConfigScreenLayout.tabX(width, 2, 3) + ConfigScreenLayout.tabWidth(width, 3);
            assertEquals(width - last, ConfigScreenLayout.tabX(width, 0, 3), 1);
            assertTrue(ConfigScreenLayout.tabX(width, 0, 3) + ConfigScreenLayout.tabWidth(width, 3)
                    <= ConfigScreenLayout.tabX(width, 1, 3));
        }
    }

    private static GuiLayout layout(final int width, final int height, final boolean banner, final int tabs) {
        return ConfigScreenLayout.layout(width, height, banner, tabs, FILTER, PRESETS, LEFT, RIGHT, STATUS);
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), () -> layout.width() + "x" + layout.height() + " overlaps " + layout.overlaps()
                + ", out of bounds " + layout.outOfBounds());
    }
}
