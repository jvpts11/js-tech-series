/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * What a line loses of the energy it carries by distance: every block of it loses a number of millionths of what
 * crosses it, and a way through many blocks loses the sum of theirs, never more than all of it, so a line can lose as
 * little as a hundredth of a percent over a hundred blocks. What is lost is rounded up, so a loss never rounds to
 * nothing while energy still crosses a lossy line, and what arrives is never more than what was sent.
 */
public final class EnergyLoss {

    /** The millionths that make the whole. */
    public static final int WHOLE = 1_000_000;

    private EnergyLoss() {
    }

    /** The loss of a way through blocks that lose {@code first} and then {@code then} millionths. */
    public static int along(final int first, final int then) {
        return (int) Math.min(WHOLE, (long) check(first) + check(then));
    }

    /** What arrives of {@code sent} over a way that loses {@code millionths}. */
    public static long delivered(final long sent, final int millionths) {
        if (sent < 0) {
            throw new IllegalArgumentException("an amount of energy is never below nothing: " + sent);
        }
        return sent - lost(sent, millionths);
    }

    /** What is lost of {@code sent} over a way that loses {@code millionths}, rounded up. */
    public static long lost(final long sent, final int millionths) {
        final int loss = check(millionths);
        if (loss == 0 || sent == 0) {
            return 0L;
        }
        if (loss == WHOLE) {
            return sent;
        }
        // sent * loss / WHOLE rounded up, without overflowing for any sent a long holds.
        final long whole = sent / WHOLE * loss;
        final long part = sent % WHOLE * loss;
        return whole + (part + WHOLE - 1) / WHOLE;
    }

    /** What has to be sent for {@code wanted} to arrive over a way that loses {@code millionths}. */
    public static long toSend(final long wanted, final int millionths) {
        final int loss = check(millionths);
        if (wanted <= 0) {
            return 0L;
        }
        if (loss == WHOLE) {
            return Long.MAX_VALUE;
        }
        /*
         * With the loss rounded up, what arrives of s is s * keep / WHOLE rounded down, so the least s that brings
         * wanted is wanted * WHOLE / keep rounded up, worked out in two halves so it never overflows.
         */
        final long keep = WHOLE - loss;
        final long times = wanted / keep;
        final long rest = wanted % keep;
        if (times > (Long.MAX_VALUE - WHOLE) / WHOLE) {
            return Long.MAX_VALUE;
        }
        return times * WHOLE + (rest * WHOLE + keep - 1) / keep;
    }

    private static int check(final int millionths) {
        if (millionths < 0 || millionths > WHOLE) {
            throw new IllegalArgumentException("a loss is between 0 and " + WHOLE + " millionths, not " + millionths);
        }
        return millionths;
    }
}
