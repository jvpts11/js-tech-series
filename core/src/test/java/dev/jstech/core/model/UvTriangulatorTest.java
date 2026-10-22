/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UvTriangulatorTest {

    private static final double[] SQUARE = {0, 0, 10, 0, 10, 10, 0, 10};
    private static final double[] HOLE = {4, 4, 6, 4, 6, 6, 4, 6};

    @Test
    void triangulate_coversASquareExactly() {
        final UvTriangulator.Result laid = UvTriangulator.triangulate(List.of(SQUARE), new double[0], 1e-9);
        assertEquals(100, area(laid), 1e-9);
    }

    @Test
    void triangulate_leavesAHoleOutWhicheverWayItsRingRuns() {
        final double[] backward = {4, 6, 6, 6, 6, 4, 4, 4};
        for (final double[] hole : List.of(HOLE, backward)) {
            final UvTriangulator.Result laid = UvTriangulator.triangulate(List.of(SQUARE, hole), new double[0],
                    1e-9);
            assertEquals(96, area(laid), 1e-9, "the square less the hole");
        }
    }

    @Test
    void triangulate_keepsThePointsInsideAndTheRingsCorners() {
        final double[] inside = {2, 2, 8, 8, 5, 5};
        final UvTriangulator.Result laid = UvTriangulator.triangulate(List.of(SQUARE, HOLE), inside, 1e-9);
        assertEquals(96, area(laid), 1e-9, "a point in the hole is dropped, the others laid in");
        int fromRings = 0;
        for (final int source : laid.sources()) {
            if (source >= 0) {
                fromRings++;
            }
        }
        assertEquals(8, fromRings, "every ring corner is told by where it came from");
    }

    @Test
    void triangulate_turnsEveryTriangleCounterClockwise() {
        final UvTriangulator.Result laid = UvTriangulator.triangulate(List.of(SQUARE, HOLE),
                new double[] {1, 1, 9, 9, 2, 8}, 1e-9);
        final double[] p = laid.points();
        final int[] t = laid.triangles();
        for (int i = 0; i < t.length; i += 3) {
            final double turn = (p[t[i + 1] * 2] - p[t[i] * 2]) * (p[t[i + 2] * 2 + 1] - p[t[i] * 2 + 1])
                    - (p[t[i + 1] * 2 + 1] - p[t[i] * 2 + 1]) * (p[t[i + 2] * 2] - p[t[i] * 2]);
            assertTrue(turn > 0, "triangle " + i / 3 + " turns the wrong way");
        }
    }

    private static double area(final UvTriangulator.Result laid) {
        final double[] p = laid.points();
        final int[] t = laid.triangles();
        double sum = 0;
        for (int i = 0; i < t.length; i += 3) {
            sum += ((p[t[i + 1] * 2] - p[t[i] * 2]) * (p[t[i + 2] * 2 + 1] - p[t[i] * 2 + 1])
                    - (p[t[i + 1] * 2 + 1] - p[t[i] * 2 + 1]) * (p[t[i + 2] * 2] - p[t[i] * 2])) / 2;
        }
        return sum;
    }
}
