/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

/**
 * The arithmetic of an amount stepper: one step and a factor of two either way, kept inside a range.
 */
public final class StepperMath {

    private StepperMath() {
    }

    /** {@code value} kept inside [{@code min}, {@code max}]. */
    public static long clamp(final long value, final long min, final long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long increment(final long value, final long min, final long max) {
        return clamp(value == Long.MAX_VALUE ? value : value + 1, min, max);
    }

    public static long decrement(final long value, final long min, final long max) {
        return clamp(value == Long.MIN_VALUE ? value : value - 1, min, max);
    }

    /** Twice the value, or the maximum when doubling would pass it. */
    public static long doubled(final long value, final long min, final long max) {
        return clamp(value > max / 2 ? max : value * 2, min, max);
    }

    /** Half the value, rounded down, never below the minimum. */
    public static long halved(final long value, final long min, final long max) {
        return clamp(Math.floorDiv(value, 2L), min, max);
    }

    /**
     * The amount {@code digits} spell, kept inside the range: what is typed into a stepper. Anything but a digit is
     * passed over, and a number too long for a {@code long} reads as the maximum.
     */
    public static long typed(final String digits, final long min, final long max) {
        long value = 0;
        for (int i = 0; i < digits.length(); i++) {
            final char c = digits.charAt(i);
            if (c < '0' || c > '9') {
                continue;
            }
            final int digit = c - '0';
            if (value > (Long.MAX_VALUE - digit) / 10) {
                return max;
            }
            value = value * 10 + digit;
        }
        return clamp(value, min, max);
    }
}
