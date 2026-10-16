/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import java.util.random.RandomGenerator;

/**
 * Where a particle starts within an emitter's shape, and the way it leaves, in the emitter's own frame, its way out
 * along Y. Every point of a shape is as likely as every other. Pure.
 */
public final class ShapeSampling {

    private static final double HALF = 0.5;

    private ShapeSampling() {
    }

    /** A point within a sphere of {@code radius} into {@code at}, and its way out from the middle into {@code way}. */
    public static void sphere(final RandomGenerator random, final double radius, final double[] at,
                              final double[] way) {
        unit(random, way);
        final double r = radius * Math.cbrt(random.nextDouble());
        at[0] = way[0] * r;
        at[1] = way[1] * r;
        at[2] = way[2] * r;
    }

    /** A point within a cube {@code size} a side, its middle at the emitter, into {@code at}. */
    public static void box(final RandomGenerator random, final double size, final double[] at) {
        at[0] = (random.nextDouble() - HALF) * size;
        at[1] = (random.nextDouble() - HALF) * size;
        at[2] = (random.nextDouble() - HALF) * size;
    }

    /** A point across a disc of {@code radius} facing the way out, into {@code at}. */
    public static void disc(final RandomGenerator random, final double radius, final double[] at) {
        final double r = radius * Math.sqrt(random.nextDouble());
        final double turn = 2 * Math.PI * random.nextDouble();
        at[0] = Math.cos(turn) * r;
        at[1] = 0;
        at[2] = Math.sin(turn) * r;
    }

    /** A way within a cone {@code angle} degrees across about the way out, into {@code way}. */
    public static void cone(final RandomGenerator random, final double angle, final double[] way) {
        final double half = Math.toRadians(angle / 2);
        final double tilt = Math.acos(1 - random.nextDouble() * (1 - Math.cos(half)));
        final double turn = 2 * Math.PI * random.nextDouble();
        way[0] = Math.sin(tilt) * Math.cos(turn);
        way[1] = Math.cos(tilt);
        way[2] = Math.sin(tilt) * Math.sin(turn);
    }

    /** A point along a line {@code size} long down the way out, its middle at the emitter, into {@code at}. */
    public static void line(final RandomGenerator random, final double size, final double[] at) {
        at[0] = 0;
        at[1] = (random.nextDouble() - HALF) * size;
        at[2] = 0;
    }

    /* A way picked evenly over every way there is. */
    private static void unit(final RandomGenerator random, final double[] way) {
        final double y = 2 * random.nextDouble() - 1;
        final double turn = 2 * Math.PI * random.nextDouble();
        final double flat = Math.sqrt(1 - y * y);
        way[0] = flat * Math.cos(turn);
        way[1] = y;
        way[2] = flat * Math.sin(turn);
    }
}
