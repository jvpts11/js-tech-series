/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BallisticsTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void airDensity_isTheSeaLevelsAtSeaLevel() {
        assertEquals(Ballistics.SEA_LEVEL_AIR, Ballistics.airDensity(101.325, 15.0), 1.0e-6);
    }

    @Test
    void airDensity_isNothingInAVacuum() {
        assertEquals(0.0, Ballistics.airDensity(0.0, -120.0), CLOSE);
    }

    @Test
    void airDensity_isHigherInColdAirAndLowerInThinAir() {
        assertTrue(Ballistics.airDensity(101.325, -30.0) > Ballistics.airDensity(101.325, 15.0));
        assertTrue(Ballistics.airDensity(50.0, 15.0) < Ballistics.airDensity(101.325, 15.0));
    }

    @Test
    void dragScale_keepsEverythingWithNoMassOrNoAir() {
        assertEquals(1.0, Ballistics.dragScale(40.0, 1.225, 0.001, 0.0, 0.05), CLOSE);
        assertEquals(1.0, Ballistics.dragScale(40.0, 0.0, 0.001, 0.01, 0.05), CLOSE);
    }

    @Test
    void dragScale_isTheExactSumOfDragGrowingWithSpeedSquared() {
        final double k = 0.5 * 1.225 * 0.001 / 0.01;
        final double speed = 40.0;
        final double after = speed * Ballistics.dragScale(speed, 1.225, 0.001, 0.01, 1.0);
        assertEquals(speed / (1.0 + k * speed), after, CLOSE);
    }

    @Test
    void dragScale_neverTurnsTheSpeedRound() {
        final double scale = Ballistics.dragScale(10_000.0, Ballistics.WATER, 1.0, 0.001, 1.0);
        assertTrue(scale > 0.0 && scale < 1.0);
    }

    @Test
    void throughScale_keepsTheSquareRootOfWhatIsLeft() {
        assertEquals(Math.sqrt(0.5), Ballistics.throughScale(2.0, 1.0), CLOSE);
        assertEquals(0.0, Ballistics.throughScale(2.0, 0.0), CLOSE);
        assertEquals(0.0, Ballistics.throughScale(0.0, 0.0), CLOSE);
    }
}
