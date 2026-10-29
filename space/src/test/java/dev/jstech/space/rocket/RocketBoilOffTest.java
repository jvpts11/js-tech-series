/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RocketBoilOffTest {

    private static final double CLOSE = 1e-9;
    /* Liquid oxygen's heat of boiling, in joules a kilogram. */
    private static final double OXYGEN = 213_100;

    @Test
    void kilograms_areTheHeatLeakingInOverTheHeatOfBoiling() {
        // A hundred watts a kelvin, two hundred kelvin warmer, an hour: 72 MJ boil 337.9 kg of oxygen.
        assertEquals(100.0 * 200 * 3_600 / OXYGEN, RocketBoilOff.kilograms(100, 200, OXYGEN, 3_600), CLOSE);
    }

    @Test
    void kilograms_areNoneWhereThePlaceIsNoWarmerOrNoTimeGoesBy() {
        assertEquals(0, RocketBoilOff.kilograms(100, 0, OXYGEN, 3_600), CLOSE);
        assertEquals(0, RocketBoilOff.kilograms(100, -50, OXYGEN, 3_600), CLOSE);
        assertEquals(0, RocketBoilOff.kilograms(100, 200, OXYGEN, 0), CLOSE);
        assertEquals(0, RocketBoilOff.kilograms(0, 200, OXYGEN, 3_600), CLOSE);
        assertEquals(0, RocketBoilOff.kilograms(100, 200, Double.NaN, 3_600), CLOSE);
    }

    @Test
    void surface_isTheSideOfTheTank() {
        assertEquals(Math.PI * 3 * 18, RocketBoilOff.surface(3, 18), CLOSE);
        assertEquals(Math.PI * 5, RocketBoilOff.surface(5, 1), CLOSE);
    }
}
