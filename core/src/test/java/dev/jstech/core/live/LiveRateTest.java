/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiveRateTest {

    @Test
    void framesPerSecond_isTenUpToSixteenBlocks() {
        assertEquals(10, LiveRate.framesPerSecond(0));
        assertEquals(10, LiveRate.framesPerSecond(16 * 16));
    }

    @Test
    void framesPerSecond_isTwoUpToThirtyTwoBlocks() {
        assertEquals(2, LiveRate.framesPerSecond(16 * 16 + 0.01));
        assertEquals(2, LiveRate.framesPerSecond(32 * 32));
    }

    @Test
    void framesPerSecond_isNoneFurtherAway() {
        assertEquals(0, LiveRate.framesPerSecond(32 * 32 + 0.01));
        assertFalse(LiveRate.shows(40 * 40));
        assertTrue(LiveRate.shows(20 * 20));
    }

    @Test
    void due_drawsANewPictureAtOnce() {
        assertTrue(LiveRate.due(LiveRate.NEVER, 0L, 2));
        assertFalse(LiveRate.due(LiveRate.NEVER, 0L, 0), "a picture not drawn at all is never due");
    }

    @Test
    void due_waitsOneFrameAtTheRateGiven() {
        assertFalse(LiveRate.due(0L, 99_999_999L, 10));
        assertTrue(LiveRate.due(0L, 100_000_000L, 10));
        assertFalse(LiveRate.due(0L, 499_999_999L, 2));
        assertTrue(LiveRate.due(0L, 500_000_000L, 2));
    }
}
