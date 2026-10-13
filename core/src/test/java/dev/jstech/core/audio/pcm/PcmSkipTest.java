/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PcmSkipTest {

    /** At a thousand samples a second a millisecond is one sample, which keeps the arithmetic in sight. */
    private static final int RATE = 1000;

    @Test
    void from_noTimeReadsTheRecordingAsItIs() {
        final IPcmSource source = new Samples(new short[] {1, 2, 3}, 1);
        assertSame(source, PcmSkip.from(source, 0));
    }

    @Test
    void read_monoStartsThatManyMillisecondsIn() throws IOException {
        assertArrayEquals(new short[] {4, 5, 6}, readAll(PcmSkip.from(new Samples(new short[] {1, 2, 3, 4, 5, 6}, 1),
                3)));
    }

    @Test
    void read_stereoSkipsWholeMomentsNotSingleSamples() throws IOException {
        final short[] pairs = {1, -1, 2, -2, 3, -3, 4, -4};
        assertArrayEquals(new short[] {3, -3, 4, -4}, readAll(PcmSkip.from(new Samples(pairs, 2), 2)));
    }

    @Test
    void read_pastTheEndIsTheEnd() throws IOException {
        final IPcmSource skipped = PcmSkip.from(new Samples(new short[] {1, 2}, 1), 50);
        assertEquals(-1, skipped.read(new short[4], 0, 4));
    }

    private static short[] readAll(final IPcmSource source) throws IOException {
        short[] out = new short[0];
        final short[] buffer = new short[2];
        int read;
        while ((read = source.read(buffer, 0, buffer.length)) >= 0) {
            final int at = out.length;
            out = Arrays.copyOf(out, at + read);
            System.arraycopy(buffer, 0, out, at, read);
        }
        return out;
    }

    /** Samples held in memory, given out a few at a time as a decoder does. */
    private static final class Samples implements IPcmSource {

        private final short[] samples;
        private final PcmFormat format;
        private int at;

        Samples(final short[] samples, final int channels) {
            this.samples = samples;
            this.format = new PcmFormat(RATE, channels);
        }

        @Override
        public PcmFormat format() {
            return format;
        }

        @Override
        public int read(final short[] into, final int offset, final int length) {
            if (at >= samples.length) {
                return -1;
            }
            final int count = Math.min(Math.min(length, 3), samples.length - at);
            System.arraycopy(samples, at, into, offset, count);
            at += count;
            return count;
        }
    }
}
