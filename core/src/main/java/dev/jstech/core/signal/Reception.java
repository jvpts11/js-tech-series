/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

/**
 * What a receiver hears of its frequency: the value it gives out, how well it arrives, and whether two transmitters
 * clash on it, when it gives out nothing.
 *
 * @param value the value it gives out, 0 to 15
 * @param bars  how well it arrives, 0 (not at all) to {@link RadioReach#MOST_BARS}
 * @param clash whether two transmitters of one owner are on its frequency
 */
public record Reception(int value, int bars, boolean clash) {

    /** Nothing heard: no transmitter, or none within reach. */
    public static final Reception NOTHING = new Reception(0, 0, false);
    /** Two transmitters on the frequency: nothing given out until one is taken off. */
    public static final Reception CLASH = new Reception(0, 0, true);

    /** Whether a transmitter's value arrives. */
    public boolean heard() {
        return this.bars > 0 && !this.clash;
    }
}
