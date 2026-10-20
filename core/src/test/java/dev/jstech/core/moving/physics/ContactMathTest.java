/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContactMathTest {

    @Test
    void reducedMass_ofABodyAgainstTheGroundIsItsOwn() {
        assertEquals(500, ContactMath.reducedMass(500, Double.POSITIVE_INFINITY), 0);
        assertEquals(500, ContactMath.reducedMass(Double.POSITIVE_INFINITY, 500), 0);
        assertEquals(250, ContactMath.reducedMass(500, 500), 1.0e-12);
    }

    @Test
    void energy_isHalfTheMassTimesTheSpeedSquared() {
        assertEquals(0.5 * 2000 * 100, ContactMath.energy(2000, 10), 1.0e-9);
    }

    @Test
    void breaks_whenTheBlowIsMoreThanTheBlockTakes() {
        // A block of 300 kPa takes 300 kJ a cubic metre.
        assertFalse(ContactMath.breaks(299_000, 300, 1));
        assertTrue(ContactMath.breaks(301_000, 300, 1));
        assertTrue(ContactMath.breaks(160_000, 300, 0.5));
    }

    @Test
    void grip_ofIceIsLittleAndOfStoneItsOwn() {
        assertEquals(0.6, ContactMath.grip(0.6), 1.0e-12);
        assertTrue(ContactMath.grip(0.98) < 0.05);
        assertEquals(0, ContactMath.grip(1.0), 0);
    }

    @Test
    void grip_betweenTwoSidesIsTheirGeometricMean() {
        assertEquals(0.3, ContactMath.grip(0.9, 0.1), 1.0e-12);
        assertEquals(0, ContactMath.grip(0.6, 0), 0);
    }

    @Test
    void bounce_isLittleAndNoneWhenSlow() {
        assertEquals(0, ContactMath.bounce(0.1), 0);
        assertTrue(ContactMath.bounce(5) > 0 && ContactMath.bounce(5) < 0.5);
    }

    @Test
    void spring_holdsByHowFarPressedAndHowFastAndNeverPulls() {
        assertEquals(1000 * 0.1 + 100 * 0.5, ContactMath.spring(0.1, 0.5, 1000, 100), 1.0e-9);
        assertEquals(0, ContactMath.spring(0, -10, 1000, 100), 0);
    }

    @Test
    void friction_stopsASlowSlideAndHoldsAFastOneByTheGripTimesTheLoad() {
        assertEquals(-100, ContactMath.friction(0.5, 10, 0.05, 0.6, 10_000), 1.0e-9);
        assertEquals(-0.6 * 1000, ContactMath.friction(30, 1000, 0.05, 0.6, 1000), 1.0e-9);
        assertEquals(0.6 * 1000, ContactMath.friction(-30, 1000, 0.05, 0.6, 1000), 1.0e-9);
    }
}
