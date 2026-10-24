/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrcHeatTest {

    private static final double CLOSE = 1e-6;
    private static final double AIR = 293.15;
    private static final double CAPACITY = 200_000;

    @Test
    void second_settlesWhenTheCoolingIsEnough() {
        final CrcHeat.Step step = CrcHeat.second(AIR, CAPACITY, 5_000, new CrcHeat.Way(5_000, AIR),
                CrcHeat.Way.NONE, CrcHeat.Way.NONE);
        assertEquals(AIR, step.kelvin(), CLOSE);
        assertEquals(5_000, step.air(), CLOSE);
    }

    @Test
    void second_risesByWhatTheCoolingCannotTake() {
        final CrcHeat.Step step = CrcHeat.second(AIR, CAPACITY, 85_000, new CrcHeat.Way(5_000, AIR),
                CrcHeat.Way.NONE, CrcHeat.Way.NONE);
        assertEquals(AIR + 80_000 / CAPACITY, step.kelvin(), CLOSE);
        assertFalse(step.burns());
    }

    @Test
    void second_neverCoolsBelowWhatEachWayCoolsTo() {
        final double hotWater = 333.0;
        final CrcHeat.Step step = CrcHeat.second(340.0, CAPACITY, 0, CrcHeat.Way.NONE,
                new CrcHeat.Way(2_000_000, hotWater), CrcHeat.Way.NONE);
        assertEquals(hotWater, step.kelvin(), CLOSE);
        assertEquals(7.0 * CAPACITY, step.water(), CLOSE);
    }

    @Test
    void second_coolsByTheAirThenTheWaterThenThePipe() {
        final CrcHeat.Step step = CrcHeat.second(350.0, CAPACITY, 60_000, new CrcHeat.Way(10_000, AIR),
                new CrcHeat.Way(30_000, 333.0), new CrcHeat.Way(50_000, 300.0));
        assertEquals(10_000, step.air(), CLOSE);
        assertEquals(30_000, step.water(), CLOSE);
        assertEquals(50_000, step.pipe(), CLOSE);
        assertEquals(350.0 - 30_000 / CAPACITY, step.kelvin(), CLOSE);
    }

    @Test
    void second_burnsPastAHundredDegrees() {
        assertTrue(CrcHeat.second(CrcHeat.BURNS_AT, CAPACITY, 1_000, CrcHeat.Way.NONE, CrcHeat.Way.NONE,
                CrcHeat.Way.NONE).burns());
        assertThrows(IllegalArgumentException.class, () -> CrcHeat.second(AIR, 0, 0, CrcHeat.Way.NONE,
                CrcHeat.Way.NONE, CrcHeat.Way.NONE));
    }
}
