/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * The arithmetic of a colour picker: a colour as hue, saturation and value, the square and the bar it is picked on,
 * and its {@code #RRGGBB} as a person writes it. Colours are opaque {@code 0xAARRGGBB}.
 */
public final class ColorMath {

    /** The alpha of an opaque colour, a mask, not a colour. */
    private static final int OPAQUE = 0xFF << 24;

    private ColorMath() {
    }

    /** The colour of hue {@code h} (0 to 1, round), saturation {@code s} and value {@code v} (0 to 1), opaque. */
    public static int hsv(final double h, final double s, final double v) {
        final double hue = (h - Math.floor(h)) * 6.0;
        final double sat = clamp(s);
        final double val = clamp(v);
        final int sector = (int) Math.floor(hue) % 6;
        final double f = hue - Math.floor(hue);
        final double p = val * (1 - sat);
        final double q = val * (1 - sat * f);
        final double t = val * (1 - sat * (1 - f));
        final double[] rgb = switch (sector) {
            case 0 -> new double[] {val, t, p};
            case 1 -> new double[] {q, val, p};
            case 2 -> new double[] {p, val, t};
            case 3 -> new double[] {p, q, val};
            case 4 -> new double[] {t, p, val};
            default -> new double[] {val, p, q};
        };
        return OPAQUE | channel(rgb[0]) << 16 | channel(rgb[1]) << 8 | channel(rgb[2]);
    }

    /** {@code argb} as hue, saturation and value, each 0 to 1; a grey has hue 0. */
    public static double[] toHsv(final int argb) {
        final double r = (argb >> 16 & 0xFF) / 255.0;
        final double g = (argb >> 8 & 0xFF) / 255.0;
        final double b = (argb & 0xFF) / 255.0;
        final double max = Math.max(r, Math.max(g, b));
        final double min = Math.min(r, Math.min(g, b));
        final double delta = max - min;
        double hue = 0;
        if (delta > 0) {
            if (max == r) {
                hue = ((g - b) / delta) % 6;
            } else if (max == g) {
                hue = (b - r) / delta + 2;
            } else {
                hue = (r - g) / delta + 4;
            }
            hue /= 6;
            if (hue < 0) {
                hue += 1;
            }
        }
        return new double[] {hue, max == 0 ? 0 : delta / max, max};
    }

    /** {@code argb} written as a person writes a colour: {@code #RRGGBB}, in capitals. */
    public static String hex(final int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    /** A colour read from {@code #RRGGBB} or {@code RRGGBB}, opaque; none when it is neither. */
    public static OptionalInt parse(final String text) {
        final String hex = text.trim().startsWith("#") ? text.trim().substring(1) : text.trim();
        if (hex.length() != 6) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(OPAQUE | Integer.parseInt(hex, 16));
    }

    private static int channel(final double value) {
        return (int) Math.round(clamp(value) * 255);
    }

    private static double clamp(final double value) {
        return Math.max(0, Math.min(1, value));
    }
}
