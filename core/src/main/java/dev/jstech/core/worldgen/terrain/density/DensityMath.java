/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

/**
 * The arithmetic of the Core's kinds of density function, with no game types, so a unit test checks each shape as the
 * world generation draws it: terraces, ridges, powers, smooth joins of two shapes, a value moved from one range to
 * another, and the horizontal distance from a point.
 */
public final class DensityMath {

    private DensityMath() {
    }

    /**
     * {@code value} in terraces {@code step} high: flat across most of each step, then rising smoothly to the next
     * over the last {@code smoothness} of it (0 for sheer steps, 1 for no flats at all).
     */
    public static double terrace(final double value, final double step, final double smoothness) {
        final double floor = Math.floor(value / step);
        final double along = value / step - floor;
        final double flat = 1 - Math.clamp(smoothness, 0, 1);
        if (along <= flat) {
            return floor * step;
        }
        final double rise = (along - flat) / (1 - flat);
        return (floor + smoothstep(rise)) * step;
    }

    /** {@code value} folded into a ridge: highest where it crosses nothing, falling away on both sides. */
    public static double ridge(final double value) {
        return 1 - Math.abs(value);
    }

    /** {@code value} raised to {@code exponent}, keeping its sign: a ground made steeper or gentler. */
    public static double power(final double value, final double exponent) {
        return Math.copySign(Math.pow(Math.abs(value), exponent), value);
    }

    /**
     * The smaller of {@code a} and {@code b}, the corner between them rounded over {@code smoothness}: two shapes
     * joined with no crease. Never above the smaller, and at most a quarter of {@code smoothness} below it.
     */
    public static double smoothMin(final double a, final double b, final double smoothness) {
        if (smoothness <= 0) {
            return Math.min(a, b);
        }
        final double h = Math.clamp(0.5 + 0.5 * (b - a) / smoothness, 0, 1);
        return b + (a - b) * h - smoothness * h * (1 - h);
    }

    /** The larger of {@code a} and {@code b}, the corner between them rounded over {@code smoothness}. */
    public static double smoothMax(final double a, final double b, final double smoothness) {
        return -smoothMin(-a, -b, smoothness);
    }

    /**
     * {@code value} moved from the range {@code fromLow..fromHigh} to {@code toLow..toHigh} along a line, held inside
     * the new range when {@code clamped}.
     */
    public static double remap(final double value, final double fromLow, final double fromHigh, final double toLow,
                               final double toHigh, final boolean clamped) {
        final double span = fromHigh - fromLow;
        final double share = span == 0 ? 0 : (value - fromLow) / span;
        final double out = toLow + (toHigh - toLow) * share;
        return clamped ? Math.clamp(out, Math.min(toLow, toHigh), Math.max(toLow, toHigh)) : out;
    }

    /** How far {@code (x, z)} is from {@code (centreX, centreZ)}, across, in units of {@code radius}. */
    public static double distance(final double x, final double z, final double centreX, final double centreZ,
                                  final double radius) {
        return Math.hypot(x - centreX, z - centreZ) / radius;
    }

    /* A rise from 0 to 1 that starts and ends level. */
    private static double smoothstep(final double t) {
        final double clamped = Math.clamp(t, 0, 1);
        return clamped * clamped * (3 - 2 * clamped);
    }
}
