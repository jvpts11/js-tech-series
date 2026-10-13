/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

/**
 * Maps data values onto a graph's vertical pixel range.
 */
public record GraphScale(double minValue, double maxValue) {

    public GraphScale {
        if (maxValue < minValue) {
            throw new IllegalArgumentException(
                    "maxValue (" + maxValue + ") must be >= minValue (" + minValue + ")");
        }
    }

    public double normalize(final double value) {
        final double range = maxValue - minValue;
        if (range == 0.0) {
            return 0.0;
        }
        return (value - minValue) / range;
    }

    public double normalizeClamped(final double value) {
        final double t = normalize(value);
        if (t < 0.0) {
            return 0.0;
        }
        if (t > 1.0) {
            return 1.0;
        }
        return t;
    }

    public double valueToY(final double value, final double heightPixels) {
        final double t = normalizeClamped(value);
        return heightPixels * (1.0 - t);
    }

    /**
     * The scale that spans the finite values; NaN and the infinities are ignored whatever their place, and a list with
     * none gives an empty scale.
     */
    public static GraphScale fromData(final double[] values) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (final double v : values) {
            if (!Double.isFinite(v)) {
                continue;
            }
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return min > max ? new GraphScale(0.0, 0.0) : new GraphScale(min, max);
    }
}