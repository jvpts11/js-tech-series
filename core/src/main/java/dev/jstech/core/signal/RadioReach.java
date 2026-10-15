/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

/**
 * How far a signal by frequency carries: as far as its sender's power in open air, less for every solid block in the
 * way. Within its reach the value arrives whole, whatever the distance; beyond it nothing arrives. How well it arrives,
 * from one bar at the edge of its reach to four near its sender, says only how much margin is left, never the value.
 */
public final class RadioReach {

    /** The blocks of reach each solid block in the way takes. */
    public static final double OBSTACLE_COST = 4.0;
    /** The most bars a signal arrives with. */
    public static final int MOST_BARS = 4;

    private RadioReach() {
    }

    /** The reach, in blocks, of a sender of {@code power} through {@code obstacles} solid blocks; never below 0. */
    public static double reach(final double power, final int obstacles) {
        return Math.max(0, power - OBSTACLE_COST * Math.max(0, obstacles));
    }

    /** Whether a sender of {@code power}, {@code distance} blocks away through {@code obstacles}, is heard. */
    public static boolean heard(final double power, final double distance, final int obstacles) {
        return power > 0 && distance <= reach(power, obstacles);
    }

    /**
     * How well a sender of {@code power}, {@code distance} blocks away through {@code obstacles}, is heard: 0 when it
     * is not, 1 at the edge of its reach, up to {@link #MOST_BARS} with three quarters of its power to spare.
     */
    public static int bars(final double power, final double distance, final int obstacles) {
        if (!heard(power, distance, obstacles)) {
            return 0;
        }
        final double spare = (reach(power, obstacles) - distance) / power;
        return Math.min(MOST_BARS, 1 + (int) Math.floor(spare * MOST_BARS));
    }
}
