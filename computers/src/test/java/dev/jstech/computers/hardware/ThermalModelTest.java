/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThermalModelTest {

    private static final double PLAINS = 20.0;

    @Test
    void ambient_followsTheBiomeAndTheNether() {
        assertEquals(20.0, ThermalModel.ambient(20, 15, 0.8, 10, false), 1e-9);
        assertEquals(38.0, ThermalModel.ambient(20, 15, 2.0, 10, false), 1e-9);
        assertEquals(8.0, ThermalModel.ambient(20, 15, 0.0, 10, false), 1e-9);
        assertEquals(48.0, ThermalModel.ambient(20, 15, 2.0, 10, true), 1e-9);
    }

    @Test
    void raw_isEightyAtALoadEqualToTheBudget() {
        assertEquals(80.0, ThermalModel.raw(PLAINS, 500, 500), 1e-9);
        assertEquals(PLAINS, ThermalModel.raw(PLAINS, 0, 0), 1e-9);
        assertTrue(Double.isInfinite(ThermalModel.raw(PLAINS, 10, 0)), "heat with nothing to take it away");
    }

    @Test
    void read_runsAtFullClockUpToEighty() {
        final ThermalModel.Reading reading = ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 395, 500));
        assertEquals(67, ThermalModel.shown(reading.celsius(), 999), "the spec's gaming PC sits at about 68 °C");
        assertEquals(1.0, reading.clock(), 1e-9);
        assertFalse(reading.slowed() || reading.throttled() || reading.trips());
    }

    @Test
    void read_fadesTheBoostByFifteenPercentToNinety() {
        assertEquals(0.925, ThermalModel.read(PLAINS, 85).clock(), 1e-9);
        assertEquals(0.85, ThermalModel.read(PLAINS, 90).clock(), 1e-9);
        assertFalse(ThermalModel.read(PLAINS, 89).throttled());
    }

    @Test
    void read_throttlesToHoldNinety() {
        // The Aesthetic case with a tower cooler: 655 W of heat on a 500 W budget, 76% of its clock at 90 °C.
        final ThermalModel.Reading hot = ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 655, 500));
        assertEquals(90, ThermalModel.shown(hot.celsius(), 999));
        assertEquals(76, hot.clockPercent());
        assertTrue(hot.throttled() && !hot.trips());
        // No cooler and no fans: 395 W on the case's 200 W, half its clock.
        assertEquals(50, ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 395, 200)).clockPercent());
    }

    @Test
    void read_isTheRacksCutAtThePlainsSurroundings() {
        // The rack's budget over its load, which the throttling at 90 °C is to within a point.
        final int cut = ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 3_900, 3_000)).clockPercent();
        assertTrue(Math.abs(cut - 77) <= 1, "budget over load is 77%, the model " + cut + "%");
    }

    @Test
    void read_neverFallsBelowAQuarterAndTripsAtAHundred() {
        final ThermalModel.Reading floored = ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 1_000, 250));
        assertEquals(ThermalModel.FLOOR, floored.clock(), 1e-9);
        assertTrue(floored.celsius() > ThermalModel.THROTTLE_AT && !floored.trips(), "past 90 at the floor");
        final ThermalModel.Reading tripped = ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, 2_000, 200));
        assertTrue(tripped.trips(), "not even a quarter holds it");
        assertTrue(ThermalModel.read(PLAINS, Double.POSITIVE_INFINITY).trips(), "heat with nothing to take it");
    }

    @Test
    void read_tripsInTheNetherWhatThePlainsOnlyThrottle() {
        final double load = 800;
        final double budget = 200;
        assertFalse(ThermalModel.read(PLAINS, ThermalModel.raw(PLAINS, load, budget)).trips());
        final double nether = ThermalModel.ambient(20, 15, 2.0, 10, true);
        assertTrue(ThermalModel.read(nether, ThermalModel.raw(nether, load, budget)).trips());
    }

    @Test
    void read_holdsTheFloorInSurroundingsAlreadyPastNinety() {
        final ThermalModel.Reading reading = ThermalModel.read(95, 95);
        assertEquals(ThermalModel.FLOOR, reading.clock(), 1e-9);
        assertFalse(reading.trips(), "95 °C surroundings with nothing running");
        assertTrue(ThermalModel.read(101, 101).trips());
    }

    @Test
    void restartBelow_isTheSettingOrFiveOverWarmerSurroundings() {
        assertEquals(60.0, ThermalModel.restartBelow(60, PLAINS), 1e-9);
        assertEquals(60.0, ThermalModel.restartBelow(60, 55), 1e-9);
        assertEquals(65.0, ThermalModel.restartBelow(60, 60), 1e-9);
        assertEquals(75.0, ThermalModel.restartBelow(60, 70), 1e-9);
    }

    @Test
    void shown_roundsAndCapsTheDegrees() {
        assertEquals(55, ThermalModel.shown(55.45, 999));
        assertEquals(999, ThermalModel.shown(Double.POSITIVE_INFINITY, 999));
        assertEquals(0, ThermalModel.shown(Double.NaN, 999));
    }
}
