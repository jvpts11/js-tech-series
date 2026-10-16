/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ShapeSamplingTest {

    private static final int DRAWS = 2_000;
    private static final double CLOSE = 1e-9;

    @Test
    void sphere_staysWithinItsRadiusAndLeavesOutward() {
        final SplittableRandom random = new SplittableRandom(1);
        final double[] at = new double[3];
        final double[] way = new double[3];
        for (int i = 0; i < DRAWS; i++) {
            ShapeSampling.sphere(random, 2, at, way);
            assertTrue(length(at) <= 2 + CLOSE);
            assertEquals(1, length(way), 1e-6);
        }
    }

    @Test
    void disc_liesFlatWithinItsRadius() {
        final SplittableRandom random = new SplittableRandom(2);
        final double[] at = new double[3];
        for (int i = 0; i < DRAWS; i++) {
            ShapeSampling.disc(random, 0.4, at);
            assertEquals(0, at[1], CLOSE);
            assertTrue(Math.hypot(at[0], at[2]) <= 0.4 + CLOSE);
        }
    }

    @Test
    void cone_leavesWithinHalfItsAngle() {
        final SplittableRandom random = new SplittableRandom(3);
        final double[] way = new double[3];
        final double half = Math.cos(Math.toRadians(15));
        for (int i = 0; i < DRAWS; i++) {
            ShapeSampling.cone(random, 30, way);
            assertTrue(way[1] >= half - CLOSE, "within the cone: " + way[1]);
            assertEquals(1, length(way), 1e-6);
        }
    }

    @Test
    void boxAndLine_stayWithinTheirSize() {
        final SplittableRandom random = new SplittableRandom(4);
        final double[] at = new double[3];
        for (int i = 0; i < DRAWS; i++) {
            ShapeSampling.box(random, 1, at);
            assertTrue(Math.abs(at[0]) <= 0.5 && Math.abs(at[1]) <= 0.5 && Math.abs(at[2]) <= 0.5);
            ShapeSampling.line(random, 3, at);
            assertTrue(Math.abs(at[1]) <= 1.5 && at[0] == 0 && at[2] == 0);
        }
    }

    private static double length(final double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }
}
