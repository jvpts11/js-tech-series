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

import org.junit.jupiter.api.Test;

class ModelPreviewLayoutTest {

    /* A CAD model: four animations, the switches without locators, the hint and four lines on its detail. */
    private static final ModelPreviewLayout.Column CAD = new ModelPreviewLayout.Column(4, 4, 5, true);
    /* The most a column holds: many animations, every switch, every line. */
    private static final ModelPreviewLayout.Column FULL = new ModelPreviewLayout.Column(12, 5, 5, true);
    /* A model of cubes with no animations, no far model, its locators. */
    private static final ModelPreviewLayout.Column STILL = new ModelPreviewLayout.Column(0, 4, 4, false);

    @Test
    void layout_fitsWithoutCollidingOnTheSmallestScreenAndATypicalOne() {
        for (final ModelPreviewLayout.Column column : new ModelPreviewLayout.Column[] {CAD, FULL, STILL}) {
            assertClean(ModelPreviewLayout.layout(ModelPreviewLayout.SMALLEST_WIDTH,
                    ModelPreviewLayout.SMALLEST_HEIGHT, column));
            assertClean(ModelPreviewLayout.layout(480, 270, column));
            assertClean(ModelPreviewLayout.layout(640, 360, column));
        }
    }

    @Test
    void layout_withNoAnimationsLeavesTheTreeTheRoom() {
        final ModelPreviewLayout.Column moving = new ModelPreviewLayout.Column(4, 4, 4, false);
        assertTrue(ModelPreviewLayout.treeRows(270, STILL) > ModelPreviewLayout.treeRows(270, moving));
    }

    @Test
    void buttonsShown_stopsAtFourAndAtWhatTheScreenHolds() {
        assertEquals(4, ModelPreviewLayout.buttonsShown(360, FULL));
        assertEquals(2, ModelPreviewLayout.buttonsShown(360, new ModelPreviewLayout.Column(2, 4, 5, true)));
        assertTrue(ModelPreviewLayout.buttonsShown(ModelPreviewLayout.SMALLEST_HEIGHT, FULL) >= 1,
                "a short screen keeps one animation's button");
        assertTrue(ModelPreviewLayout.treeRows(ModelPreviewLayout.SMALLEST_HEIGHT, FULL) >= ModelPreviewLayout
                .FEWEST_ROWS, "the tree always shows a few bones");
    }

    @Test
    void detailsShown_letsTheHintGoFirstOnAShortScreen() {
        assertEquals(5, ModelPreviewLayout.detailsShown(360, CAD));
        assertTrue(ModelPreviewLayout.hintShown(360, CAD));
        assertTrue(ModelPreviewLayout.detailsShown(ModelPreviewLayout.SMALLEST_HEIGHT, FULL) < 5);
        assertFalse(ModelPreviewLayout.hintShown(ModelPreviewLayout.SMALLEST_HEIGHT, FULL));
        assertTrue(ModelPreviewLayout.barY(ModelPreviewLayout.SMALLEST_HEIGHT, FULL) > ModelPreviewLayout.detailY(
                ModelPreviewLayout.SMALLEST_HEIGHT, FULL, 0), "the bar is under the triangles' line");
    }

    @Test
    void switchesTop_risesARowForAFifthSwitch() {
        assertEquals(ModelPreviewLayout.SWITCH_PITCH, ModelPreviewLayout.switchesTop(270, CAD) - ModelPreviewLayout
                .switchesTop(270, FULL));
        assertEquals(ModelPreviewLayout.switchY(270, FULL, 4), ModelPreviewLayout.switchY(270, FULL, 2)
                + ModelPreviewLayout.SWITCH_PITCH);
    }

    @Test
    void positions_matchTheApprovedScreen() {
        // The approved screen is 480 by 270: the view down to the bottom bar, the column 168 wide at the right.
        assertEquals(294, ModelPreviewLayout.viewWidth(480));
        assertEquals(306, ModelPreviewLayout.columnX(480));
        assertEquals(38, ModelPreviewLayout.buttonY(0));
        assertEquals(240, ModelPreviewLayout.bottomY(270));
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
