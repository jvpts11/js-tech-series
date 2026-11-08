/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeatCurveTest {

    @Test
    void at_closesInOnItsTargetAsAWarmBodyDoes() {
        final HeatCurve curve = new HeatCurve(20, 80, 0);
        assertEquals(20.0, curve.at(0), 1e-9);
        assertEquals(20 + 60 * (1 - Math.exp(-1)), curve.at(HeatCurve.TAU_TICKS), 1e-9);
        assertTrue(curve.at(HeatCurve.TAU_TICKS * 10) > 79.99, "nearly there after ten times as long");
    }

    @Test
    void at_neverReadsBeforeItSetOut() {
        assertEquals(20.0, new HeatCurve(20, 80, 100).at(50), 1e-9);
    }

    @Test
    void aim_setsOutFromWhereItStands() {
        final HeatCurve curve = new HeatCurve(20, 80, 0);
        final double halfway = curve.at(HeatCurve.TAU_TICKS);
        curve.aim(HeatCurve.TAU_TICKS, 20);
        assertEquals(halfway, curve.at(HeatCurve.TAU_TICKS), 1e-9);
        assertEquals(20.0, curve.target(), 1e-9);
        assertTrue(curve.at(HeatCurve.TAU_TICKS * 2) < halfway, "cooling once aimed down");
    }

    @Test
    void aim_atTheSameTargetChangesNothing() {
        final HeatCurve curve = new HeatCurve(20, 80, 0);
        curve.aim(300, 80);
        assertEquals(0L, curve.since());
        assertEquals(20.0, curve.from(), 1e-9);
    }

    @Test
    void at_climbsWithoutEndForHeatNothingTakesAway() {
        final HeatCurve curve = new HeatCurve(20, Double.POSITIVE_INFINITY, 0);
        assertEquals(80.0, curve.at(HeatCurve.TAU_TICKS), 1e-9);
        curve.aim(HeatCurve.TAU_TICKS, 20);
        assertEquals(80.0, curve.at(HeatCurve.TAU_TICKS), 1e-9, "aimed down from where it climbed to");
    }

    @Test
    void settle_standsAtTheTarget() {
        final HeatCurve curve = new HeatCurve(20, 70, 0);
        curve.settle(5);
        assertEquals(70.0, curve.at(5), 1e-9);
        assertEquals(70.0, curve.at(5_000), 1e-9);
    }
}
