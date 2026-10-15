/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

/**
 * The least and the most a number of a structure may be, both counted in: how many energy ports it has, how wide it
 * is. Pure.
 *
 * @param least the least, zero or more
 * @param most  the most, no less than the least
 */
public record Limit(int least, int most) {

    public Limit {
        if (least < 0 || most < least) {
            throw new IllegalArgumentException("a limit runs from zero or more up to no less than its least, not "
                    + least + " to " + most);
        }
    }

    /** Exactly {@code count}. */
    public static Limit exactly(final int count) {
        return new Limit(count, count);
    }

    /** {@code least} or more, with no most. */
    public static Limit atLeast(final int least) {
        return new Limit(least, Integer.MAX_VALUE);
    }

    /** Whether {@code value} is within it. */
    public boolean contains(final int value) {
        return value >= this.least && value <= this.most;
    }
}
