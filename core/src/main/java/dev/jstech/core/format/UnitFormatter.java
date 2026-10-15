/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats numeric values with mod units, locale-aware.
 */
public final class UnitFormatter {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;
    private static final long TRILLION = 1_000_000_000_000L;
    private static final int FRACTION_DIGITS = 2;

    /** The scales a value is shown at, smallest first: what it is divided by, and the letter that says so. */
    private static final Scale[] SCALES = {
            new Scale(THOUSAND, "K"), new Scale(MILLION, "M"), new Scale(BILLION, "G"), new Scale(TRILLION, "T")
    };
    /** The SI prefixes, a thousand apart, from none to exa. */
    private static final String[] SI_PREFIXES = {"", "k", "M", "G", "T", "P", "E"};

    private final Locale locale;
    private final NumberFormat fullFormat;
    private final NumberFormat compactFormat;

    public UnitFormatter(final Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
        this.fullFormat = NumberFormat.getIntegerInstance(locale);
        this.compactFormat = NumberFormat.getNumberInstance(locale);
        this.compactFormat.setMinimumFractionDigits(0);
        this.compactFormat.setMaximumFractionDigits(FRACTION_DIGITS);
    }

    public static UnitFormatter forCurrentLocale(){
        return new UnitFormatter(Locale.getDefault());
    }

    public String full(final long value, final Unit unit) {
        Objects.requireNonNull(unit, "unit must not be null");
        return fullFormat.format(value) + unit.suffix();
    }

    public String compact(final long value, final Unit unit) {
        Objects.requireNonNull(unit, "unit must not be null");
        return compact(value) + unit.suffix();
    }

    /**
     * {@code value} at the SI scale that suits it, its number and its prefix apart, so a unit's symbol goes after the
     * prefix: 7 900 watts are {@code 7.9} and {@code k}, to be written "7.9 kW".
     */
    public Scaled si(final long value) {
        final long abs = value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
        final String sign = value < 0 ? "-" : "";
        if (abs < THOUSAND) {
            return new Scaled(sign + abs, "");
        }
        int step = 0;
        double scaled = abs;
        while (step < SI_PREFIXES.length - 1 && scaled >= THOUSAND) {
            scaled /= THOUSAND;
            step++;
        }
        if (step < SI_PREFIXES.length - 1 && roundsUpToAThousand(scaled)) {
            scaled /= THOUSAND;
            step++;
        }
        return new Scaled(sign + compactFormat.format(scaled), SI_PREFIXES[step]);
    }

    /** {@code value} at the scale that suits it, with the scale's letter and no unit: {@code 1.5K}. */
    public String compact(final long value) {
        /*
         * Math.abs(Long.MIN_VALUE) stays negative (it overflows back to itself), which would print a
         * stray leading "-" on top of the sign prefix; clamp it to Long.MAX_VALUE so the magnitude is
         * always non-negative.
         */
        final long abs = value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
        final String sign = value < 0 ? "-" : "";

        if (abs < THOUSAND) {
            return sign + abs;
        }
        int scale = 0;
        while (scale < SCALES.length - 1 && abs >= SCALES[scale + 1].divisor()) {
            scale++;
        }
        // A value just under the next scale can round up to 1,000 of this one: show it as 1 of the next instead.
        if (scale < SCALES.length - 1 && roundsUpToAThousand((double) abs / SCALES[scale].divisor())) {
            scale++;
        }
        return sign + compactFormat.format((double) abs / SCALES[scale].divisor()) + SCALES[scale].prefix();
    }

    public Locale locale() {
        return locale;
    }

    private static boolean roundsUpToAThousand(final double scaled) {
        return new BigDecimal(scaled).setScale(FRACTION_DIGITS, RoundingMode.HALF_EVEN)
                .compareTo(BigDecimal.valueOf(THOUSAND)) >= 0;
    }

    /**
     * A value at a scale: its number and the SI prefix that says the scale.
     *
     * @param number the number, at most two places after the point
     * @param prefix the prefix, empty below a thousand
     */
    public record Scaled(String number, String prefix) {
    }

    private record Scale(long divisor, String prefix) {
    }
}
