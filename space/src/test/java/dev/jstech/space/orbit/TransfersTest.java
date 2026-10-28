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

class TransfersTest {

    /* The real hours of play the design gives between windows, and how close the sums have to come. */
    private static final double MARS_HOURS = 5.0;
    private static final double VENUS_HOURS = 3.7;
    private static final double JUPITER_HOURS = 2.5;
    private static final double HOURS_CLOSE = 0.1;
    private static final Orbit VENUS = new Orbit(0.723, 0.1);
    private static final Orbit JUPITER = new Orbit(5.203, 0.6);

    @Test
    void synodicTicks_bringMarsWindowsAboutEveryFiveRealHours() {
        assertEquals(MARS_HOURS, hours(Transfers.synodicTicks(SolarSystemOrbits.EARTH, SolarSystemOrbits.MARS)),
                HOURS_CLOSE);
    }

    @Test
    void synodicTicks_bringVenusAndJupiterWindowsAsTheDesignSays() {
        assertEquals(VENUS_HOURS, hours(Transfers.synodicTicks(SolarSystemOrbits.EARTH, VENUS)), HOURS_CLOSE);
        assertEquals(JUPITER_HOURS, hours(Transfers.synodicTicks(SolarSystemOrbits.EARTH, JUPITER)), HOURS_CLOSE);
    }

    @Test
    void hohmannLead_isAboutFortyFourDegreesForMars() {
        assertEquals(44.0, Math.toDegrees(Transfers.hohmannLead(SolarSystemOrbits.EARTH, SolarSystemOrbits.MARS)),
                1.0);
    }

    @Test
    void ticksToWindow_landsOnTheWindowAndRepeatsEverySynodicPeriod() {
        final Orbit earth = SolarSystemOrbits.EARTH;
        final Orbit mars = SolarSystemOrbits.MARS;
        final long wait = Transfers.ticksToWindow(earth, mars, 0);
        assertTrue(Transfers.inWindow(earth, mars, wait));
        assertEquals(1.0, Transfers.deltaVFactor(earth, mars, wait), 1e-3);
        final long next = wait + Math.round(Transfers.synodicTicks(earth, mars));
        assertTrue(Transfers.inWindow(earth, mars, next));
        assertEquals(0, Transfers.ticksToWindow(earth, mars, wait));
    }

    @Test
    void deltaVFactor_growsOutOfTheWindowToTwiceFromTheOppositePlace() {
        final Orbit earth = SolarSystemOrbits.EARTH;
        final Orbit mars = SolarSystemOrbits.MARS;
        final long window = Transfers.ticksToWindow(earth, mars, 0);
        final long opposite = window + Math.round(Transfers.synodicTicks(earth, mars) / 2);
        assertFalse(Transfers.inWindow(earth, mars, opposite));
        assertEquals(Transfers.WORST, Transfers.deltaVFactor(earth, mars, opposite), 0.01);
        final long between = window + Math.round(Transfers.synodicTicks(earth, mars) / 4);
        final double factor = Transfers.deltaVFactor(earth, mars, between);
        assertTrue(factor > 1 && factor < Transfers.WORST);
    }

    @Test
    void travelTicks_shortenWithDeltaVSpentDownToAQuarter() {
        assertEquals(SolarSystemOrbits.MARS_TRIP_TICKS, Transfers.travelTicks(SolarSystemOrbits.MARS_TRIP_TICKS,
                3_600, 3_600), 1e-9);
        assertEquals(SolarSystemOrbits.MARS_TRIP_TICKS / 2.0, Transfers.travelTicks(SolarSystemOrbits.MARS_TRIP_TICKS,
                3_600, 7_200), 1e-9);
        assertEquals(SolarSystemOrbits.MARS_TRIP_TICKS * Transfers.FASTEST, Transfers.travelTicks(
                SolarSystemOrbits.MARS_TRIP_TICKS, 3_600, 100_000), 1e-9);
    }

    @Test
    void travelTicks_refuseLessThanTheLeastDeltaV() {
        assertThrows(IllegalArgumentException.class, () -> Transfers.travelTicks(1_200, 4_100, 4_000));
    }

    private static double hours(final double ticks) {
        return ticks / OrbitScale.TICKS_PER_REAL_HOUR;
    }
}
