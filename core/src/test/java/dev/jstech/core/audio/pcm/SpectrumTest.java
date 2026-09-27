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

class SpectrumTest {

    private static final int RATE = 44_100;
    private static final int BANDS = 19;

    @Test
    void bands_riseWhereTheToneIs() {
        final float[] out = new float[BANDS];
        final int band = 9;
        Spectrum.bands(sine(Spectrum.middleOf(band, BANDS), 0.8), RATE, out);
        int loudest = 0;
        for (int i = 1; i < BANDS; i++) {
            if (out[i] > out[loudest]) {
                loudest = i;
            }
        }
        assertEquals(band, loudest, "a tone lifts the band it sits in the most");
        assertTrue(out[band] > 0.9F, "a loud tone fills its band; got " + out[band]);
        assertTrue(out[0] < out[band] * 0.5F, "and leaves the bass low; got " + out[0]);
    }

    @Test
    void bands_ofSilenceAreEmpty() {
        final float[] out = new float[BANDS];
        Spectrum.bands(new float[2048], RATE, out);
        for (final float level : out) {
            assertEquals(0.0F, level);
        }
    }

    @Test
    void bands_aboveHalfTheRateHearNothing() {
        final float[] out = new float[BANDS];
        Spectrum.bands(sine(1000.0, 0.8), 8000, out);
        assertEquals(0.0F, out[BANDS - 1], "an 8 kHz recording holds nothing at 14 kHz");
    }

    @Test
    void middleOf_runsFromTheLowestToTheHighest() {
        assertEquals(Spectrum.LOWEST, Spectrum.middleOf(0, BANDS), 1e-6);
        assertEquals(Spectrum.HIGHEST, Spectrum.middleOf(BANDS - 1, BANDS), 1e-6);
    }

    private static float[] sine(final double frequency, final double amplitude) {
        final float[] samples = new float[2048];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (float) (amplitude * Math.sin(2.0 * Math.PI * frequency * i / RATE));
        }
        return samples;
    }
}
