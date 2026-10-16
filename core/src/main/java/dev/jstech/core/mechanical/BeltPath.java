/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.Optional;

/**
 * Where a belt or a chain runs between two pulleys, in the plane they turn in: its two straight runs, each from where
 * it leaves one pulley to where it meets the other, touching both as an open belt does; and how far it travels in a
 * time at a pulley's speed. Pure.
 *
 * @param fromAx where the first run leaves the first pulley, across the plane
 * @param fromAy and up it
 * @param toAx   where it meets the second
 * @param toAy   and up it
 * @param fromBx where the second run leaves the first pulley
 * @param fromBy and up it
 * @param toBx   where it meets the second
 * @param toBy   and up it
 */
public record BeltPath(double fromAx, double fromAy, double toAx, double toAy, double fromBx, double fromBy,
                       double toBx, double toBy) {

    private static final double SECONDS_A_MINUTE = 60.0;

    /**
     * The belt between pulleys of radii {@code r1} at ({@code x1}, {@code y1}) and {@code r2} at ({@code x2},
     * {@code y2}); none when one stands inside the other, where no belt can run.
     */
    public static Optional<BeltPath> open(final double x1, final double y1, final double r1, final double x2,
                                          final double y2, final double r2) {
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double apart = Math.sqrt(dx * dx + dy * dy);
        if (apart <= Math.abs(r1 - r2) || apart == 0) {
            return Optional.empty();
        }
        // An open belt touches both pulleys at points facing the same way out from their middles.
        final double along = Math.atan2(dy, dx);
        final double spread = Math.acos((r1 - r2) / apart);
        final double up = along + spread;
        final double down = along - spread;
        return Optional.of(new BeltPath(x1 + r1 * Math.cos(up), y1 + r1 * Math.sin(up), x2 + r2 * Math.cos(up),
                y2 + r2 * Math.sin(up), x1 + r1 * Math.cos(down), y1 + r1 * Math.sin(down), x2 + r2 * Math.cos(down),
                y2 + r2 * Math.sin(down)));
    }

    /** How far a belt on a pulley of {@code radius} turning at {@code rpm} travels in {@code seconds}, signed. */
    public static double travel(final double rpm, final double radius, final double seconds) {
        return rpm / SECONDS_A_MINUTE * 2 * Math.PI * radius * seconds;
    }

    /** How long each run is. */
    public double runLength() {
        return Math.hypot(this.toAx - this.fromAx, this.toAy - this.fromAy);
    }
}
