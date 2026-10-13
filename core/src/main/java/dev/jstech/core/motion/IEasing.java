/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import java.util.Locale;
import java.util.Map;

/**
 * How a motion goes from its start to its end: given how much of its time has passed, from 0 to 1, how far along it
 * is, from 0 to 1.
 *
 * <p>Written as the systems that are imitated write their own: a cubic Bezier curve as in CSS and in the toolkits
 * of every modern desktop ({@code cubic-bezier(0, 0, 0, 1)}), straight, or in steps, the way the oldest systems moved a
 * shape in a few jumps. The usual curves have names ({@link #named}), so a motion file can say
 * {@code ease-out-expo} rather than four numbers.
 */
public sealed interface IEasing permits IEasing.Linear, IEasing.CubicBezier, IEasing.Steps {

    /** Straight from start to end. */
    IEasing LINEAR = new Linear();

    /** The curves known by name, as CSS and the desktops' own toolkits call them. */
    Map<String, IEasing> NAMED = Map.ofEntries(
            Map.entry("linear", LINEAR),
            Map.entry("ease", new CubicBezier(0.25, 0.1, 0.25, 1.0)),
            Map.entry("ease-in", new CubicBezier(0.42, 0.0, 1.0, 1.0)),
            Map.entry("ease-out", new CubicBezier(0.0, 0.0, 0.58, 1.0)),
            Map.entry("ease-in-out", new CubicBezier(0.42, 0.0, 0.58, 1.0)),
            Map.entry("ease-in-quad", new CubicBezier(0.55, 0.085, 0.68, 0.53)),
            Map.entry("ease-out-quad", new CubicBezier(0.25, 0.46, 0.45, 0.94)),
            Map.entry("ease-in-cubic", new CubicBezier(0.55, 0.055, 0.675, 0.19)),
            Map.entry("ease-out-cubic", new CubicBezier(0.215, 0.61, 0.355, 1.0)),
            Map.entry("ease-in-sine", new CubicBezier(0.47, 0.0, 0.745, 0.715)),
            Map.entry("ease-out-expo", new CubicBezier(0.19, 1.0, 0.22, 1.0)),
            Map.entry("fluent-entrance", new CubicBezier(0.0, 0.0, 0.0, 1.0)),
            Map.entry("fluent-point", new CubicBezier(0.55, 0.55, 0.0, 1.0)),
            Map.entry("fluent-exit", new CubicBezier(1.0, 0.0, 1.0, 1.0)));

    /** How far along the motion is when {@code time} of it has passed, both from 0 to 1. */
    double apply(double time);

    /** How a motion file writes {@code curve}: by its name when it has one, the way a person would write it. */
    static String nameOf(final IEasing curve) {
        String name = null;
        for (final Map.Entry<String, IEasing> known : NAMED.entrySet()) {
            // The first name in alphabetical order, so the same curve is always written the same way.
            if (known.getValue().equals(curve) && (name == null || known.getKey().compareTo(name) < 0)) {
                name = known.getKey();
            }
        }
        return name != null ? name : curve.written();
    }

    /** How a motion file writes this curve. */
    String written();

    /**
     * The curve a motion file names: one of the {@link #NAMED} ones, {@code cubic-bezier(x1, y1, x2, y2)} or
     * {@code steps(n)}.
     *
     * @throws IllegalArgumentException when the text names no curve
     */
    static IEasing named(final String text) {
        final String key = text.trim().toLowerCase(Locale.ROOT);
        final IEasing known = NAMED.get(key);
        if (known != null) {
            return known;
        }
        if (key.startsWith("cubic-bezier(") && key.endsWith(")")) {
            final String[] parts = key.substring("cubic-bezier(".length(), key.length() - 1).split(",");
            if (parts.length == 4) {
                try {
                    return new CubicBezier(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                            Double.parseDouble(parts[2].trim()), Double.parseDouble(parts[3].trim()));
                } catch (final NumberFormatException notANumber) {
                    throw new IllegalArgumentException("a cubic-bezier needs four numbers: " + text, notANumber);
                }
            }
        }
        if (key.startsWith("steps(") && key.endsWith(")")) {
            try {
                return new Steps(Integer.parseInt(key.substring("steps(".length(), key.length() - 1).trim()));
            } catch (final NumberFormatException notANumber) {
                throw new IllegalArgumentException("steps needs a whole number: " + text, notANumber);
            }
        }
        throw new IllegalArgumentException("no curve is called " + text);
    }

    /** Straight from start to end. */
    record Linear() implements IEasing {

        @Override
        public double apply(final double time) {
            return clamp(time);
        }

        @Override
        public String written() {
            return "linear";
        }
    }

    /**
     * A cubic Bezier curve from (0, 0) to (1, 1) through two control points, as CSS writes it: for a time on the x
     * axis, how far along on the y axis. The x coordinates are held to 0 to 1, as CSS holds them, so every time has
     * one answer.
     */
    record CubicBezier(double x1, double y1, double x2, double y2) implements IEasing {

        public CubicBezier {
            // Infinity gets in through exponent overflow ("1e999"); a NaN or infinite control point would make every
            // mid-range progress NaN, so a curve built from one is refused instead.
            if (!Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(x2) || !Double.isFinite(y2)) {
                throw new IllegalArgumentException("a cubic-bezier needs finite numbers: " + x1 + ", " + y1 + ", "
                        + x2 + ", " + y2);
            }
            x1 = clamp(x1);
            x2 = clamp(x2);
        }

        @Override
        public double apply(final double time) {
            final double x = clamp(time);
            if (x == 0.0 || x == 1.0) {
                return x;
            }
            return curve(y1, y2, parameterAt(x));
        }

        @Override
        public String written() {
            return "cubic-bezier(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")";
        }

        /** The curve's own parameter where its x is {@code x}: Newton's method, and halving where that strays. */
        private double parameterAt(final double x) {
            double t = x;
            for (int i = 0; i < 8; i++) {
                final double error = curve(x1, x2, t) - x;
                final double slope = slope(x1, x2, t);
                if (Math.abs(error) < 1e-7) {
                    return t;
                }
                if (Math.abs(slope) < 1e-6) {
                    break;
                }
                t -= error / slope;
            }
            double low = 0.0;
            double high = 1.0;
            t = x;
            for (int i = 0; i < 40; i++) {
                final double at = curve(x1, x2, t);
                if (Math.abs(at - x) < 1e-7) {
                    break;
                }
                if (at < x) {
                    low = t;
                } else {
                    high = t;
                }
                t = (low + high) / 2.0;
            }
            return t;
        }

        /** One coordinate of the curve at parameter {@code t}, the curve starting at 0 and ending at 1. */
        private static double curve(final double p1, final double p2, final double t) {
            final double u = 1.0 - t;
            return 3.0 * u * u * t * p1 + 3.0 * u * t * t * p2 + t * t * t;
        }

        private static double slope(final double p1, final double p2, final double t) {
            final double u = 1.0 - t;
            return 3.0 * u * u * p1 + 6.0 * u * t * (p2 - p1) + 3.0 * t * t * (1.0 - p2);
        }
    }

    /** In that many equal jumps, the last landing at the end: how the oldest systems moved a shape. */
    record Steps(int count) implements IEasing {

        public Steps {
            if (count < 1) {
                throw new IllegalArgumentException("steps needs at least one step, not " + count);
            }
        }

        @Override
        public double apply(final double time) {
            final double x = clamp(time);
            return x >= 1.0 ? 1.0 : Math.floor(x * count) / count;
        }

        @Override
        public String written() {
            return "steps(" + count + ")";
        }
    }

    private static double clamp(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
