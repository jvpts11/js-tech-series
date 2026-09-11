/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Lua's numbers: whole ones as longs, real ones as doubles, and the rules that join them.
 *
 * <p>A whole number stays whole through {@code + - * // %} and wraps around at the ends; a division
 * or a power is always real; a real that is whole is still real, and prints with its point so a
 * program can tell. Text turns into a number the way the language reads a number, whitespace and a
 * hexadecimal form included, and a number turns into text the way C's {@code %.14g} would.
 */
public final class LuaNumbers {

    private static final int SIGNIFICANT = 14;

    private LuaNumbers() {
    }

    /** Whether the value is one of the two kinds of number. */
    public static boolean isNumber(final Object value) {
        return value instanceof Long || value instanceof Double;
    }

    /** The value as a number, or null when it is neither a number nor text that reads as one. */
    public static Object toNumber(final Object value) {
        if (value instanceof Long || value instanceof Double) {
            return value;
        }
        if (value instanceof Integer whole) {
            return whole.longValue();
        }
        if (value instanceof Float real) {
            return real.doubleValue();
        }
        if (value instanceof String text) {
            return parse(text);
        }
        return null;
    }

    /** The value as a real, for a value already known to be a number. */
    public static double toDouble(final Object number) {
        return number instanceof Long whole ? (double) whole : (Double) number;
    }

    /**
     * The value as a whole number when it has one exactly: a long, or a real with no fraction that
     * fits, or text that reads as either. Null otherwise.
     */
    public static Long toInteger(final Object value) {
        final Object number = toNumber(value);
        if (number instanceof Long whole) {
            return whole;
        }
        if (number instanceof Double real && real == Math.rint(real) && !real.isInfinite()
                && real >= -0x1p63 && real < 0x1p63) {
            return real.longValue();
        }
        return null;
    }

    /**
     * Reads a number the way the language does: leading and trailing whitespace, an optional sign, a
     * decimal or hexadecimal whole or real. Null when the text is not one.
     */
    public static Object parse(final String text) {
        final String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        int at = 0;
        boolean negative = false;
        if (trimmed.charAt(0) == '-' || trimmed.charAt(0) == '+') {
            negative = trimmed.charAt(0) == '-';
            at = 1;
        }
        final String body = trimmed.substring(at);
        if (body.isEmpty()) {
            return null;
        }
        if (body.length() > 2 && body.charAt(0) == '0' && (body.charAt(1) == 'x' || body.charAt(1) == 'X')) {
            return hex(body.substring(2), negative);
        }
        return decimal(body, negative);
    }

    private static Object decimal(final String body, final boolean negative) {
        boolean whole = true;
        for (int i = 0; i < body.length(); i++) {
            final char c = body.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                whole = false;
            } else if (!Character.isDigit(c) && c != '+' && c != '-') {
                return null;
            }
        }
        if (whole) {
            try {
                final long value = Long.parseLong(body);
                return negative ? -value : value;
            } catch (final NumberFormatException tooBig) {
                // A whole number too big for a long is read as a real, as the language does.
            }
        }
        // Java reads more than Lua would (a trailing 'd', "Infinity"), so the shape is checked first.
        if (!body.matches("(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")) {
            return null;
        }
        try {
            final double value = Double.parseDouble(body);
            return negative ? -value : value;
        } catch (final NumberFormatException notANumber) {
            return null;
        }
    }

    private static Object hex(final String digits, final boolean negative) {
        if (digits.isEmpty()) {
            return null;
        }
        long value = 0;
        for (int i = 0; i < digits.length(); i++) {
            final int digit = Character.digit(digits.charAt(i), 16);
            if (digit < 0) {
                return null;
            }
            // Hexadecimal whole numbers wrap around, as the language reads them.
            value = value * 16 + digit;
        }
        return negative ? -value : value;
    }

    /** Reads a whole number written in that base, as {@code tonumber(text, base)} does; null when not one. */
    public static Long parse(final String text, final int base) {
        final String trimmed = text.strip().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        int at = 0;
        boolean negative = false;
        if (trimmed.charAt(0) == '-') {
            negative = true;
            at = 1;
        }
        if (at >= trimmed.length()) {
            return null;
        }
        long value = 0;
        for (int i = at; i < trimmed.length(); i++) {
            final int digit = Character.digit(trimmed.charAt(i), base);
            if (digit < 0) {
                return null;
            }
            value = value * base + digit;
        }
        return negative ? -value : value;
    }

    /** The number as text: a whole number plainly, a real with fourteen significant digits and its point. */
    public static String format(final Object number) {
        if (number instanceof Long whole) {
            return Long.toString(whole);
        }
        return formatReal((Double) number);
    }

    /** A real as C's {@code %.14g} writes it, with {@code .0} added when nothing else says it is real. */
    public static String formatReal(final double value) {
        if (Double.isNaN(value)) {
            return value < 0 ? "-nan" : "nan";
        }
        if (Double.isInfinite(value)) {
            return value < 0 ? "-inf" : "inf";
        }
        if (value == 0) {
            return 1 / value < 0 ? "-0.0" : "0.0";
        }
        final String text = general(value, SIGNIFICANT);
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '.' || c == 'e' || c == 'n' || c == 'i') {
                return text;
            }
        }
        return text + ".0";
    }

    /**
     * C's {@code %.<precision>g}: fixed notation when the exponent is between minus four and the
     * precision, scientific otherwise, trailing zeros dropped either way.
     */
    public static String general(final double value, final int precision) {
        final int digits = Math.max(1, precision);
        final BigDecimal exact = new BigDecimal(value);
        final BigDecimal rounded = exact.round(new java.math.MathContext(digits));
        final int exponent = rounded.precision() - rounded.scale() - 1;
        if (exponent >= -4 && exponent < digits) {
            final String fixed = rounded.setScale(Math.max(0, digits - 1 - exponent), java.math.RoundingMode.HALF_EVEN)
                    .toPlainString();
            return trimZeros(fixed);
        }
        final String mantissa = trimZeros(rounded.movePointLeft(exponent)
                .setScale(digits - 1, java.math.RoundingMode.HALF_EVEN).toPlainString());
        return mantissa + "e" + (exponent < 0 ? "-" : "+") + (Math.abs(exponent) < 10 ? "0" : "") + Math.abs(exponent);
    }

    private static String trimZeros(final String text) {
        if (text.indexOf('.') < 0) {
            return text;
        }
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }

    // arithmetic on two numbers already known to be numbers

    public static Object add(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a + b;
        }
        return toDouble(left) + toDouble(right);
    }

    public static Object subtract(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a - b;
        }
        return toDouble(left) - toDouble(right);
    }

    public static Object multiply(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a * b;
        }
        return toDouble(left) * toDouble(right);
    }

    public static Object divide(final Object left, final Object right) {
        return toDouble(left) / toDouble(right);
    }

    public static Object power(final Object left, final Object right) {
        return Math.pow(toDouble(left), toDouble(right));
    }

    /** The remainder that takes the sign of the divisor, as the language's does; null for a whole n % 0. */
    public static Object modulo(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            if (b == 0) {
                return null;
            }
            return Math.floorMod(a, b);
        }
        final double a = toDouble(left);
        final double b = toDouble(right);
        double m = a % b;
        if (m != 0 && (m < 0) != (b < 0)) {
            m += b;
        }
        return m;
    }

    /** Division rounded towards minus infinity; null for a whole n // 0. */
    public static Object floorDivide(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            if (b == 0) {
                return null;
            }
            return Math.floorDiv(a, b);
        }
        return Math.floor(toDouble(left) / toDouble(right));
    }

    public static Object negate(final Object number) {
        if (number instanceof Long whole) {
            return -whole;
        }
        return -toDouble(number);
    }

    /** Whether the two numbers say the same thing, whichever kind each is. */
    public static boolean equal(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a.longValue() == b.longValue();
        }
        return toDouble(left) == toDouble(right);
    }

    /** Whether the left number is smaller. */
    public static boolean less(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a < b;
        }
        return toDouble(left) < toDouble(right);
    }

    /** Whether the left number is smaller or the same. */
    public static boolean lessOrEqual(final Object left, final Object right) {
        if (left instanceof Long a && right instanceof Long b) {
            return a <= b;
        }
        return toDouble(left) <= toDouble(right);
    }
}
