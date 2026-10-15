/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One thing a sensor reads, as it speaks it in redstone: what it is, its value now, and the span it is read over, from
 * the value that gives no signal to the one that gives the full 15. Light, heat, speed, a room's pressure, radiation,
 * altitude, a machine's declared parameter: each is a reading.
 *
 * @param name  what it reads
 * @param value its value now
 * @param least the value that gives a signal of 0
 * @param most  the value that gives a signal of 15
 * @param unit  the unit its value is in; none for a count or a share
 */
public record Reading(TextKey name, double value, double least, double most, @Nullable TextKey unit) {

    /** The strongest signal of redstone. */
    public static final int FULL = 15;

    public Reading {
        Objects.requireNonNull(name, "name");
        if (!(most > least)) {
            throw new IllegalArgumentException("a reading's span runs from the less to the more, not from " + least
                    + " to " + most);
        }
    }

    /** The signal it gives, from 0 at the least of its span to 15 at the most, the value outside it held to it. */
    public int strength() {
        return strength(this.value, this.least, this.most);
    }

    /** The signal {@code value} gives over the span from {@code least} to {@code most}, 0 to 15. */
    public static int strength(final double value, final double least, final double most) {
        final double share = (value - least) / (most - least);
        if (Double.isNaN(share) || share <= 0) {
            return 0;
        }
        return share >= 1 ? FULL : (int) Math.round(share * FULL);
    }
}
