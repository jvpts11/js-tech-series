/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class MultiblockViewLayoutTest {

    @Test
    void layout_hasNothingOverlappingNorOutside() {
        final GuiLayout layout = MultiblockViewLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void layerButtons_giveEachLayerOneUntilItSteps() {
        assertEquals(4, MultiblockViewLayout.layerButtons(3));
        assertEquals(MultiblockViewLayout.MOST_LAYER_BUTTONS + 1,
                MultiblockViewLayout.layerButtons(MultiblockViewLayout.MOST_LAYER_BUTTONS));
        assertEquals(4, MultiblockViewLayout.layerButtons(12));
        assertFalse(MultiblockViewLayout.stepping(MultiblockViewLayout.MOST_LAYER_BUTTONS));
        assertTrue(MultiblockViewLayout.stepping(MultiblockViewLayout.MOST_LAYER_BUTTONS + 1));
    }

    @Test
    void pressed_showsItsOwnLayerOrAll() {
        assertEquals(2, MultiblockViewLayout.pressed(2, 0, 3));
        assertEquals(0, MultiblockViewLayout.pressed(0, 2, 3));
        assertTrue(MultiblockViewLayout.lit(2, 2, 3));
        assertTrue(MultiblockViewLayout.lit(0, 0, 3));
        assertFalse(MultiblockViewLayout.lit(1, 2, 3));
    }

    @Test
    void pressed_stepsATallMultiblockRoundItsLayers() {
        assertEquals(12, MultiblockViewLayout.pressed(2, 0, 12));
        assertEquals(1, MultiblockViewLayout.pressed(3, 0, 12));
        assertEquals(1, MultiblockViewLayout.pressed(3, 12, 12));
        assertEquals(4, MultiblockViewLayout.pressed(2, 5, 12));
        assertEquals(5, MultiblockViewLayout.pressed(1, 5, 12));
        assertTrue(MultiblockViewLayout.lit(1, 5, 12));
        assertFalse(MultiblockViewLayout.lit(1, 0, 12));
    }

    @Test
    void layerButtonAt_findsTheButtonsWhereTheyAreDrawn() {
        final MultiblockViewLayout.Point third = MultiblockViewLayout.layerButton(3);
        assertEquals(3, MultiblockViewLayout.layerButtonAt(third.x() + 1, third.y() + 1, 3));
        assertEquals(-1, MultiblockViewLayout.layerButtonAt(third.x() + 1, third.y() + 1, 2));
        assertEquals(-1, MultiblockViewLayout.layerButtonAt(0, 0, 3));
    }

    @Test
    void foot_tellsItsTwoButtonsApart() {
        assertTrue(MultiblockViewLayout.onShow(1, MultiblockViewLayout.FOOT_Y + 1));
        assertFalse(MultiblockViewLayout.onPage(1, MultiblockViewLayout.FOOT_Y + 1));
        assertTrue(MultiblockViewLayout.onPage(MultiblockViewLayout.WIDTH - 1, MultiblockViewLayout.FOOT_Y + 1));
        assertTrue(MultiblockViewLayout.onPreview(10, 10));
        assertFalse(MultiblockViewLayout.onPreview(MultiblockViewLayout.SIDE_X, 10));
    }
}
