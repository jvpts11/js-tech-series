/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClimateMathTest {

    private static final double CLOSE = 1e-9;

    @Test
    void celsius_givesTheGamesBiomesTheirRealTemperatures() {
        assertEquals(14, ClimateMath.celsius(0.8), CLOSE);
        assertEquals(-10, ClimateMath.celsius(0), CLOSE);
        assertEquals(26, ClimateMath.celsius(2.0), CLOSE);
        assertTrue(ClimateMath.celsius(0.25) < 0, "a taiga's snow");
    }

    @Test
    void gameTemperature_readsCelsiusBack() {
        for (double t = 0; t <= 2; t += 0.05) {
            assertEquals(t, ClimateMath.gameTemperature(ClimateMath.celsius(t)), 1e-9);
        }
    }

    @Test
    void rain_isTheDownfallInMillimetresAYear() {
        assertEquals(800, ClimateMath.rain(0.4), CLOSE);
        assertEquals(0, ClimateMath.rain(0), CLOSE);
        assertEquals(0.4, ClimateMath.downfall(800), CLOSE);
    }

    @Test
    void seasons_swingLittleInTheWetTropicsAndMuchInTheCold() {
        assertEquals(2, ClimateMath.seasons(0.95, 0.9), CLOSE);
        assertEquals(10, ClimateMath.seasons(0.8, 0.4), CLOSE);
        assertEquals(14, ClimateMath.seasons(0.0, 0.5), CLOSE);
    }

    @Test
    void temperature_swingsWithTheYearAndCoolsWithHeight() {
        assertEquals(24, ClimateMath.temperature(14, 10, 63, 42, 112), 1e-9);
        assertEquals(4, ClimateMath.temperature(14, 10, 63, 98, 112), 1e-9);
        assertEquals(14 - 1000 * ClimateMath.LAPSE, ClimateMath.temperature(14, 10, 1063, 14, 112), 1e-9);
        assertEquals(14 - 1000 * ClimateMath.LAPSE, ClimateMath.temperature(14, 1063), 1e-9);
        assertEquals(14, ClimateMath.temperature(14, 10), 1e-9);
    }
}
