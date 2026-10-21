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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudioLayoutTest {

    /** The smallest screen, common ones, and a large one. */
    private static final List<int[]> SCREENS = List.of(new int[] {426, 240}, new int[] {480, 320},
            new int[] {640, 360}, new int[] {960, 540});
    /**
     * The tools' labels, each as wide as the longer of the series' two languages writes it at three quarters of the
     * game's font, in the order of the tabs.
     */
    private static final List<Integer> TABS = List.of(27, 23, 23, 32, 36, 18, 18, 41, 59, 45, 27, 18, 18);

    @Test
    void layout_isCleanOnEveryScreen() {
        for (final int[] screen : SCREENS) {
            final GuiLayout layout = StudioLayout.layout(screen[0], screen[1], TABS);
            assertTrue(layout.isClean(), screen[0] + "x" + screen[1] + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_keepsTheStatesInsideTheBenchAndApart() {
        for (final int[] screen : SCREENS) {
            final StudioLayout.Regions regions = StudioLayout.of(screen[0], screen[1], TABS);
            final List<StudioLayout.Rect> states = regions.states();
            assertEquals(3, states.size());
            for (int i = 0; i < states.size(); i++) {
                assertTrue(states.get(i).within(regions.bench()), "state " + i + " at " + screen[0]);
                assertFalse(states.get(i).meets(regions.label()), "state " + i + " and the label at " + screen[0]);
                for (int j = i + 1; j < states.size(); j++) {
                    assertFalse(states.get(i).meets(states.get(j)), "states " + i + " and " + j);
                }
            }
        }
    }

    @Test
    void of_keepsTheSearchAndTheChecksInsideTheBar() {
        final StudioLayout.Regions regions = StudioLayout.of(426, 240, TABS);
        assertTrue(regions.search().within(regions.bar()));
        assertTrue(regions.checks().within(regions.bar()));
        assertFalse(regions.search().meets(regions.checks()));
    }

    @Test
    void of_putsTheTabsInOneRowWhenTheyFit() {
        final StudioLayout.Regions regions = StudioLayout.of(960, 540, TABS);
        assertEquals(StudioLayout.TAB_HEIGHT, regions.strip().height());
        assertTrue(regions.tabs().stream().allMatch(tab -> tab.y() == regions.strip().y()));
    }

    @Test
    void of_wrapsTheTabsIntoMoreRowsOnANarrowScreen() {
        final StudioLayout.Regions regions = StudioLayout.of(426, 240, TABS);
        assertTrue(regions.strip().height() > StudioLayout.TAB_HEIGHT, "the tabs wrap at 426");
        for (final StudioLayout.Rect tab : regions.tabs()) {
            assertTrue(tab.within(regions.strip()), "every tab stays on the strip: " + tab);
            assertTrue(tab.right() <= regions.width() - StudioLayout.PAD, "no tab runs past the screen: " + tab);
        }
        assertTrue(regions.body().y() > regions.strip().bottom(), "the pages start under the strip");
    }

    @Test
    void of_givesNoTabsAStripOfOneRow() {
        final StudioLayout.Regions regions = StudioLayout.of(640, 360, Collections.emptyList());
        assertEquals(StudioLayout.TAB_HEIGHT, regions.strip().height());
    }

    @Test
    void open_standsInsideTheChecksOnItsRow() {
        final StudioLayout.Regions regions = StudioLayout.of(426, 240, TABS);
        final int rowY = regions.across().y() + StudioLayout.HEAD;
        final StudioLayout.Rect open = StudioLayout.open(regions, rowY);
        assertTrue(open.within(regions.across()));
        assertTrue(open.y() >= rowY && open.bottom() <= rowY + StudioLayout.CHECK_ROW);
    }

    @Test
    void of_holdsASmallerScreenToTheSmallest() {
        final StudioLayout.Regions regions = StudioLayout.of(320, 200, TABS);
        assertEquals(StudioLayout.MIN_WIDTH, regions.width());
        assertEquals(StudioLayout.MIN_HEIGHT, regions.height());
    }

    @Test
    void of_givesTheMiddleTheRoomTheSidesLeave() {
        final StudioLayout.Regions regions = StudioLayout.of(960, 540, TABS);
        assertEquals(StudioLayout.TREE_MAX, regions.tree().width());
        assertEquals(StudioLayout.PARTS_MAX, regions.madeOf().width());
        assertEquals(960 - 2 * StudioLayout.PAD - 2 * StudioLayout.GAP - StudioLayout.TREE_MAX
                - StudioLayout.PARTS_MAX, regions.bench().width());
    }

    @Test
    void columns_splitTheBodyByShareWithTheLastTakingTheRest() {
        final StudioLayout.Rect body = new StudioLayout.Rect(6, 40, 628, 290);
        final List<StudioLayout.Rect> columns = StudioLayout.columns(body, 0.25, 0.5);
        assertEquals(3, columns.size());
        assertEquals(6, columns.get(0).x());
        assertEquals(body.right(), columns.get(2).right());
        for (int i = 0; i + 1 < columns.size(); i++) {
            assertEquals(columns.get(i).right() + StudioLayout.GAP, columns.get(i + 1).x());
        }
    }
}
