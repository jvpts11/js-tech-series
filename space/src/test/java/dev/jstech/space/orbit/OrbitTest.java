/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrbitTest {

    /* The International Space Station's real orbit, in minutes. */
    private static final double STATION_MINUTES = 92;

    @Test
    void periodTicks_ofTheEarthAreAWeekOfOverworldDays() {
        assertEquals(7 * 24_000, SolarSystemOrbits.EARTH.periodTicks(), 1e-9);
    }

    @Test
    void periodTicks_ofMarsAreItsRealYearAgainstTheEarths() {
        assertEquals(1.881, SolarSystemOrbits.MARS.periodTicks() / SolarSystemOrbits.EARTH.periodTicks(), 0.005);
    }

    @Test
    void angleAt_goesRoundOnceAYearFromWhereItBegan() {
        final Orbit mars = SolarSystemOrbits.MARS;
        final long year = Math.round(mars.periodTicks());
        // A year rounded to whole ticks is off by under half a tick: some hundred-thousandths of a radian.
        assertEquals(mars.angleAt(0), mars.angleAt(year), 1e-4);
        assertEquals(Math.PI, new Orbit(1.0, 0.0).angleAt(OrbitScale.EARTH_YEAR_TICKS / 2), 1e-9);
    }

    @Test
    void xAndZ_lieAtItsDistanceInBlocks() {
        final Orbit mars = SolarSystemOrbits.MARS;
        final double x = mars.xAt(1_000);
        final double z = mars.zAt(1_000);
        assertEquals(1.524 * OrbitScale.BLOCKS_PER_AU, Math.sqrt(x * x + z * z), 1e-6);
    }

    @Test
    void ticksOf_putALowOrbitAtALittleOverAMinute() {
        final double ticks = OrbitScale.ticksOf(STATION_MINUTES * 60);
        assertEquals(1.28, ticks / 20 / 60, 0.01);
    }

    @Test
    void orbit_refusesNoDistance() {
        assertThrows(IllegalArgumentException.class, () -> new Orbit(0, 0));
    }
}
