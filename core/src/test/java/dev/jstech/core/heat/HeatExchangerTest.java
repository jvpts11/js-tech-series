/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeatExchangerTest {

    @Test
    void counterflow_withAHugeWallBringsTheLesserStreamAllTheWay() {
        assertEquals(300_000.0, HeatExchanger.counterflow(600.0, 1_000.0, 300.0, 1_000.0, 1e12), 1.0);
    }

    @Test
    void counterflow_ofEqualStreamsThroughAWallOfTheirRateMovesHalf() {
        assertEquals(150_000.0, HeatExchanger.counterflow(600.0, 1_000.0, 300.0, 1_000.0, 1_000.0), 1e-6);
    }

    @Test
    void counterflow_ofUnequalStreamsFollowsTheEffectiveness() {
        final double watts = HeatExchanger.counterflow(600.0, 4_000.0, 300.0, 4_186.0, 5_000.0);
        assertEquals(675_000.0, watts, 1_000.0);
        assertTrue(watts < 4_000.0 * 300.0, "no stream comes out past the other's inlet");
    }

    @Test
    void counterflow_runsBackwardsWhenTheHotIsTheColder() {
        assertTrue(HeatExchanger.counterflow(300.0, 1_000.0, 600.0, 1_000.0, 1_000.0) < 0.0);
    }

    @Test
    void counterflow_movesNothingWithoutAStreamOrAWall() {
        assertEquals(0.0, HeatExchanger.counterflow(600.0, 0.0, 300.0, 1_000.0, 1_000.0), 1e-9);
        assertEquals(0.0, HeatExchanger.counterflow(600.0, 1_000.0, 300.0, 1_000.0, 0.0), 1e-9);
    }

    @Test
    void wall_bringsAStreamTowardsTheWallsTemperature() {
        assertEquals(200_000.0, HeatExchanger.wall(500.0, 300.0, 1_000.0, 1e12), 1.0);
        assertEquals(126_424.1, HeatExchanger.wall(500.0, 300.0, 1_000.0, 1_000.0), 0.1);
        assertTrue(HeatExchanger.wall(300.0, 500.0, 1_000.0, 1_000.0) < 0.0, "a hotter stream warms the wall");
    }
}
