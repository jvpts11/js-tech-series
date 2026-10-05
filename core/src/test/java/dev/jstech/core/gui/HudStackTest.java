/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HudStackTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;

    @Test
    void place_stacksTheTopCornersDownwards() {
        final List<HudStack.Point> points = HudStack.place(HudStack.Corner.TOP_LEFT, WIDTH, HEIGHT,
                List.of(new HudStack.Size(50, 10), new HudStack.Size(30, 20)));
        assertEquals(new HudStack.Point(4, 4), points.get(0));
        assertEquals(new HudStack.Point(4, 16), points.get(1));
    }

    @Test
    void place_putsTheRightCornersFlushWithTheRightEdge() {
        final List<HudStack.Point> points = HudStack.place(HudStack.Corner.TOP_RIGHT, WIDTH, HEIGHT,
                List.of(new HudStack.Size(50, 10), new HudStack.Size(30, 20)));
        assertEquals(WIDTH - 4 - 50, points.get(0).x());
        assertEquals(WIDTH - 4 - 30, points.get(1).x());
    }

    @Test
    void place_stacksTheBottomCornersUpwards() {
        final List<HudStack.Point> points = HudStack.place(HudStack.Corner.BOTTOM_RIGHT, WIDTH, HEIGHT,
                List.of(new HudStack.Size(50, 10), new HudStack.Size(30, 20)));
        assertEquals(new HudStack.Point(WIDTH - 54, HEIGHT - 14), points.get(0));
        assertEquals(new HudStack.Point(WIDTH - 34, HEIGHT - 16 - 20), points.get(1));
    }

    @Test
    void place_neverLetsTwoElementsOfACornerOverlap() {
        for (final HudStack.Corner corner : List.of(HudStack.Corner.TOP_LEFT, HudStack.Corner.TOP_RIGHT,
                HudStack.Corner.BOTTOM_LEFT, HudStack.Corner.BOTTOM_RIGHT)) {
            final List<HudStack.Size> sizes = List.of(new HudStack.Size(40, 9), new HudStack.Size(80, 30),
                    new HudStack.Size(10, 1), new HudStack.Size(60, 12));
            final List<HudStack.Point> points = HudStack.place(corner, WIDTH, HEIGHT, sizes);
            for (int a = 0; a < sizes.size(); a++) {
                for (int b = a + 1; b < sizes.size(); b++) {
                    final int topA = points.get(a).y();
                    final int topB = points.get(b).y();
                    final boolean apart = topA + sizes.get(a).height() <= topB
                            || topB + sizes.get(b).height() <= topA;
                    assertTrue(apart, corner + ": elements " + a + " and " + b + " overlap");
                }
            }
        }
    }

    @Test
    void size_refusesANegativeSide() {
        assertThrows(IllegalArgumentException.class, () -> new HudStack.Size(-1, 4));
    }
}
