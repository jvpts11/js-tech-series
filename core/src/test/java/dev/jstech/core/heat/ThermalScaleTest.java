/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThermalScaleTest {

    private static final double ROOM = 288.0;
    private static final double EPSILON = 1.0e-9;

    @Test
    void warmth_isNoneAtTheAirsTemperature() {
        assertEquals(0.0, ThermalScale.warmth(ROOM, ROOM), EPSILON);
    }

    @Test
    void warmth_showsABodyAFewTensOfKelvinWarmer() {
        final double body = ThermalScale.warmth(310.0, ROOM);
        assertTrue(body > 0.15, "a person stands out against a room: " + body);
        assertTrue(body < ThermalScale.warmth(600.0, ROOM), "less than a hot plate");
    }

    @Test
    void warmth_isWholeAtTheHottestAndBeyond() {
        assertEquals(1.0, ThermalScale.warmth(ThermalScale.HOTTEST, ROOM), EPSILON);
        assertEquals(1.0, ThermalScale.warmth(5_000.0, ROOM), EPSILON);
    }

    @Test
    void warmth_fallsBelowNoneForWhatIsColder() {
        assertTrue(ThermalScale.warmth(250.0, ROOM) < 0.0, "ice is colder than the room");
        assertEquals(-1.0, ThermalScale.warmth(ThermalScale.COLDEST, ROOM), EPSILON);
        assertEquals(-1.0, ThermalScale.warmth(4.0, ROOM), EPSILON);
    }

    @Test
    void warmth_isNoneForNoTemperature() {
        assertEquals(0.0, ThermalScale.warmth(Double.NaN, ROOM), EPSILON);
        assertEquals(0.0, ThermalScale.warmth(300.0, 0.0), EPSILON);
    }

    @Test
    void apart_showsOnlyWhatDiffersFromTheAir() {
        assertFalse(ThermalScale.apart(ROOM + 1.0, ROOM));
        assertTrue(ThermalScale.apart(ROOM + ThermalScale.APART, ROOM));
        assertTrue(ThermalScale.apart(ROOM - 10.0, ROOM));
    }
}
