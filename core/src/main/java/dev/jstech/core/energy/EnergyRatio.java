/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import java.math.BigInteger;

/**
 * How many of a unit of energy make how many FE, as two whole numbers, so a conversion is exact and the same both ways
 * on every machine: {@code units} of the unit are worth {@code fe} FE. A conversion rounds down, so energy is never
 * made out of a rounding, and saturates at the largest amount a long holds rather than overflowing.
 *
 * @param units how many of the unit
 * @param fe    how many FE they are worth
 */
public record EnergyRatio(long units, long fe) {

    /** One for one: FE itself. */
    public static final EnergyRatio ONE = new EnergyRatio(1L, 1L);

    public EnergyRatio {
        if (units <= 0 || fe <= 0) {
            throw new IllegalArgumentException("a ratio of energy is of two amounts above nothing: " + units
                    + " to " + fe);
        }
        final long common = gcd(units, fe);
        units /= common;
        fe /= common;
    }

    /** How many FE {@code amount} of the unit is worth, rounded down. */
    public long toFe(final long amount) {
        return scale(amount, this.fe, this.units);
    }

    /** How many of the unit {@code amount} FE is worth, rounded down. */
    public long fromFe(final long amount) {
        return scale(amount, this.units, this.fe);
    }

    /* amount * times / per, rounded down, held to what a long holds. */
    private static long scale(final long amount, final long times, final long per) {
        if (amount < 0) {
            throw new IllegalArgumentException("an amount of energy is never below nothing: " + amount);
        }
        final long high = Math.multiplyHigh(amount, times);
        if (high == 0 && amount * times >= 0) {
            return amount * times / per;
        }
        final BigInteger exact = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(times))
                .divide(BigInteger.valueOf(per));
        return exact.bitLength() < Long.SIZE ? exact.longValue() : Long.MAX_VALUE;
    }

    private static long gcd(final long a, final long b) {
        long x = a;
        long y = b;
        while (y != 0) {
            final long next = x % y;
            x = y;
            y = next;
        }
        return x;
    }
}
