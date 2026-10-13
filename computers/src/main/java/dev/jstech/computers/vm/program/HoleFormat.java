/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * One hole of a {@code printf} format that asks for more than the value as it stands: a width, a precision, flags,
 * or a base other than ten. A value is put in it the way C puts one.
 *
 * <p>The hole is written the way C writes it, less the letters that say how long a number is, which mean nothing
 * here: a percent sign, the flags ({@code -} to the left, {@code +} and a space for the sign, {@code 0} to fill with
 * zeros, {@code #} for the base's mark), a width, a precision after a dot, and the letter. {@code %d} and {@code %i}
 * are a whole number, {@code %u} the same read as never below zero, {@code %x}, {@code %X} and {@code %o} the same in
 * hexadecimal and octal, {@code %f} a number with the precision's count of decimals, {@code %e} and {@code %E} one
 * with an exponent, {@code %s} text cut to the precision, and {@code %c} a character.
 *
 * <p>A number with a fraction and no precision is written as the language writes it, the same as a plain
 * {@code %f}, so a width only pads it; C's six decimals are asked for with {@code %.6f}.
 *
 * @param flags     the flags, in the order they were written
 * @param width     the width the value is padded to, or {@link #NONE}
 * @param precision the precision, or {@link #NONE}
 * @param letter    the letter that says what the value is written as
 */
public record HoleFormat(String flags, int width, int precision, char letter) {

    /** No width, or no precision. */
    public static final int NONE = -1;
    /** The widest a width or a precision may be, which keeps one hole from filling a program's memory. */
    public static final int MOST = 999;

    private static final String FLAGS = "-+ 0#";
    private static final String LETTERS = "diuxXofeEsc";
    /** How many decimals an exponent is written with when the hole does not say, as in C. */
    private static final int EXPONENT_DECIMALS = 6;

    /** Reads a hole written as {@code %-08.3x}, or gives null when it is not one this runtime writes. */
    public static HoleFormat read(final String spec) {
        if (spec == null || spec.length() < 2 || spec.charAt(0) != '%') {
            return null;
        }
        int at = 1;
        final StringBuilder flags = new StringBuilder();
        while (at < spec.length() && FLAGS.indexOf(spec.charAt(at)) >= 0) {
            flags.append(spec.charAt(at++));
        }
        final int widthStart = at;
        while (at < spec.length() && Character.isDigit(spec.charAt(at))) {
            at++;
        }
        final int width = number(spec, widthStart, at);
        int precision = NONE;
        if (at < spec.length() && spec.charAt(at) == '.') {
            final int precisionStart = ++at;
            while (at < spec.length() && Character.isDigit(spec.charAt(at))) {
                at++;
            }
            precision = at == precisionStart ? 0 : number(spec, precisionStart, at);
        }
        if (at != spec.length() - 1 || LETTERS.indexOf(spec.charAt(at)) < 0 || width > MOST || precision > MOST) {
            return null;
        }
        return new HoleFormat(flags.toString(), width, precision, spec.charAt(at));
    }

    /** The value put in the hole: a number, a character or text, as the letter says. */
    public String apply(final Object value) {
        return switch (this.letter) {
            case 'd', 'i' -> this.whole(value);
            case 'u', 'x', 'X', 'o' -> this.unsigned(value);
            case 'f' -> this.fraction(value);
            case 'e', 'E' -> this.exponent(value);
            case 'c' -> this.padded("", String.valueOf((char) Numbers.toInt(value)), false);
            default -> this.text(value);
        };
    }

    private String whole(final Object value) {
        final long number = Numbers.toLong(value);
        final String digits = number < 0 ? Long.toUnsignedString(-number) : Long.toString(number);
        return this.padded(this.sign(number < 0), this.atLeast(digits, number == 0), true);
    }

    /** A whole number read as never below zero, in the hole's base, as wide as the value's own type. */
    private String unsigned(final Object value) {
        final long bits = value instanceof Long whole ? whole : Integer.toUnsignedLong(Numbers.toInt(value));
        String digits = switch (this.letter) {
            case 'x' -> Long.toHexString(bits);
            case 'X' -> Long.toHexString(bits).toUpperCase(Locale.ROOT);
            case 'o' -> Long.toOctalString(bits);
            default -> Long.toUnsignedString(bits);
        };
        digits = this.atLeast(digits, bits == 0);
        String mark = "";
        if (this.has('#') && this.letter == 'o' && !digits.startsWith("0")) {
            digits = "0" + digits;
        } else if (this.has('#') && bits != 0 && (this.letter == 'x' || this.letter == 'X')) {
            mark = this.letter == 'x' ? "0x" : "0X";
        }
        return this.padded(mark, digits, true);
    }

    private String fraction(final Object value) {
        final double number = Numbers.toDouble(value);
        if (!Double.isFinite(number)) {
            return this.notFinite(number);
        }
        final String digits = this.precision == NONE ? asWritten(value)
                : new BigDecimal(Math.abs(number)).setScale(this.precision, RoundingMode.HALF_EVEN).toPlainString();
        return this.padded(this.sign(number < 0 || isNegativeZero(number)), digits, true);
    }

    /** A number's size as the language writes the number when it is joined to text: a whole one with no fraction. */
    private static String asWritten(final Object value) {
        if (value instanceof Float real) {
            return String.valueOf(Math.abs(real));
        }
        if (value instanceof Double real) {
            return String.valueOf(Math.abs(real));
        }
        final long whole = Numbers.toLong(value);
        return whole < 0 ? Long.toUnsignedString(-whole) : Long.toString(whole);
    }

    private String exponent(final Object value) {
        final double number = Numbers.toDouble(value);
        if (!Double.isFinite(number)) {
            return this.notFinite(number);
        }
        final int decimals = this.precision == NONE ? EXPONENT_DECIMALS : this.precision;
        final String digits = withExponent(Math.abs(number), decimals, this.letter == 'E' ? 'E' : 'e');
        return this.padded(this.sign(number < 0 || isNegativeZero(number)), digits, true);
    }

    /**
     * A size written as one digit, the decimals, and the power of ten: {@code 1.234560e+02}. The exact value is
     * rounded half to even, which is what C does with the number the machine holds.
     */
    private static String withExponent(final double size, final int decimals, final char mark) {
        BigDecimal exact = new BigDecimal(size);
        int power = 0;
        if (exact.signum() != 0) {
            power = exact.precision() - exact.scale() - 1;
            exact = exact.movePointLeft(power).setScale(decimals, RoundingMode.HALF_EVEN);
            if (exact.compareTo(BigDecimal.TEN) >= 0) {
                // Rounding carried into another digit, as 9.99 to one decimal does.
                power++;
                exact = exact.movePointLeft(1).setScale(decimals, RoundingMode.HALF_EVEN);
            }
        } else {
            exact = exact.setScale(decimals, RoundingMode.HALF_EVEN);
        }
        final String exponent = Integer.toString(Math.abs(power));
        return exact.toPlainString() + mark + (power < 0 ? '-' : '+') + (exponent.length() < 2 ? "0" : "")
                + exponent;
    }

    /** Infinity and a value that is no number, the way C writes them, never filled with zeros. */
    private String notFinite(final double number) {
        String word = Double.isNaN(number) ? "nan" : "inf";
        if (this.letter == 'E') {
            word = word.toUpperCase(Locale.ROOT);
        }
        return this.padded(this.sign(number < 0), word, false);
    }

    private String text(final Object value) {
        final String whole = String.valueOf(value);
        return this.padded("", this.precision != NONE && this.precision < whole.length()
                ? whole.substring(0, this.precision) : whole, false);
    }

    /** What goes before a number: its minus, or the plus or the space the flags ask for. */
    private String sign(final boolean negative) {
        if (negative) {
            return "-";
        }
        return this.has('+') ? "+" : this.has(' ') ? " " : "";
    }

    /**
     * A whole number's digits with at least the precision's count, zeros in front; a precision of none on a zero
     * writes no digits at all, as in C.
     */
    private String atLeast(final String digits, final boolean zero) {
        if (this.precision == NONE) {
            return digits;
        }
        if (this.precision == 0 && zero) {
            return "";
        }
        return digits.length() >= this.precision ? digits : "0".repeat(this.precision - digits.length()) + digits;
    }

    /**
     * The pieces padded to the width: spaces in front, or behind when the hole is to the left, or zeros between the
     * sign and the digits when the hole asks for them and nothing else decides the digits.
     */
    private String padded(final String front, final String digits, final boolean numeric) {
        final int missing = this.width - front.length() - digits.length();
        if (missing <= 0) {
            return front + digits;
        }
        if (this.has('-')) {
            return front + digits + " ".repeat(missing);
        }
        final boolean zeros = numeric && this.has('0')
                && (this.precision == NONE || this.letter == 'f' || this.letter == 'e' || this.letter == 'E');
        return zeros ? front + "0".repeat(missing) + digits : " ".repeat(missing) + front + digits;
    }

    private boolean has(final char flag) {
        return this.flags.indexOf(flag) >= 0;
    }

    private static boolean isNegativeZero(final double number) {
        return number == 0 && Double.doubleToRawLongBits(number) != 0;
    }

    private static int number(final String spec, final int from, final int to) {
        if (from == to) {
            return NONE;
        }
        // More digits than MOST has is refused by the caller; reading them as MOST + 1 keeps an overflow out.
        return to - from > 3 ? MOST + 1 : Integer.parseInt(spec.substring(from, to));
    }
}
