/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * How a place answers a sound: how long its echo takes to die away, how loud the echo is beside the sound itself,
 * how soon the first echoes come back and the tail after them, and how much of the high notes the echo keeps.
 *
 * @param decaySeconds     how long the echo takes to fall by sixty decibels, the reverberation time
 * @param wet              how loud the echo is, from 0 (none, the open air) to 1
 * @param reflectionsDelay how soon the first echoes come back, in seconds
 * @param lateDelay        how long after the first echoes the tail begins, in seconds
 * @param highRatio        how long the high notes ring beside the low ones: under 1 where soft walls drink them
 */
public record Reverb(double decaySeconds, double wet, double reflectionsDelay, double lateDelay, double highRatio) {

    /** The shortest echo a place has, and the longest, as the sound card's reverberation can make them. */
    public static final double SHORTEST = 0.1;
    public static final double LONGEST = 20.0;
    /** No echo at all: the open air. */
    public static final Reverb DRY = new Reverb(SHORTEST, 0, 0, 0, 1);

    public Reverb {
        if (decaySeconds < SHORTEST || decaySeconds > LONGEST || wet < 0 || wet > 1 || reflectionsDelay < 0
                || lateDelay < 0 || highRatio <= 0) {
            throw new IllegalArgumentException("not an echo: " + decaySeconds + " s, wet " + wet + ", delays "
                    + reflectionsDelay + " and " + lateDelay + ", highs " + highRatio);
        }
    }

    /** Whether there is any echo to hear. */
    public boolean heard() {
        return this.wet > 0;
    }
}
