/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChartMathTest {

    @Test
    void niceMax_roundsUpToANumberAPersonWrites() {
        assertEquals(5.0, ChartMath.niceMax(3.6), 1e-9);
        assertEquals(250.0, ChartMath.niceMax(201), 1e-9);
        assertEquals(1000.0, ChartMath.niceMax(1000), 1e-9);
        assertEquals(1.0, ChartMath.niceMax(0), 1e-9);
    }

    @Test
    void slices_cutTheWholeTurnByShares() {
        final List<double[]> slices = ChartMath.slices(new double[] {52, 26, 12, 10});

        assertEquals(0.52, slices.get(0)[1], 1e-9);
        assertEquals(1.0, slices.get(3)[1], 1e-9);
        assertEquals(0.0, ChartMath.slices(new double[] {0, -3}).get(1)[1], 1e-9);
    }

    @Test
    void sliceAt_findsTheSliceUnderAPointClockwiseFromTheTop() {
        final List<double[]> slices = ChartMath.slices(new double[] {52, 26, 12, 10});

        assertEquals(0, ChartMath.sliceAt(51, 40, 50, 50, 20, slices));
        assertEquals(1, ChartMath.sliceAt(40, 55, 50, 50, 20, slices));
        assertEquals(-1, ChartMath.sliceAt(90, 50, 50, 50, 20, slices));
    }
}
