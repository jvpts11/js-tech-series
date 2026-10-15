/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import java.util.Locale;

/**
 * A converter's limit as its thumbwheels set it: four digit drums and a fifth that turns between kilowatts, megawatts
 * and gigawatts, "0500k" being 500 kW. Each class has the most a converter of it carries, its ceiling, from 100 kW at
 * low voltage to 12 GW at ultra-high, and a converter's limit stays under it. Pure: no game types.
 *
 * @param digits what the four digit drums show, 0 to 9999
 * @param power  the power of ten the fifth drum stands for: 3, 6 or 9
 */
public record ConverterLimit(int digits, int power) {

    /** How many digit drums there are, the prefix's drum besides. */
    public static final int DRUMS = 4;
    /** The most the digit drums show. */
    public static final int MOST_DIGITS = 9999;
    /** The prefix drum's powers of ten, in the order it turns through them: kilo, mega, giga. */
    public static final int KILO = 3;
    public static final int MEGA = 6;
    public static final int GIGA = 9;

    private static final int RADIX = 10;
    private static final int POWER_STEP = 3;
    private static final long LOW_CEILING = 100_000L;
    private static final long MEDIUM_CEILING = 10_000_000L;
    private static final long HIGH_CEILING = 200_000_000L;
    private static final long EXTRA_HIGH_CEILING = 1_500_000_000L;
    private static final long ULTRA_HIGH_CEILING = 12_000_000_000L;

    public ConverterLimit {
        if (digits < 0 || digits > MOST_DIGITS) {
            throw new IllegalArgumentException("the drums show 0 to " + MOST_DIGITS + ", not " + digits);
        }
        if (power != KILO && power != MEGA && power != GIGA) {
            throw new IllegalArgumentException("the prefix drum stands for kilo, mega or giga, not 10^" + power);
        }
    }

    /** The most a converter of {@code voltage} carries, in watts. */
    public static long ceiling(final VoltageClass voltage) {
        return switch (voltage) {
            case LOW -> LOW_CEILING;
            case MEDIUM -> MEDIUM_CEILING;
            case HIGH -> HIGH_CEILING;
            case EXTRA_HIGH -> EXTRA_HIGH_CEILING;
            case ULTRA_HIGH -> ULTRA_HIGH_CEILING;
        };
    }

    /**
     * {@code watts} as the drums show it, in the smallest prefix that holds it in four digits: 100 kW is "0100k", 12 GW
     * "0012G". Watts that four digits of no prefix hold exactly are rounded down to what they do hold.
     */
    public static ConverterLimit of(final long watts) {
        final long at = Math.max(0L, watts);
        for (int p = KILO; p <= GIGA; p += POWER_STEP) {
            final long digits = at / pow(p);
            if (digits <= MOST_DIGITS) {
                return new ConverterLimit((int) digits, p);
            }
        }
        return new ConverterLimit(MOST_DIGITS, GIGA);
    }

    /** What it stands for, in watts. */
    public long watts() {
        return this.digits * pow(this.power);
    }

    /**
     * The limit with digit drum {@code drum} turned a step, up or down: 0 is the leftmost, the thousands. A drum turns
     * round, 9 up to 0, 0 down to 9, as a thumbwheel does, and leaves the others as they are.
     */
    public ConverterLimit turned(final int drum, final boolean up) {
        if (drum < 0 || drum >= DRUMS) {
            throw new IllegalArgumentException("there are " + DRUMS + " digit drums, not a drum " + drum);
        }
        final int place = (int) pow(DRUMS - 1 - drum);
        final int digit = this.digits / place % RADIX;
        final int next = up ? (digit + 1) % RADIX : (digit + RADIX - 1) % RADIX;
        return new ConverterLimit(this.digits + (next - digit) * place, this.power);
    }

    /** The limit with the prefix drum turned a step: kilo to mega to giga, and round to kilo. */
    public ConverterLimit nextPrefix() {
        return new ConverterLimit(this.digits, this.power == GIGA ? KILO : this.power + POWER_STEP);
    }

    /** What the digit drums show, four digits: "0500". */
    public String drumsText() {
        return String.format(Locale.ROOT, "%04d", this.digits);
    }

    /** The letter on the prefix drum: k, M or G. */
    public String prefixSymbol() {
        return this.power == KILO ? "k" : this.power == MEGA ? "M" : "G";
    }

    private static long pow(final int exponent) {
        long value = 1L;
        for (int i = 0; i < exponent; i++) {
            value *= RADIX;
        }
        return value;
    }
}
