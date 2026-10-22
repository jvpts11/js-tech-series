/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DissonanceSensesTest {

    /* A quarter of the colours' swing, where it is furthest towards red. */
    private static final double SWUNG = 1.5;
    /* A quarter of the pitch's waver, where it is highest. */
    private static final double WAVERED = 1.0;

    @Test
    void shift_isNoneBelowTheFirstBand() {
        assertEquals(0, DissonanceSenses.shift(DissonanceSenses.SIGHT_WARPS - 0.1, SWUNG));
        assertEquals(0, DissonanceSenses.shimmer(DissonanceSenses.SIGHT_WARPS - 0.1));
    }

    @Test
    void shift_swingsTheColoursFromTheFirstBand() {
        final double at = DissonanceSenses.shift(DissonanceSenses.SIGHT_WARPS, SWUNG);
        assertTrue(at > 0.1, "the colours swing at the band; got " + at);
        assertTrue(DissonanceSenses.shift(DissonanceSenses.SIGHT_WARPS, SWUNG * 3) < -0.1, "and back the other way");
        assertTrue(DissonanceSenses.shimmer(DissonanceSenses.SIGHT_WARPS) > 0, "and the view shimmers");
    }

    @Test
    void shift_growsWithTheDissonanceAndStaysOnTheLawsScale() {
        final double little = DissonanceSenses.shift(DissonanceSenses.SIGHT_WARPS, SWUNG);
        final double much = DissonanceSenses.shift(60, SWUNG);
        assertTrue(much > little, "further built up, further off: " + little + " then " + much);
        for (double seconds = 0; seconds < 12; seconds += 0.25) {
            final double shift = DissonanceSenses.shift(120, seconds);
            assertTrue(shift >= -1 && shift <= 1, "a shift the laws of light can hold: " + shift);
        }
        assertTrue(DissonanceSenses.shimmer(120) <= 1, "and a shimmer within it");
    }

    @Test
    void pitch_isAsItIsBelowTheSecondBand() {
        assertEquals(1, DissonanceSenses.pitch(DissonanceSenses.GRAVITY_DRIFTS - 0.1, WAVERED));
    }

    @Test
    void pitch_wavesFromTheSecondBand() {
        final double high = DissonanceSenses.pitch(DissonanceSenses.GRAVITY_DRIFTS, WAVERED);
        final double low = DissonanceSenses.pitch(DissonanceSenses.GRAVITY_DRIFTS, WAVERED * 3);
        assertTrue(high > 1.05 && low < 0.95, "a sound plays higher, then lower: " + high + ", " + low);
        final double worst = DissonanceSenses.pitch(120, WAVERED);
        assertTrue(worst <= 1.25 + 1.0e-9, "and never past a quarter off: " + worst);
    }
}
