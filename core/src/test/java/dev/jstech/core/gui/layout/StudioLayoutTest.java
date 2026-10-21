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

class StudioLayoutTest {

    /** The smallest screen, common ones, and a large one. */
    private static final List<int[]> SCREENS = List.of(new int[] {426, 240}, new int[] {480, 320},
            new int[] {640, 360}, new int[] {960, 540});

    @Test
    void layout_isCleanOnEveryScreen() {
        for (final int[] screen : SCREENS) {
            final GuiLayout layout = StudioLayout.layout(screen[0], screen[1]);
            assertTrue(layout.isClean(), screen[0] + "x" + screen[1] + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_keepsTheStatesInsideTheBenchAndApart() {
        for (final int[] screen : SCREENS) {
            final StudioLayout.Regions regions = StudioLayout.of(screen[0], screen[1]);
            final List<StudioLayout.Rect> states = regions.states();
            assertEquals(3, states.size());
            for (int i = 0; i < states.size(); i++) {
                assertTrue(states.get(i).within(regions.bench()), "state " + i + " at " + screen[0]);
                for (int j = i + 1; j < states.size(); j++) {
                    assertFalse(states.get(i).meets(states.get(j)), "states " + i + " and " + j);
                }
            }
        }
    }

    @Test
    void of_keepsTheSearchAndTheChecksInsideTheBar() {
        final StudioLayout.Regions regions = StudioLayout.of(426, 240);
        assertTrue(regions.search().within(regions.bar()));
        assertTrue(regions.checks().within(regions.bar()));
        assertFalse(regions.search().meets(regions.checks()));
    }

    @Test
    void open_standsInsideTheChecksOnItsRow() {
        final StudioLayout.Regions regions = StudioLayout.of(426, 240);
        final int rowY = regions.across().y() + StudioLayout.HEAD;
        final StudioLayout.Rect open = StudioLayout.open(regions, rowY);
        assertTrue(open.within(regions.across()));
        assertTrue(open.y() >= rowY && open.bottom() <= rowY + StudioLayout.CHECK_ROW);
    }

    @Test
    void of_holdsASmallerScreenToTheSmallest() {
        final StudioLayout.Regions regions = StudioLayout.of(320, 200);
        assertEquals(StudioLayout.MIN_WIDTH, regions.width());
        assertEquals(StudioLayout.MIN_HEIGHT, regions.height());
    }

    @Test
    void of_givesTheMiddleTheRoomTheSidesLeave() {
        final StudioLayout.Regions regions = StudioLayout.of(960, 540);
        assertEquals(StudioLayout.TREE_MAX, regions.tree().width());
        assertEquals(StudioLayout.PARTS_MAX, regions.madeOf().width());
        assertEquals(960 - 2 * StudioLayout.PAD - 2 * StudioLayout.GAP - StudioLayout.TREE_MAX
                - StudioLayout.PARTS_MAX, regions.bench().width());
    }
}
