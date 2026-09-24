/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaveformTest {

    @Test
    void at_staysBetweenMinusOneAndOne() {
        for (final Waveform wave : Waveform.values()) {
            for (int step = 0; step < 100; step++) {
                final double value = wave.at(step / 100.0, step / 100.0);
                assertTrue(value >= -1.0 && value <= 1.0, wave + " at " + step + " gave " + value);
            }
        }
    }

    @Test
    void at_squareIsHighForHalfItsPeriodAndPulsesForLess() {
        assertEquals(1.0, Waveform.SQUARE.at(0.49, 0));
        assertEquals(-1.0, Waveform.SQUARE.at(0.51, 0));
        assertEquals(-1.0, Waveform.PULSE_25.at(0.3, 0));
        assertEquals(1.0, Waveform.PULSE_12.at(0.1, 0));
        assertEquals(-1.0, Waveform.PULSE_12.at(0.2, 0));
    }

    @Test
    void at_smoothShapesPeakWhereTheyShould() {
        assertEquals(1.0, Waveform.TRIANGLE.at(0.5, 0), 1e-9);
        assertEquals(-1.0, Waveform.TRIANGLE.at(0.0, 0), 1e-9);
        assertEquals(1.0, Waveform.SINE.at(0.25, 0), 1e-9);
        assertEquals(0.0, Waveform.SAWTOOTH.at(0.5, 0), 1e-9);
    }

    @Test
    void byId_findsEveryShapeByItsNumberAndAnUnknownOneAsTheSquare() {
        for (final Waveform wave : Waveform.values()) {
            assertEquals(wave, Waveform.byId(wave.id()));
        }
        assertEquals(Waveform.SQUARE, Waveform.byId(99));
    }

    @Test
    void at_noiseFollowsTheRandomItIsGiven() {
        assertEquals(-1.0, Waveform.NOISE.at(0.3, 0.0));
        assertEquals(1.0, Waveform.NOISE.at(0.3, 1.0));
    }
}
