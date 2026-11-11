/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.Locale;

/**
 * Teracoin's amounts. A TRC divides into eight decimal places, as a bitcoin does, so an amount is kept as a whole
 * number of hundred-millionths of a TRC (computers/docs/design/teracoin.md). Pure.
 */
public final class Trc {

    /** The hundred-millionths in one TRC. */
    public static final long UNITS = 100_000_000L;
    /** The decimal places an amount is written with in full. */
    public static final int DECIMALS = 8;

    private Trc() {
    }

    /** {@code whole} TRC, in hundred-millionths. */
    public static long of(final long whole) {
        return Math.multiplyExact(whole, UNITS);
    }

    /** {@code units} written in full, its thousands grouped and its eight places shown: {@code 1,284.53120000}. */
    public static String format(final long units) {
        return format(units, DECIMALS);
    }

    /** {@code units} with {@code places} decimal places, the ones beyond cut rather than rounded: what is there. */
    public static String format(final long units, final int places) {
        final long magnitude = Math.abs(units);
        final StringBuilder out = new StringBuilder(units < 0 ? "-" : "");
        out.append(String.format(Locale.ROOT, "%,d", magnitude / UNITS));
        final int shown = Math.max(0, Math.min(DECIMALS, places));
        if (shown > 0) {
            out.append('.').append(String.format(Locale.ROOT, "%08d", magnitude % UNITS), 0, shown);
        }
        return out.toString();
    }

    /** {@code units} as a price is written, no places it does not need: {@code 10}, {@code 241.43}. */
    public static String plain(final long units) {
        final String full = format(units, DECIMALS);
        int end = full.length();
        while (full.charAt(end - 1) == '0') {
            end--;
        }
        return full.charAt(end - 1) == '.' ? full.substring(0, end - 1) : full.substring(0, end);
    }

    /** {@code units} with its sign always written, as a history shows money coming and going: {@code +41.43000000}. */
    public static String signed(final long units, final int places) {
        return (units >= 0 ? "+" : "") + format(units, places);
    }

    /**
     * The amount a player typed in TRC, in hundred-millionths: digits, then at most one point and eight places, its
     * thousands grouped or not; -1 for anything else, an amount past what can exist included.
     */
    public static long parse(final String typed) {
        final String plain = typed.trim().replace(",", "");
        if (plain.isEmpty() || !plain.matches("\\d*(\\.\\d{0,8})?") || plain.equals(".")) {
            return -1L;
        }
        final int point = plain.indexOf('.');
        final String whole = point < 0 ? plain : plain.substring(0, point);
        final String places = point < 0 ? "" : plain.substring(point + 1);
        if (whole.length() > 12) {
            return -1L;
        }
        final long wholeUnits = whole.isEmpty() ? 0L : Long.parseLong(whole) * UNITS;
        final long fraction = places.isEmpty() ? 0L : Long.parseLong((places + "00000000").substring(0, DECIMALS));
        return wholeUnits + fraction;
    }
}
