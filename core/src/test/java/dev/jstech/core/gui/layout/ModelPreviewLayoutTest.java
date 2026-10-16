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

class ModelPreviewLayoutTest {

    @Test
    void layout_fitsWithoutCollidingOnTheSmallestScreenAndATypicalOne() {
        assertClean(ModelPreviewLayout.layout(ModelPreviewLayout.SMALLEST_WIDTH, ModelPreviewLayout.SMALLEST_HEIGHT,
                4));
        assertClean(ModelPreviewLayout.layout(480, 270, 4));
        assertClean(ModelPreviewLayout.layout(640, 360, 12));
    }

    @Test
    void layout_withNoAnimationsLeavesTheTreeTheRoom() {
        assertClean(ModelPreviewLayout.layout(480, 270, 0));
        assertTrue(ModelPreviewLayout.treeRows(270, 0) > ModelPreviewLayout.treeRows(270, 4));
    }

    @Test
    void buttonsShown_stopsAtFourAndAtWhatTheScreenHolds() {
        assertEquals(4, ModelPreviewLayout.buttonsShown(360, 12));
        assertEquals(2, ModelPreviewLayout.buttonsShown(360, 2));
        assertTrue(ModelPreviewLayout.treeRows(ModelPreviewLayout.SMALLEST_HEIGHT, 12) >= ModelPreviewLayout
                .FEWEST_ROWS, "the tree always shows a few bones");
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
