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
