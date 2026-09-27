/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class PcmTapTest {

    private static final int RATE = 1000;

    @Test
    void heard_isWhatTheClockSaysIsPlayingNotTheNewestRead() throws IOException {
        final long[] now = {0L};
        final PcmTap tap = new PcmTap(ramp(new PcmFormat(RATE, 1)), () -> now[0]);
        final float[] out = new float[10];
        assertFalse(tap.heard(out), "nothing is heard before anything is read");
        tap.read(new short[2000], 0, 2000);
        now[0] = 500L;
        assertTrue(tap.heard(out));
        // Half a second in at a thousand samples a second, the ramp is at its 500th sample.
        assertEquals(499.0F / 32768.0F, out[9], 1e-6F);
        assertEquals(490.0F / 32768.0F, out[0], 1e-6F);
    }

    @Test
    void heard_mixesTheChannelsToOne() throws IOException {
        final long[] now = {0L};
        final PcmTap tap = new PcmTap(new IPcmSource() {
            @Override
            public PcmFormat format() {
                return new PcmFormat(RATE, 2);
            }

            @Override
            public int read(final short[] into, final int offset, final int length) {
                for (int i = 0; i < length; i++) {
                    into[offset + i] = (short) (i % 2 == 0 ? 1000 : 3000);
                }
                return length;
            }
        }, () -> now[0]);
        tap.read(new short[400], 0, 400);
        now[0] = 100L;
        final float[] out = new float[1];
        tap.heard(out);
        assertEquals(2000.0F / 32768.0F, out[0], 1e-6F);
    }

    @Test
    void read_passesTheSamplesThroughUntouched() throws IOException {
        final PcmTap tap = new PcmTap(ramp(new PcmFormat(RATE, 1)), () -> 0L);
        final short[] into = new short[5];
        assertEquals(5, tap.read(into, 0, 5));
        assertEquals(4, into[4]);
    }

    /* Samples counting up from nought, as many as are asked for. */
    private static IPcmSource ramp(final PcmFormat format) {
        return new IPcmSource() {
            private short next;

            @Override
            public PcmFormat format() {
                return format;
            }

            @Override
            public int read(final short[] into, final int offset, final int length) {
                for (int i = 0; i < length; i++) {
                    into[offset + i] = next++;
                }
                return length;
            }
        };
    }
}
