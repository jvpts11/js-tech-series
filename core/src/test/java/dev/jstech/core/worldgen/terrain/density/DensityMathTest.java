/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DensityMathTest {

    private static final double CLOSE = 1e-9;

    @Test
    void terrace_isFlatAcrossAStepAndRisesOverItsLastPart() {
        assertEquals(0, DensityMath.terrace(0.3, 1, 0.5), CLOSE);
        assertEquals(0, DensityMath.terrace(0.5, 1, 0.5), CLOSE);
        assertEquals(0.5, DensityMath.terrace(0.75, 1, 0.5), CLOSE);
        assertEquals(1, DensityMath.terrace(1.2, 1, 0.5), CLOSE);
        assertEquals(-1, DensityMath.terrace(-0.7, 1, 0.5), CLOSE);
    }

    @Test
    void terrace_withNoSmoothnessIsSheerSteps() {
        assertEquals(2, DensityMath.terrace(2.99, 1, 0), CLOSE);
        assertEquals(3, DensityMath.terrace(3.0, 1, 0), CLOSE);
    }

    @Test
    void ridge_isHighestWhereTheValueCrossesNothing() {
        assertEquals(1, DensityMath.ridge(0), CLOSE);
        assertEquals(0.75, DensityMath.ridge(0.25), CLOSE);
        assertEquals(0.75, DensityMath.ridge(-0.25), CLOSE);
    }

    @Test
    void power_keepsTheSign() {
        assertEquals(0.25, DensityMath.power(0.5, 2), CLOSE);
        assertEquals(-0.25, DensityMath.power(-0.5, 2), CLOSE);
        assertEquals(0, DensityMath.power(0, 3), CLOSE);
    }

    @Test
    void smoothMin_staysUnderTheSmallerByAtMostAQuarterOfItsSmoothness() {
        final double k = 0.4;
        for (double a = -1; a <= 1; a += 0.05) {
            final double b = 0.2;
            final double joined = DensityMath.smoothMin(a, b, k);
            assertTrue(joined <= Math.min(a, b) + CLOSE, "never above the smaller at " + a);
            assertTrue(joined >= Math.min(a, b) - k / 4 - CLOSE, "at most a quarter below at " + a);
        }
        assertEquals(-0.5, DensityMath.smoothMin(-0.5, 0.5, 0.4), CLOSE);
        assertEquals(Math.min(0.3, 0.1), DensityMath.smoothMin(0.3, 0.1, 0), CLOSE);
    }

    @Test
    void smoothMax_mirrorsSmoothMin() {
        assertEquals(-DensityMath.smoothMin(-0.3, -0.1, 0.2), DensityMath.smoothMax(0.3, 0.1, 0.2), CLOSE);
        assertTrue(DensityMath.smoothMax(0.2, 0.2, 0.4) >= 0.2);
    }

    @Test
    void remap_movesALineAndClampsWhenAsked() {
        assertEquals(0.5, DensityMath.remap(5, 0, 10, 0, 1, false), CLOSE);
        assertEquals(-1, DensityMath.remap(0, 0, 10, -1, 1, true), CLOSE);
        assertEquals(1.5, DensityMath.remap(12.5, 0, 10, -1, 1, false), CLOSE);
        assertEquals(1, DensityMath.remap(12.5, 0, 10, -1, 1, true), CLOSE);
        assertEquals(2, DensityMath.remap(7, 3, 3, 2, 9, false), CLOSE);
    }

    @Test
    void distance_isAcrossInUnitsOfTheRadius() {
        assertEquals(1, DensityMath.distance(3, 4, 0, 0, 5), CLOSE);
        assertEquals(0, DensityMath.distance(10, -2, 10, -2, 7), CLOSE);
    }
}
