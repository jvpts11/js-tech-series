/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.air;

/**
 * The arithmetic of air, without the game: a gas's partial pressure from its share and the air's pressure, whether a
 * body can breathe (by the oxygen's partial pressure, as a body does, not by the share of oxygen), and a value between
 * two heights of a dimension's bands.
 */
public final class AirMath {

    /** The overworld's air at sea level, in kilopascals. */
    public static final double SEA_LEVEL = 101.3;
    /** The least partial pressure of oxygen a body breathes on, in kilopascals: below it, it suffocates. */
    public static final double LEAST_OXYGEN = 16.0;

    private AirMath() {
    }

    /** The partial pressure of a gas making up {@code share} of air at {@code pressure} kilopascals. */
    public static double partial(final double share, final double pressure) {
        return Math.max(0, share) * Math.max(0, pressure);
    }

    /** Whether air with {@code oxygenShare} of oxygen at {@code pressure} kilopascals can be breathed. */
    public static boolean breathable(final double oxygenShare, final double pressure) {
        return partial(oxygenShare, pressure) >= LEAST_OXYGEN;
    }

    /**
     * The value at {@code y} between two heights {@code lowY} and {@code highY} holding {@code low} and {@code high}:
     * straight between them, the nearer one's beyond.
     */
    public static double between(final double y, final double lowY, final double low, final double highY,
                                 final double high) {
        if (highY <= lowY || y <= lowY) {
            return low;
        }
        if (y >= highY) {
            return high;
        }
        return low + (high - low) * (y - lowY) / (highY - lowY);
    }
}
