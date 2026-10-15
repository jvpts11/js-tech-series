/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic of the charts: the round top a scale stops at, and the slices of a pie and which one a point falls
 * in, the first slice starting straight up and going clockwise.
 */
public final class ChartMath {

    private ChartMath() {
    }

    /**
     * The smallest round number at or above {@code value}: 1, 2, 2.5 or 5 times a power of ten, so a chart's top line
     * reads as a number a person would write.
     */
    public static double niceMax(final double value) {
        if (!(value > 0) || !Double.isFinite(value)) {
            return 1;
        }
        final double power = Math.pow(10, Math.floor(Math.log10(value)));
        for (final double step : new double[] {1, 2, 2.5, 5, 10}) {
            if (step * power >= value - 1e-12 * power) {
                return step * power;
            }
        }
        return 10 * power;
    }

    /**
     * The slices of a pie of {@code values}: each one's start and end as fractions of the whole turn, in order; a
     * value below zero counts as none, and a pie of nothing has no slices.
     */
    public static List<double[]> slices(final double[] values) {
        double total = 0;
        for (final double value : values) {
            total += Math.max(0, value);
        }
        final List<double[]> out = new ArrayList<>(values.length);
        double at = 0;
        for (final double value : values) {
            final double share = total == 0 ? 0 : Math.max(0, value) / total;
            out.add(new double[] {at, at + share});
            at += share;
        }
        return out;
    }

    /**
     * Which slice the point {@code (x, y)} falls in, for a pie centred at {@code (cx, cy)} with radius {@code r}; -1
     * outside it.
     */
    public static int sliceAt(final double x, final double y, final double cx, final double cy, final double r,
                              final List<double[]> slices) {
        final double dx = x - cx;
        final double dy = y - cy;
        if (dx * dx + dy * dy > r * r) {
            return -1;
        }
        final double turn = turnOf(dx, dy);
        for (int i = 0; i < slices.size(); i++) {
            if (turn >= slices.get(i)[0] && turn < slices.get(i)[1]) {
                return i;
            }
        }
        return -1;
    }

    /** How far round from straight up, clockwise, the direction {@code (dx, dy)} is, as a fraction of a turn. */
    public static double turnOf(final double dx, final double dy) {
        final double turn = Math.atan2(dx, -dy) / (2 * Math.PI);
        return turn < 0 ? turn + 1 : turn;
    }
}
