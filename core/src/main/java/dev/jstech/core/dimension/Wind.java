/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

/**
 * The wind at a place, along the ground: towards east and towards south, in metres a second.
 *
 * @param x towards east
 * @param z towards south
 */
public record Wind(double x, double z) {

    /** No wind: a vacuum, or a still day. */
    public static final Wind STILL = new Wind(0, 0);

    private static final double FULL_TURN = 360.0;

    /** How fast it blows, metres a second. */
    public double speed() {
        return Math.hypot(this.x, this.z);
    }

    /** The way it comes from, in degrees round from north (0) by east (90); none for no wind. */
    public double fromDegrees() {
        if (speed() <= 0) {
            return 0;
        }
        // It comes from the way opposite the one it blows to; north is towards negative Z.
        final double to = Math.toDegrees(Math.atan2(this.x, -this.z));
        return ((to + FULL_TURN / 2) % FULL_TURN + FULL_TURN) % FULL_TURN;
    }

    /** The compass point it comes from, one of eight: 0 north, 1 north-east, round to 7 north-west. */
    public int fromPoint() {
        final double eighth = FULL_TURN / 8;
        return (int) Math.floor((fromDegrees() + eighth / 2) / eighth) % 8;
    }
}
