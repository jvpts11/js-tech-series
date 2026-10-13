/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import dev.jstech.core.audio.FrequencyResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseFilterTest {

    private static final int RATE = 44_100;
    /** A cheap speaker: sampled at 22 kHz, nothing under 150 Hz or over 7 kHz. */
    private static final FrequencyResponse CHEAP = new FrequencyResponse(22_050, 0, 150, 7_000);

    @Test
    void of_fullResponseLeavesTheRecordingAsItIs() {
        final IPcmSource source = sine(1_000, RATE);
        assertSame(source, ResponseFilter.of(source, FrequencyResponse.FULL));
    }

    @Test
    void read_midrangePassesAlmostWhole() throws IOException {
        final double ratio = rms(ResponseFilter.of(sine(1_000, RATE), CHEAP)) / rms(sine(1_000, RATE));
        assertTrue(ratio > 0.8, "a 1 kHz tone keeps most of its level; kept " + ratio);
    }

    @Test
    void read_bassIsCut() throws IOException {
        final double ratio = rms(ResponseFilter.of(sine(50, RATE), CHEAP)) / rms(sine(50, RATE));
        assertTrue(ratio < 0.2, "a 50 Hz tone is mostly lost; kept " + ratio);
    }

    @Test
    void read_trebleIsCut() throws IOException {
        // Twelve decibels an octave from 7 kHz: at 16 kHz a quarter of the level is left, or less.
        final double ratio = rms(ResponseFilter.of(sine(16_000, RATE), CHEAP)) / rms(sine(16_000, RATE));
        assertTrue(ratio < 0.3, "a 16 kHz tone is mostly lost; kept " + ratio);
    }

    @Test
    void read_coarseSamplingHoldsEachSampleForTheSpeakersRate() throws IOException {
        final IPcmSource held = ResponseFilter.of(sine(100, RATE), new FrequencyResponse(22_050, 0, 0, 0));
        final short[] out = new short[RATE / 10];
        final int read = held.read(out, 0, out.length);
        for (int i = 0; i + 1 < read; i += 2) {
            assertEquals(out[i], out[i + 1], "at 22 kHz from 44.1 kHz each sample is held for two; at " + i);
        }
    }

    @Test
    void read_fewBitsKeepOnlyTheirSteps() throws IOException {
        // Eight bits of sixteen: every sample lands on a multiple of 256.
        final IPcmSource coarse = ResponseFilter.of(sine(440, RATE), new FrequencyResponse(0, 8, 0, 0));
        final short[] out = new short[RATE / 10];
        final int read = coarse.read(out, 0, out.length);
        for (int i = 0; i < read; i++) {
            assertEquals(0, out[i] % 256, "sample " + i + " is " + out[i]);
        }
    }

    @Test
    void format_isTheRecordingsOwn() {
        assertEquals(new PcmFormat(RATE, 1), ResponseFilter.of(sine(1_000, RATE), CHEAP).format());
    }

    /** The level of what {@code source} reads, past the first tenth of a second the filters take to settle. */
    private static double rms(final IPcmSource source) throws IOException {
        final short[] out = new short[RATE];
        final int read = source.read(out, 0, out.length);
        double sum = 0;
        int counted = 0;
        for (int i = RATE / 10; i < read; i++) {
            sum += (double) out[i] * out[i];
            counted++;
        }
        return Math.sqrt(sum / counted);
    }

    /** One second of a sine at {@code hertz}, mono, at half of full scale. */
    private static IPcmSource sine(final int hertz, final int rate) {
        final short[] samples = new short[rate];
        for (int i = 0; i < rate; i++) {
            samples[i] = (short) Math.round(16_000 * Math.sin(2.0 * Math.PI * hertz * i / rate));
        }
        return new IPcmSource() {
            private int at;

            @Override
            public PcmFormat format() {
                return new PcmFormat(rate, 1);
            }

            @Override
            public int read(final short[] into, final int offset, final int length) {
                if (at >= samples.length) {
                    return -1;
                }
                final int n = Math.min(length, samples.length - at);
                System.arraycopy(samples, at, into, offset, n);
                at += n;
                return n;
            }
        };
    }
}
