/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EffectPhysicsTest {

    private static final double TICK = 0.05;
    private static final double CLOSE = 1e-3;
    private static final EffectPhysics.Place VACUUM = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY, 0, 0, 0);

    @Test
    void airDensity_isSeaLevelsAtSeaLevelAndNoneInAVacuum() {
        assertEquals(EffectPhysics.SEA_AIR, EffectPhysics.airDensity(101.325, 288.15), 0.01);
        assertEquals(0, EffectPhysics.airDensity(0, 288.15));
        assertTrue(EffectPhysics.airDensity(0.6, 210) < 0.02, "Mars's thin air");
    }

    @Test
    void step_fallingSettlesAtTheTerminalSpeed() {
        final double[] v = {0, 0, 0};
        for (int i = 0; i < 400; i++) {
            EffectPhysics.step(v, EffectPhysics.Place.SEA_LEVEL, EffectPhysics.Moves.ALL, 1, 2, 0, TICK);
        }
        final double terminal = EffectPhysics.terminalSpeed(EffectPhysics.Place.SEA_LEVEL, 1, 2);
        assertEquals(-terminal, v[1], CLOSE);
    }

    @Test
    void step_whatIsLighterRisesAtBuoyancyOverDrag() {
        final double[] v = {0, 0, 0};
        for (int i = 0; i < 400; i++) {
            EffectPhysics.step(v, EffectPhysics.Place.SEA_LEVEL, EffectPhysics.Moves.ALL, 0, 1.2, 1.6, TICK);
        }
        assertEquals(1.6 / 1.2, v[1], CLOSE);
    }

    @Test
    void step_nothingRisesOrSlowsInAVacuum() {
        final double[] v = {3, 0, 0};
        for (int i = 0; i < 20; i++) {
            EffectPhysics.step(v, VACUUM, EffectPhysics.Moves.ALL, 0, 1.2, 1.6, TICK);
        }
        assertEquals(3, v[0], CLOSE);
        assertEquals(0, v[1], CLOSE);
    }

    @Test
    void step_theWindCarriesItAlong() {
        final EffectPhysics.Place windy = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY, EffectPhysics.SEA_AIR,
                4, -2);
        final double[] v = {0, 0, 0};
        for (int i = 0; i < 400; i++) {
            EffectPhysics.step(v, windy, EffectPhysics.Moves.ALL, 0, 1, 0, TICK);
        }
        assertEquals(4, v[0], CLOSE);
        assertEquals(-2, v[2], CLOSE);
    }

    @Test
    void step_movesSaysWhatActs() {
        final EffectPhysics.Place windy = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY, EffectPhysics.SEA_AIR,
                4, 0);
        final double[] v = {0, 0, 0};
        EffectPhysics.step(v, windy, new EffectPhysics.Moves(false, true, false), 1, 1, 0, TICK);
        assertEquals(0, v[0], CLOSE);
        assertEquals(0, v[1], CLOSE);
    }

    @Test
    void step_inWaterItSettlesAtOnceAndBubblesRiseAsInAir() {
        final EffectPhysics.Place water = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY, EffectPhysics.WATER,
                0, 0);
        final double[] v = {5, 0, 0};
        EffectPhysics.step(v, water, EffectPhysics.Moves.ALL, 0, 4, 2, TICK);
        assertTrue(Math.abs(v[0]) < 0.1, "stopped by the water: " + v[0]);
        assertEquals(0.5, v[1], 0.01);
    }

    @Test
    void widening_growsAsTheAirThinsUpToSix() {
        assertEquals(1, EffectPhysics.widening(EffectPhysics.Place.SEA_LEVEL), CLOSE);
        final EffectPhysics.Place thin = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY,
                EffectPhysics.SEA_AIR / 4, 0, 0);
        assertEquals(2, EffectPhysics.widening(thin), CLOSE);
        assertEquals(EffectPhysics.MOST_WIDENING, EffectPhysics.widening(VACUUM), CLOSE);
    }

    @Test
    void terminalSpeed_isEndlessWithoutAir() {
        assertTrue(Double.isInfinite(EffectPhysics.terminalSpeed(VACUUM, 1, 1)));
    }
}
