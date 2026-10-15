/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import java.util.Objects;

/**
 * One frequency on the air: its band, whose it is (an owner's id for a private one, a team's id for a team's, the
 * pair's own id for a pair) and its number (none for a pair). A frequency carries one value, so two transmitters on
 * one frequency are a clash.
 *
 * @param band   whose frequencies it is among
 * @param whose  the owner, the team or the pair it belongs to
 * @param number its number, from {@link #LEAST} to {@link #MOST}; 0 for a pair
 */
public record Frequency(Band band, String whose, int number) {

    /** The lowest number a frequency has. */
    public static final int LEAST = 1;
    /** The highest number a frequency has. */
    public static final int MOST = 9_999;

    public Frequency {
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(whose, "whose");
        if (band == Band.PAIR ? number != 0 : number < LEAST || number > MOST) {
            throw new IllegalArgumentException("a frequency of the " + band.serializedName() + " band cannot be "
                    + number);
        }
    }

    /** {@code number}, held to the numbers a frequency has. */
    public static int held(final int number) {
        return Math.max(LEAST, Math.min(MOST, number));
    }
}
