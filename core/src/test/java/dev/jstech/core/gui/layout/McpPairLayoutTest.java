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

import java.util.List;
import org.junit.jupiter.api.Test;

class McpPairLayoutTest {

    /** The smallest screen the game's scaling leaves, common ones, and a large one. */
    private static final List<int[]> SCREENS = List.of(new int[] {320, 240}, new int[] {426, 240},
            new int[] {480, 320}, new int[] {640, 360}, new int[] {960, 540});
    /** The most lines each part wraps to, on the narrowest screen, in the longer of the series' languages. */
    private static final int WHO = 3;
    private static final int READ = 4;
    private static final int ACT = 4;

    @Test
    void layout_isCleanOnEveryScreen() {
        for (final int[] screen : SCREENS) {
            final McpPairLayout.Regions regions = McpPairLayout.of(screen[0], screen[1], WHO, READ, ACT);
            final GuiLayout layout = McpPairLayout.layout(regions, WHO, READ, ACT);
            assertTrue(layout.isClean(), screen[0] + "x" + screen[1] + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_putsTheAnswersInARowWhenTheyFit() {
        final List<StudioLayout.Rect> wide = McpPairLayout.of(640, 360, 2, 2, 2).buttons();
        assertEquals(wide.get(0).y(), wide.get(2).y());
        final List<StudioLayout.Rect> narrow = McpPairLayout.of(320, 300, 2, 2, 2).buttons();
        assertTrue(narrow.get(2).y() > narrow.get(0).y());
    }

    @Test
    void of_keepsTheWordsAsNarrowAsCanBeRead() {
        assertEquals(McpPairLayout.MOST_TEXT, McpPairLayout.of(1920, 1080, 1, 1, 1).textWidth());
    }
}
