/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireMathTest {

    @Test
    void oxygen_isTheGamesFireAtSeaLevelAndNoneWithTooLittle() {
        assertEquals(1, FireMath.oxygen(FireMath.OXYGEN_AT_SEA_LEVEL, 0.21), 1.0e-12);
        assertEquals(0, FireMath.oxygen(FireMath.OXYGEN_FIRE_NEEDS - 0.1, 0.21), 1.0e-12);
        assertEquals(2, FireMath.oxygen(FireMath.OXYGEN_AT_SEA_LEVEL * 2, 0.4), 1.0e-12);
    }

    @Test
    void oxygen_halfOrMoreIsAnInfernoAndHasAMost() {
        final double rich = FireMath.oxygen(34, 0.45);
        final double inferno = FireMath.oxygen(34, 0.5);
        assertEquals(rich * 3, inferno, 1.0e-12);
        assertEquals(5 * 3, FireMath.oxygen(1_000, 1), 1.0e-12);
    }

    @Test
    void wind_speedsTheFireDownwindAndSlowsItUpwind() {
        assertEquals(1, FireMath.wind(0, 0, 1, 0), 1.0e-12);
        assertEquals(2, FireMath.wind(5, 0, 1, 0), 1.0e-12);
        assertEquals(0.25, FireMath.wind(5, 0, -1, 0), 1.0e-12);
        assertEquals(1, FireMath.wind(5, 0, 0, 1), 1.0e-12);
        assertEquals(1, FireMath.wind(20, 20, 0, 0), 1.0e-12);
    }

    @Test
    void leap_onlyInAStrongWindAndNeverFar() {
        assertEquals(0, FireMath.leap(5.9));
        assertEquals(1, FireMath.leap(6));
        assertEquals(3, FireMath.leap(40));
    }

    @Test
    void heatFrom_fallsWithTheDistanceAndAddsUpToAMost() {
        assertEquals(FireMath.FLAME_RISE, FireMath.heatFrom(0), 1.0e-12);
        assertTrue(FireMath.heatFrom(3) < FireMath.heatFrom(1));
        assertEquals(293 + FireMath.MOST_RISE, FireMath.heated(293, 10_000), 1.0e-12);
        assertEquals(293, FireMath.heated(293, -5), 1.0e-12);
    }

    @Test
    void smokeFrom_risesAndThins() {
        assertEquals(0, FireMath.smokeFrom(-1, 0), 1.0e-12);
        assertEquals(1, FireMath.smokeFrom(0, 0), 1.0e-12);
        assertTrue(FireMath.smokeFrom(6, 0) < FireMath.smokeFrom(2, 0));
        assertTrue(FireMath.smokeFrom(2, 4) < FireMath.smokeFrom(2, 0));
    }

    @Test
    void obscuration_growsTowardAllTheLight() {
        assertEquals(0, FireMath.obscuration(0), 1.0e-12);
        assertEquals(1 - Math.exp(-1), FireMath.obscuration(1), 1.0e-12);
        assertTrue(FireMath.obscuration(10) > 0.99);
    }
}
