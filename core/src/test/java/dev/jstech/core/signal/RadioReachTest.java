/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadioReachTest {

    @Test
    void reach_isThePowerInOpenAir() {
        assertEquals(64, RadioReach.reach(64, 0));
    }

    @Test
    void reach_losesTheCostOfEachWall() {
        assertEquals(64 - 3 * RadioReach.OBSTACLE_COST, RadioReach.reach(64, 3));
    }

    @Test
    void reach_neverFallsBelowNothing() {
        assertEquals(0, RadioReach.reach(8, 100));
    }

    @Test
    void heard_withinReachWhateverTheDistance() {
        assertTrue(RadioReach.heard(32, 32, 0));
        assertTrue(RadioReach.heard(32, 1, 0));
        assertFalse(RadioReach.heard(32, 32.5, 0));
    }

    @Test
    void heard_notThroughTooMuchWall() {
        assertTrue(RadioReach.heard(32, 10, 5));
        assertFalse(RadioReach.heard(32, 10, 6));
    }

    @Test
    void heard_nothingFromNoPower() {
        assertFalse(RadioReach.heard(0, 0, 0));
    }

    @Test
    void bars_fromOneAtTheEdgeToFourWithMostToSpare() {
        assertEquals(1, RadioReach.bars(40, 40, 0));
        assertEquals(2, RadioReach.bars(40, 25, 0));
        assertEquals(3, RadioReach.bars(40, 15, 0));
        assertEquals(RadioReach.MOST_BARS, RadioReach.bars(40, 5, 0));
    }

    @Test
    void bars_noneOutOfReach() {
        assertEquals(0, RadioReach.bars(40, 41, 0));
    }
}
