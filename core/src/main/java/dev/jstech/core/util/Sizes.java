/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

/**
 * Arithmetic on how much of something there is, where running past the end means the end and not the start.
 *
 * <p>How much a network holds, how much a disk takes, how much an Operation asked for: all of them are counts
 * that only grow, and all of them are multiplied and added along the way. Java wraps: a count large enough,
 * times the weight of one, comes back negative. A negative weight is not a big number badly written, it is a
 * different answer entirely, and everything downstream believes it. Free space reads as more than the disk
 * has. A check that a request fits passes because the request is below zero. An amount taken out is added back
 * in. None of it looks like arithmetic when it happens; it looks like storage inventing items.
 *
 * <p>An amount can arrive from outside, so this is not only about numbers nobody would type. A packet carries
 * whatever it was built with, and asking for every item in the world is a request a program is allowed to
 * make: the answer has to be all of them, not a negative number of them.
 *
 * <p>So the answers here stop at the largest and smallest numbers there are rather than going round. Stopping
 * is not right either, but it is wrong in the direction that shows: a request for more than there is gets
 * everything there is, which is the same answer it would have got had the number been merely large.
 *
 * <p>The cost when nothing overflows is nothing at all: these compile to the same instruction as the plain
 * arithmetic with a jump nobody takes.
 */
public final class Sizes {

    private Sizes() {
    }

    /** One amount times another, stopping at the largest there is rather than going round it. */
    public static long times(final long a, final long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (final ArithmeticException pastTheEnd) {
            return (a < 0L) == (b < 0L) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    /** One amount plus another, stopping at the largest there is rather than going round it. */
    public static long plus(final long a, final long b) {
        try {
            return Math.addExact(a, b);
        } catch (final ArithmeticException pastTheEnd) {
            return a > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    /** One amount less another, stopping at the smallest there is rather than going round it. */
    public static long minus(final long a, final long b) {
        try {
            return Math.subtractExact(a, b);
        } catch (final ArithmeticException pastTheEnd) {
            return a > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    /**
     * How many whole units of {@code per} it takes to hold {@code total}, which is a division that rounds up.
     *
     * <p>Written the obvious way, the rounding is an addition that can itself go past the end, and then the
     * answer comes back as nothing where it should have been everything.
     */
    public static long ceilDiv(final long total, final long per) {
        if (total <= 0L || per <= 0L) {
            return 0L;
        }
        return total / per + (total % per == 0L ? 0L : 1L);
    }

    /** What is left of {@code total} after {@code taken}, and never less than nothing. */
    public static long remaining(final long total, final long taken) {
        return Math.max(0L, minus(total, taken));
    }
}
