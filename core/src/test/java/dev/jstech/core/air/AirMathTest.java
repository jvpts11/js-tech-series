/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.air;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AirMathTest {

    private static final double CLOSE = 1e-9;

    @Test
    void partial_isTheShareOfThePressure() {
        assertEquals(21.0, AirMath.partial(0.21, 100), CLOSE);
        assertEquals(0, AirMath.partial(0.21, 0), CLOSE, "nothing in a vacuum");
    }

    @Test
    void breathable_followsTheOxygensPartialPressure() {
        assertTrue(AirMath.breathable(0.21, AirMath.SEA_LEVEL), "the overworld's air");
        assertFalse(AirMath.breathable(0.21, 50), "the same air at half the pressure, high on a mountain");
        assertTrue(AirMath.breathable(1.0, 20), "pure oxygen at a fifth of an atmosphere, as in a suit");
        assertFalse(AirMath.breathable(0.0, 1_000), "no oxygen at any pressure");
    }

    @Test
    void between_runsStraightBetweenTwoHeights() {
        assertEquals(50, AirMath.between(5, 0, 0, 10, 100), CLOSE);
        assertEquals(0, AirMath.between(-5, 0, 0, 10, 100), CLOSE, "below, the lower");
        assertEquals(100, AirMath.between(20, 0, 0, 10, 100), CLOSE, "above, the higher");
    }
}
