/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArcPathTest {

    private static final double[] FROM = {0, 0, 0};
    private static final double[] TO = {0, 10, 0};
    private static final double CLOSE = 1e-9;

    @Test
    void of_endsWhereItMust() {
        final List<double[]> arc = ArcPath.of(FROM, TO, 0.2, 42);

        assertArrayEquals(FROM, arc.getFirst(), CLOSE);
        assertArrayEquals(TO, arc.getLast(), CLOSE);
        assertEquals((1 << ArcPath.SPLITS) + 1, arc.size());
    }

    @Test
    void of_straysAsideButNotFar() {
        final List<double[]> arc = ArcPath.of(FROM, TO, 0.2, 42);
        double farthest = 0;
        for (final double[] point : arc) {
            farthest = Math.max(farthest, Math.hypot(point[0], point[2]));
        }
        assertTrue(farthest > 0.1, "it zigzags: " + farthest);
        assertTrue(farthest < 10 * 0.2 * 2, "but stays near the line: " + farthest);
    }

    @Test
    void of_isTheSameForTheSameSeedAndAnotherForAnother() {
        assertArrayEquals(ArcPath.of(FROM, TO, 0.2, 7).get(8), ArcPath.of(FROM, TO, 0.2, 7).get(8), CLOSE);
        assertFalse(ArcPath.of(FROM, TO, 0.2, 7).get(8)[0] == ArcPath.of(FROM, TO, 0.2, 8).get(8)[0]);
    }

    @Test
    void of_isStraightWhenNotJagged() {
        for (final double[] point : ArcPath.of(FROM, TO, 0, 3)) {
            assertEquals(0, point[0], CLOSE);
            assertEquals(0, point[2], CLOSE);
        }
    }

    @Test
    void branches_startOnTheArcAndAreShorter() {
        final List<double[]> arc = ArcPath.of(FROM, TO, 0.2, 42);
        final List<List<double[]>> branches = ArcPath.branches(arc, 3, 0.2, 42);

        assertEquals(3, branches.size());
        for (final List<double[]> branch : branches) {
            assertTrue(arc.stream().anyMatch(point -> Arrays.equals(point, branch.getFirst())), "starts on the arc");
            final double[] a = branch.getFirst();
            final double[] b = branch.getLast();
            assertTrue(Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
                    + (a[2] - b[2]) * (a[2] - b[2])) < 10, "shorter than the arc");
        }
    }
}
