/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.format;

/**
 * The scale of a panel meter that has to show up to some value, as a real one is printed: its full scale the first
 * round number of 1, 2 or 5 times a power of ten at or above the value, written in the SI prefix that keeps it under a
 * thousand, with its middle mark. A meter that shows up to 5,000 FE a tick reads "0, 2.5, 5" in kFE/t; one that shows up
 * to 12 GW, "0, 10, 20" in GW. Pure: no game types.
 *
 * @param full       the full scale, in the unit itself
 * @param prefix     the SI prefix its marks are written in: "", "k", "M", "G", "T", "P" or "E"
 * @param fullLabel  the full scale's mark, in that prefix: "5"
 * @param halfLabel  the middle mark: "2.5"
 */
public record MeterScale(long full, String prefix, String fullLabel, String halfLabel) {

    private static final long[] STEPS = {1L, 2L, 5L};
    private static final String[] PREFIXES = {"", "k", "M", "G", "T", "P", "E"};
    private static final int RADIX = 10;
    private static final int PREFIX_STEP = 1000;

    /** The scale of a meter that shows up to {@code most}; up to 1 for nothing or less. */
    public static MeterScale upTo(final long most) {
        final long target = Math.max(1L, most);
        long decade = 1L;
        long full = 0L;
        while (full == 0L) {
            for (final long step : STEPS) {
                if (step * decade >= target) {
                    full = step * decade;
                    break;
                }
            }
            if (full == 0L) {
                if (decade > Long.MAX_VALUE / RADIX / STEPS[STEPS.length - 1]) {
                    full = Long.MAX_VALUE;
                } else {
                    decade *= RADIX;
                }
            }
        }
        int prefix = 0;
        long unit = 1L;
        while (full / unit >= PREFIX_STEP && prefix < PREFIXES.length - 1) {
            unit *= PREFIX_STEP;
            prefix++;
        }
        return new MeterScale(full, PREFIXES[prefix], mark(full, unit), mark(full / 2, unit) + half(full, unit));
    }

    /** Where {@code value} stands on the scale, from 0 at its foot to 1 at its full scale. */
    public double fraction(final long value) {
        return Math.max(0.0, Math.min(1.0, (double) value / this.full));
    }

    /* The whole part of {@code value} in {@code unit}. */
    private static String mark(final long value, final long unit) {
        return Long.toString(value / unit);
    }

    /*
     * What a middle mark has beyond its whole part: the full scale is 1, 2 or 5 times a power of ten, so in its prefix
     * it is odd only as 1 or 5, and half of those is a half more than its whole part.
     */
    private static String half(final long full, final long unit) {
        return full % (2 * unit) == 0 ? "" : ".5";
    }
}
