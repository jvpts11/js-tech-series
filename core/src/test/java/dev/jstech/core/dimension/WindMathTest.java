/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindMathTest {

    private static final double CLOSE = 1e-9;

    @Test
    void at_noneInAVacuum() {
        assertEquals(0, WindMath.at(12_345, 70, 1, false).speed(), CLOSE);
    }

    @Test
    void at_aFewMetresASecondOnAPlainDay() {
        for (long t = 0; t < 200_000; t += 997) {
            final double speed = WindMath.at(t, WindMath.GROUND, 1, true).speed();
            assertTrue(speed > 1.5 && speed < 4.5, "at " + t + ": " + speed);
        }
    }

    @Test
    void at_isTheSameForTheSameTime() {
        assertEquals(WindMath.at(5_000, 80, 1, true), WindMath.at(5_000, 80, 1, true));
    }

    @Test
    void at_strongerInStormsAndHigherUp() {
        final double plain = WindMath.at(9_000, WindMath.GROUND, 1, true).speed();
        assertEquals(plain * WindMath.STORM, WindMath.at(9_000, WindMath.GROUND, WindMath.STORM, true).speed(),
                1e-6);
        assertEquals(plain * 2, WindMath.at(9_000, WindMath.GROUND + WindMath.DOUBLES_EVERY, 1, true).speed(), 1e-6);
        assertEquals(plain, WindMath.at(9_000, WindMath.GROUND - 40, 1, true).speed(), 1e-6);
    }

    @Test
    void at_turnsOverTheDays() {
        final int first = WindMath.at(0, 70, 1, true).fromPoint();
        boolean turned = false;
        for (long t = 0; t < 24_000 * 3; t += 1_000) {
            turned |= WindMath.at(t, 70, 1, true).fromPoint() != first;
        }
        assertTrue(turned, "its way turns");
    }
}
