/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * The random numbers one program draws.
 *
 * <p>A SplitMix64 generator: its whole state is one long, which is what lets a program put away with the world
 * carry on drawing the numbers it would have drawn had the world stayed up. A generator whose state cannot be read
 * back, as {@code java.util.Random}'s cannot, would start its sequence over on every load. Every program starts
 * from the same state, so one that never seeds draws the same numbers on every run, and one that seeds draws the
 * sequence that seed gives, on any machine.
 */
final class ProgramRandom {

    /** What SplitMix64 adds to its state on every draw: the golden ratio as an odd 64-bit number. */
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /** How many values a 32-bit draw can take. */
    private static final long WORDS = 1L << 32;

    private long state;

    /** Where the sequence has got to, for the save. */
    long state() {
        return this.state;
    }

    /** Carries on from that state: what a seed does, and what a program read back from its save does. */
    void startFrom(final long at) {
        this.state = at;
    }

    /** The next 64 random bits. */
    long nextLong() {
        this.state += GOLDEN_GAMMA;
        long mixed = this.state;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    /**
     * A whole number from zero up to, but not including, {@code bound}; a bound under one reads as one.
     *
     * <p>Multiplying a 32-bit draw by the bound and keeping the high half puts the draw in range without a division.
     * The few draws that would make some values come up more often than others are drawn again, so every value
     * under the bound is equally likely.
     */
    int next(final int bound) {
        final long range = Math.max(1, bound);
        long scaled = (this.nextLong() >>> 32) * range;
        if ((scaled & (WORDS - 1)) < range) {
            final long threshold = (WORDS - range) % range;
            while ((scaled & (WORDS - 1)) < threshold) {
                scaled = (this.nextLong() >>> 32) * range;
            }
        }
        return (int) (scaled >>> 32);
    }

    /** A number from zero up to, but not including, one, made from the top 53 bits of a draw. */
    double nextDouble() {
        return (this.nextLong() >>> 11) * 0x1.0p-53;
    }
}
