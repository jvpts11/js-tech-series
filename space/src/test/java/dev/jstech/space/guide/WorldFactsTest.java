/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldFactsTest {

    private static final long TICKS_A_SECOND = 20L;

    @Test
    void played_roundsTheMinutesIntoTheHourRatherThanReadingSixty() {
        assertEquals("2 h 0 min", WorldFacts.played(7_180 * TICKS_A_SECOND));
        assertEquals("1 h 30 min", WorldFacts.played(5_400 * TICKS_A_SECOND));
        assertEquals("59 min 59 s", WorldFacts.played(3_599 * TICKS_A_SECOND));
    }
}
