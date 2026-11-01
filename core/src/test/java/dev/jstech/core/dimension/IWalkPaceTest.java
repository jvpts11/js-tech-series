/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IWalkPaceTest {

    private static final double CLOSE = 1e-3;

    @Test
    void of_isTheGamesOwnPaceUnderTheOverworldsPull() {
        assertEquals(1, IWalkPace.of(1), CLOSE);
    }

    @Test
    void of_isTheSquareRootOfThePull() {
        assertEquals(0.406, IWalkPace.of(0.165), CLOSE, "the Moon's");
        assertEquals(0.616, IWalkPace.of(0.38), CLOSE, "Mars'");
        assertEquals(2, IWalkPace.of(4), CLOSE, "a world four times as heavy");
    }

    @Test
    void of_neverStopsTheSwingWhereNothingPulls() {
        assertEquals(IWalkPace.SLOWEST, IWalkPace.of(0), CLOSE);
        assertEquals(IWalkPace.SLOWEST, IWalkPace.of(-1), CLOSE);
        assertTrue(IWalkPace.of(0.1) > IWalkPace.SLOWEST, "a little pull swings faster than none");
    }
}
