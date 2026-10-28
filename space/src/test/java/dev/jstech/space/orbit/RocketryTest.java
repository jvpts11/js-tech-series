/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RocketryTest {

    /* A methalox engine's specific impulse at sea level, in seconds. */
    private static final double ISP = 330;

    @Test
    void deltaV_isTsiolkovskys() {
        assertEquals(ISP * OrbitScale.STANDARD_GRAVITY * Math.log(10), Rocketry.deltaV(ISP, 100, 10), 1e-9);
        assertEquals(0, Rocketry.deltaV(ISP, 10, 10), 1e-9);
    }

    @Test
    void thrustToWeight_isThrustOverMassTimesTheLocalPull() {
        assertEquals(2.0, Rocketry.thrustToWeight(2 * 1_000 * OrbitScale.STANDARD_GRAVITY, 1_000, 1.0), 1e-9);
        assertEquals(2.0 / 0.165, Rocketry.thrustToWeight(2 * 1_000 * OrbitScale.STANDARD_GRAVITY, 1_000, 0.165),
                1e-9);
    }

    @Test
    void lifts_onlyWhenThrustBeatsWeightSoTheMoonIsEasier() {
        final double thrust = 0.5 * 1_000 * OrbitScale.STANDARD_GRAVITY;
        assertFalse(Rocketry.lifts(thrust, 1_000, 1.0));
        assertTrue(Rocketry.lifts(thrust, 1_000, 0.165));
    }

    @Test
    void deltaV_refusesAStageLighterFullThanEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Rocketry.deltaV(ISP, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> Rocketry.deltaV(ISP, 5, 0));
    }

    @Test
    void legs_toTheMoonCostLessThanToMarsSurface() {
        final double moon = Rocketry.SURFACE_TO_LOW_ORBIT + Rocketry.LOW_ORBIT_TO_MOON_ORBIT
                + Rocketry.MOON_ORBIT_TO_SURFACE;
        assertEquals(15_200, moon, 1e-9);
        assertTrue(Rocketry.LOW_ORBIT_TO_MARS_TRANSFER < Rocketry.LOW_ORBIT_TO_MOON_ORBIT);
    }
}
