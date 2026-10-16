/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Locale;

/**
 * How an effect's file writes its values: a colour as {@code "#RRGGBB"} or {@code "#RRGGBBAA"}, and a range as one
 * number or two, {@code 4} or {@code [3, 5]}.
 */
public final class EffectCodecs {

    /** A colour, {@code 0xAARRGGBB}; written without its alpha when it is opaque. */
    public static final Codec<Integer> RGBA = Codec.STRING.comapFlatMap(EffectCodecs::readColour,
            EffectCodecs::writeColour);
    /** A range of values, written as one number when both ends are one. */
    public static final Codec<Range> RANGE = Codec.either(Codec.DOUBLE, Codec.DOUBLE.listOf().comapFlatMap(
                    pair -> pair.size() == 2 ? DataResult.success(new Range(pair.get(0), pair.get(1)))
                            : DataResult.error(() -> "a range is one number or two, not " + pair.size()),
                    range -> List.of(range.min(), range.max())))
            .xmap(either -> either.map(value -> new Range(value, value), range -> range),
                    range -> range.min() == range.max() ? Either.left(range.min()) : Either.right(range));

    private static final int OPAQUE = 0xFF;
    private static final int HEX = 16;
    private static final int RGB_DIGITS = 6;
    private static final int RGBA_DIGITS = 8;
    private static final int ALPHA_SHIFT = 24;
    private static final int RGB = 0xFFFFFF;

    private EffectCodecs() {
    }

    private static DataResult<Integer> readColour(final String text) {
        final String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != RGB_DIGITS && digits.length() != RGBA_DIGITS) {
            return DataResult.error(() -> "an effect's colour is written #RRGGBB or #RRGGBBAA, not " + text);
        }
        try {
            final long read = Long.parseLong(digits, HEX);
            if (digits.length() == RGB_DIGITS) {
                return DataResult.success(OPAQUE << ALPHA_SHIFT | (int) read);
            }
            // Written red, green, blue, alpha; kept alpha, red, green, blue.
            return DataResult.success((int) ((read & OPAQUE) << ALPHA_SHIFT | read >>> 8 & RGB));
        } catch (final NumberFormatException e) {
            return DataResult.error(() -> "an effect's colour is written #RRGGBB or #RRGGBBAA, not " + text);
        }
    }

    private static String writeColour(final int argb) {
        final int alpha = argb >>> ALPHA_SHIFT;
        if (alpha == OPAQUE) {
            return String.format(Locale.ROOT, "#%06X", argb & RGB);
        }
        return String.format(Locale.ROOT, "#%06X%02X", argb & RGB, alpha);
    }

    /**
     * A range of values.
     *
     * @param min its least
     * @param max its most
     */
    public record Range(double min, double max) {

        /** The value {@code share} of the way from its least to its most. */
        public double at(final double share) {
            return this.min + (this.max - this.min) * share;
        }

        /** The range with both ends times {@code factor}. */
        public Range times(final double factor) {
            return new Range(this.min * factor, this.max * factor);
        }
    }
}
