/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeltPathTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void open_runsOverAndUnderPulleysOfOneSizeStraight() {
        final BeltPath belt = BeltPath.open(0, 0, 1, 4, 0, 1).orElseThrow();
        assertEquals(1, belt.fromAy(), EPSILON, "one run over the top");
        assertEquals(1, belt.toAy(), EPSILON);
        assertEquals(-1, belt.fromBy(), EPSILON, "the other under");
        assertEquals(0, belt.fromAx(), EPSILON);
        assertEquals(4, belt.toAx(), EPSILON);
        assertEquals(4, belt.runLength(), EPSILON);
    }

    @Test
    void open_touchesEachPulleyAtRightAnglesToItsRun() {
        final BeltPath belt = BeltPath.open(0, 0, 2, 5, 1, 0.5).orElseThrow();
        final double runX = belt.toAx() - belt.fromAx();
        final double runY = belt.toAy() - belt.fromAy();
        // The run is a tangent: at right angles to the radius where it leaves the first pulley.
        assertEquals(0, runX * belt.fromAx() + runY * belt.fromAy(), 1.0e-9);
        assertEquals(2, Math.hypot(belt.fromAx(), belt.fromAy()), EPSILON, "on the first pulley's rim");
        assertEquals(0.5, Math.hypot(belt.toAx() - 5, belt.toAy() - 1), EPSILON, "and on the second's");
    }

    @Test
    void open_runsNoBeltBetweenPulleysOneInsideTheOther() {
        assertTrue(BeltPath.open(0, 0, 2, 0.5, 0, 1).isEmpty());
        assertTrue(BeltPath.open(0, 0, 1, 0, 0, 1).isEmpty());
    }

    @Test
    void travel_isTheRimsSpeedOverTheTime() {
        assertEquals(2 * Math.PI, BeltPath.travel(60, 1, 1), EPSILON, "a turn a second of a pulley a block round");
        assertEquals(-Math.PI, BeltPath.travel(-30, 1, 1), EPSILON, "the other way at half the speed");
    }
}
