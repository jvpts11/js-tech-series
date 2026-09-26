/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

/**
 * What a piece of sound hardware can reproduce: the rate it samples at, at most, how many bits it keeps of each
 * sample, and the low and high frequencies it cuts. An early sound card samples coarsely in few bits; a cheap speaker
 * loses the bass and the treble; good hardware plays a recording whole.
 *
 * @param maxSampleRate the highest rate it samples at, in hertz; 0 for no limit
 * @param bits          how many bits of each sample it keeps, 1 to 16; 0 for all of them
 * @param lowCutHz      below this it loses the sound, in hertz; 0 for none
 * @param highCutHz     above this it loses the sound, in hertz; 0 for none
 */
public record FrequencyResponse(int maxSampleRate, int bits, int lowCutHz, int highCutHz) {

    /** Hardware that plays a recording as it was made. */
    public static final FrequencyResponse FULL = new FrequencyResponse(0, 0, 0, 0);

    public FrequencyResponse {
        if (maxSampleRate < 0 || lowCutHz < 0 || highCutHz < 0) {
            throw new IllegalArgumentException("no negative rate or frequency: " + maxSampleRate + ", " + lowCutHz
                    + ", " + highCutHz);
        }
        if (bits < 0 || bits > 16) {
            throw new IllegalArgumentException("from 1 to 16 bits a sample, or 0 for all: " + bits);
        }
        if (lowCutHz > 0 && highCutHz > 0 && lowCutHz >= highCutHz) {
            throw new IllegalArgumentException("the low cut must sit below the high cut: " + lowCutHz + " and "
                    + highCutHz);
        }
    }

    /** Whether it plays a recording whole, with nothing to take away. */
    public boolean full() {
        return maxSampleRate == 0 && bits == 0 && lowCutHz == 0 && highCutHz == 0;
    }

    /**
     * What is left of a recording played through this and then through {@code next}: a sound card into a speaker.
     * Each limit is the tighter of the two; a low cut that would reach the high cut is dropped.
     */
    public FrequencyResponse through(final FrequencyResponse next) {
        final int rate = tighter(maxSampleRate, next.maxSampleRate);
        final int kept = tighter(bits, next.bits);
        final int high = tighter(highCutHz, next.highCutHz);
        final int low = Math.max(lowCutHz, next.lowCutHz);
        return new FrequencyResponse(rate, kept, high > 0 && low >= high ? 0 : low, high);
    }

    /* The lower of two limits where 0 means none. */
    private static int tighter(final int a, final int b) {
        if (a == 0) {
            return b;
        }
        return b == 0 ? a : Math.min(a, b);
    }
}
