/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HudSnapTest {

    private static final int WIDE = 480;
    private static final int TALL = 270;
    private static final HudStack.Size SIZE = new HudStack.Size(50, 20);

    @Test
    void snap_goesOnTheGridWhenNear() {
        assertEquals(new HudStack.Point(96, 48), HudSnap.snap(98, 46, SIZE, List.of(), WIDE, TALL));
    }

    @Test
    void snap_leavesAPlaceFarFromAnyLine() {
        // Six and eight pixels from the lines its near and far edges would go on.
        assertEquals(new HudStack.Point(104, 38), HudSnap.snap(104, 38, SIZE, List.of(), WIDE, TALL));
    }

    @Test
    void snap_goesAgainstAnotherElementAGapApart() {
        final List<HudSnap.Box> others = List.of(new HudSnap.Box(100, 100, 60, 30));
        final HudStack.Point point = HudSnap.snap(103, 133, SIZE, others, WIDE, TALL);
        assertEquals(100, point.x(), "flush with its left edge");
        assertEquals(130 + HudStack.GAP, point.y(), "just under it");
    }

    @Test
    void snap_keepsTheCornersMarginByTheScreensSide() {
        final HudStack.Point point = HudSnap.snap(WIDE - SIZE.width() - 2, 2, SIZE, List.of(), WIDE, TALL);
        assertEquals(WIDE - HudStack.MARGIN - SIZE.width(), point.x());
        assertEquals(HudStack.MARGIN, point.y());
    }

    @Test
    void snap_keepsTheElementOnTheScreenAtTheCornersMargin() {
        assertEquals(new HudStack.Point(WIDE - HudStack.MARGIN - SIZE.width(), HudStack.MARGIN),
                HudSnap.snap(900, -50, SIZE, List.of(), WIDE, TALL));
    }

    @Test
    void scale_goesInQuarterSteps() {
        assertEquals(1.25F, HudSnap.scale(1.2F));
        assertEquals(HudPlacement.LEAST_SCALE, HudSnap.scale(0.1F));
        assertEquals(HudPlacement.MOST_SCALE, HudSnap.scale(7));
    }
}
