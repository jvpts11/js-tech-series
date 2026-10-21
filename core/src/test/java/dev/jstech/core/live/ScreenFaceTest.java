/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenFaceTest {

    private static final double NEAR = 1e-9;

    @Test
    void point_readsANorthFaceFromItsEastSide() {
        // Seen from the north, looking south, the face's left is to the east, at x 1.
        assertArrayEquals(new double[] {0, 0}, ScreenFace.point(ScreenFace.Side.NORTH, 1, 1, 0), NEAR);
        assertArrayEquals(new double[] {0.75, 0.5}, ScreenFace.point(ScreenFace.Side.NORTH, 0.25, 0.5, 0), NEAR);
    }

    @Test
    void point_readsEverySideFaceFromItsTopLeft() {
        assertArrayEquals(new double[] {0, 0}, ScreenFace.point(ScreenFace.Side.SOUTH, 0, 1, 1), NEAR);
        assertArrayEquals(new double[] {0, 0}, ScreenFace.point(ScreenFace.Side.EAST, 1, 1, 1), NEAR);
        assertArrayEquals(new double[] {0, 0}, ScreenFace.point(ScreenFace.Side.WEST, 0, 1, 0), NEAR);
    }

    @Test
    void pixel_findsThePixelOfAScreenOnPartOfTheFace() {
        final TouchArea area = new TouchArea(0.25, 0.25, 0.75, 0.75, 64, 32);
        assertArrayEquals(new int[] {32, 16}, ScreenFace.pixel(area, new double[] {0.5, 0.5}).orElseThrow());
        assertArrayEquals(new int[] {0, 0}, ScreenFace.pixel(area, new double[] {0.25, 0.25}).orElseThrow());
    }

    @Test
    void pixel_isNothingOffTheScreen() {
        final TouchArea area = new TouchArea(0.25, 0.25, 0.75, 0.75, 64, 32);
        assertTrue(ScreenFace.pixel(area, new double[] {0.1, 0.5}).isEmpty());
        assertTrue(ScreenFace.pixel(area, new double[] {0.5, 0.75}).isEmpty(), "the bottom edge is past it");
    }
}
