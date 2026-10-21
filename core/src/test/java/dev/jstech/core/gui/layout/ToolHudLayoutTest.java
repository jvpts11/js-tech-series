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

class ToolHudLayoutTest {

    @Test
    void pointedAt_choosesTheFirstModeStraightUp() {
        assertEquals(0, ToolHudLayout.pointedAt(0, -50, 5));
        assertEquals(0, ToolHudLayout.pointedAt(0, -500, 5), "far out, the direction alone chooses");
    }

    @Test
    void pointedAt_goesRoundClockwise() {
        assertEquals(1, ToolHudLayout.pointedAt(50, -10, 5), "the second is to the right of the top");
        assertEquals(2, ToolHudLayout.pointedAt(30, 40, 5));
        assertEquals(3, ToolHudLayout.pointedAt(-30, 40, 5));
        assertEquals(4, ToolHudLayout.pointedAt(-50, -10, 5));
    }

    @Test
    void pointedAt_choosesNothingInTheMiddle() {
        assertEquals(-1, ToolHudLayout.pointedAt(5, 5, 5));
        assertEquals(-1, ToolHudLayout.pointedAt(0, -ToolHudLayout.INNER + 1, 5));
    }

    @Test
    void segmentAt_leavesAGapBetweenSegments() {
        final double border = Math.toRadians(ToolHudLayout.span(4) / 2.0);
        final double r = (ToolHudLayout.INNER + ToolHudLayout.OUTER) / 2.0;
        assertEquals(-1, ToolHudLayout.segmentAt(Math.sin(border) * r, -Math.cos(border) * r, 4),
                "the border between the first and the second is a gap");
        assertEquals(0, ToolHudLayout.segmentAt(0, -r, 4));
    }

    @Test
    void segmentAt_isOnlyBetweenTheCircles() {
        assertEquals(-1, ToolHudLayout.segmentAt(0, -(ToolHudLayout.OUTER + 1), 3));
        assertEquals(-1, ToolHudLayout.segmentAt(0, -(ToolHudLayout.INNER - 1), 3));
        assertEquals(0, ToolHudLayout.segmentAt(0, -ToolHudLayout.OUTER, 3));
    }

    @Test
    void onEdge_marksTheRimNotTheInside() {
        final int middle = (ToolHudLayout.INNER + ToolHudLayout.OUTER) / 2;
        assertTrue(ToolHudLayout.onEdge(0, -ToolHudLayout.OUTER, 5));
        assertFalse(ToolHudLayout.onEdge(0, -middle, 5));
    }

    @Test
    void place_sitsHalfwayRoundTheRing() {
        final int middle = (ToolHudLayout.INNER + ToolHudLayout.OUTER) / 2;
        assertEquals(0, ToolHudLayout.placeX(0, 4));
        assertEquals(-middle, ToolHudLayout.placeY(0, 4));
        assertEquals(middle, ToolHudLayout.placeX(1, 4), "the second of four is on the right");
        assertEquals(0, ToolHudLayout.placeY(1, 4));
    }

    @Test
    void iconFill_showsAPixelForAnythingAboveNothing() {
        assertEquals(0, ToolHudLayout.iconFill(0.0));
        assertEquals(1, ToolHudLayout.iconFill(0.001));
        assertEquals(ToolHudLayout.ICON_BAR_WIDTH, ToolHudLayout.iconFill(1.0));
        assertEquals(ToolHudLayout.ICON_BAR_WIDTH, ToolHudLayout.iconFill(3.0), "never past the bar");
    }

    @Test
    void barFill_fillsTheBarByTheFraction() {
        assertEquals(0, ToolHudLayout.barFill(0.0));
        assertEquals(11, ToolHudLayout.barFill(0.65));
        assertEquals(ToolHudLayout.BAR_WIDTH, ToolHudLayout.barFill(1.0));
    }

    @Test
    void iconBarRow_stacksUpFromTheBottom() {
        assertEquals(ToolHudLayout.ICON_BAR_ROW, ToolHudLayout.iconBarRow(0));
        assertEquals(ToolHudLayout.ICON_BAR_ROW - ToolHudLayout.ICON_BAR_STEP, ToolHudLayout.iconBarRow(1));
        assertTrue(ToolHudLayout.iconBarRow(ToolHudLayout.MOST_ICON_BARS - 1) >= 7,
                "the bars stop at the icon's middle, below the mode's letter");
    }
}
