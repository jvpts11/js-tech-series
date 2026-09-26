/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import dev.jstech.core.audio.StereoSide;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StereoSelectTest {

    private static final short[] STEREO = {10, -10, 20, -20, 30, -30, 40, -40, 50, -50};

    @Test
    void of_bothSidesReadsTheRecordingAsItIs() {
        final IPcmSource source = new Samples(STEREO, 2);
        assertSame(source, StereoSelect.of(source, StereoSide.BOTH));
    }

    @Test
    void of_monoRecordingHasNoSideToChoose() {
        final IPcmSource source = new Samples(new short[]{1, 2, 3}, 1);
        assertSame(source, StereoSelect.of(source, StereoSide.LEFT));
    }

    @Test
    void read_leftSideIsTheFirstOfEachPair() throws IOException {
        assertArrayEquals(new short[]{10, 20, 30, 40, 50}, readAll(StereoSelect.of(new Samples(STEREO, 2),
                StereoSide.LEFT), 3));
    }

    @Test
    void read_rightSideIsTheSecondOfEachPair() throws IOException {
        assertArrayEquals(new short[]{-10, -20, -30, -40, -50}, readAll(StereoSelect.of(new Samples(STEREO, 2),
                StereoSide.RIGHT), 2));
    }

    @Test
    void format_oneSideIsMonoAtTheSameRate() {
        final PcmFormat format = StereoSelect.of(new Samples(STEREO, 2), StereoSide.RIGHT).format();
        assertEquals(1, format.channels());
        assertEquals(44_100, format.sampleRate());
    }

    @Test
    void read_sourceStoppingBetweenTheSidesOfOneMomentLosesNothing() throws IOException {
        final IPcmSource odd = new Samples(STEREO, 2, 3);
        assertArrayEquals(new short[]{10, 20, 30, 40, 50}, readAll(StereoSelect.of(odd, StereoSide.LEFT), 4));
    }

    private static short[] readAll(final IPcmSource source, final int chunk) throws IOException {
        final short[] out = new short[64];
        int total = 0;
        while (true) {
            final int read = source.read(out, total, chunk);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return Arrays.copyOf(out, total);
    }

    /** Samples held in memory, handed out at most {@code limit} at a time, as a file read in pieces would. */
    private static final class Samples implements IPcmSource {

        private final short[] samples;
        private final PcmFormat format;
        private final int limit;
        private int at;

        Samples(final short[] samples, final int channels) {
            this(samples, channels, Integer.MAX_VALUE);
        }

        Samples(final short[] samples, final int channels, final int limit) {
            this.samples = samples;
            this.format = new PcmFormat(44_100, channels);
            this.limit = limit;
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
            final int n = Math.min(Math.min(length, limit), samples.length - at);
            System.arraycopy(samples, at, into, offset, n);
            at += n;
            return n;
        }
    }
}
