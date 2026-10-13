/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.vm.program.HoleFormat;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A format as {@code printf} reads one: text, with a hole wherever a percent sign says what goes there.
 *
 * <p>The holes are the ones anybody who wrote for those machines has in their fingers: {@code %d} and
 * {@code %i} for a whole number, {@code %f} for one with a fraction, {@code %s} for text, {@code %c} for a
 * character, and {@code %%} for the sign itself. The letters that say how long a number is ({@code l},
 * {@code ll}, {@code h}, {@code hh}) are taken and mean nothing, since {@code %ld} is what the hand writes for a
 * long.
 *
 * <p>The second version of the language reads the rest of what C wrote in a hole: flags, a width, a precision after
 * a dot, {@code %u}, {@code %x}, {@code %X} and {@code %o} for a whole number read as never below zero, and
 * {@code %e} and {@code %E} for a number with an exponent. A hole that asks for any of that is written down as a
 * call that puts the value in it the way C does; a plain one is the value itself, joined to the rest.
 *
 * <p>It is read when the program is compiled and never while it runs, which is why the format has to be written
 * out where it is used. What comes of it is pieces to be joined, so a line printed with plain holes is the very
 * line somebody adding the pieces together by hand would have written.
 */
@TextHolder
public final class PrintfFormat {

    /*
     * What is wrong with a format that does not read. A percent sign the player is meant to see is written
     * doubled, since a single one would be taken for a place an argument goes.
     */
    private static final TextKey ENDS_ON_PERCENT = TextKey.of("jsc.sigma.printf_format.ends_on_percent",
            "the format ends on a '%%' with no letter after it");
    private static final TextKey FROM_A_VALUE = TextKey.of("jsc.sigma.printf_format.from_a_value",
            "a width or a precision cannot come from a value here; write the number into the format");
    private static final TextKey TOO_WIDE = TextKey.of("jsc.sigma.printf_format.too_wide",
            "a width or a precision goes up to %s");
    private static final TextKey NO_SUCH_HOLE = TextKey.of("jsc.sigma.printf_format.no_such_hole",
            "'%%%s' is no hole printf has; there are %%d, %%i, %%u, %%x, %%X, %%o, %%f, %%e, %%E, %%s, %%c and %%%%");

    private static final String FLAGS = "-+ 0#";
    /** The letters that say how long a number is, which are read and mean nothing. */
    private static final Set<String> LENGTHS = Set.of("", "l", "ll", "h", "hh");
    /** The only one of those the first version took. */
    private static final String LONG = "l";
    /** The letters a hole of the first version could have, with nothing between them and the percent sign. */
    private static final String PLAIN_LETTERS = "difsc";
    /** The version that read the rest of what C wrote in a hole. */
    private static final int WIDTHS_CAME = 2;

    private PrintfFormat() {
    }

    /** What a hole takes. */
    @TextHolder
    public enum Wants {
        WHOLE_NUMBER(TextKey.of("jsc.sigma.printf_format.whole_number", "a whole number")),
        FRACTION(TextKey.of("jsc.sigma.printf_format.fraction", "a number")),
        TEXT(TextKey.of("jsc.sigma.printf_format.text", "text")),
        CHARACTER(TextKey.of("jsc.sigma.printf_format.character", "a character"));

        private final TextKey words;

        Wants(final TextKey words) {
            this.words = words;
        }

        /** The kind of value, in the words a message uses, read in the player's language. */
        public Text words() {
            return this.words.text();
        }
    }

    /**
     * A hole.
     *
     * @param letter  the letter after the percent sign
     * @param wants   what kind of value it takes
     * @param written the hole as it was written, which a message quotes back
     * @param spec    the hole as the runtime reads it, with the letters that say how long a number is left out
     * @param since   the version of the language that reads it
     */
    public record Hole(char letter, Wants wants, String written, String spec, int since) {

        /** Whether it asks for nothing but the value, which is then joined to the rest as it stands. */
        public boolean plain() {
            return this.spec.length() == 2 && PLAIN_LETTERS.indexOf(this.letter) >= 0;
        }
    }

    /**
     * A format that was read.
     *
     * @param pieces  the text and the holes in the order they come, each a {@code String} or a {@link Hole}
     * @param problem what is wrong with the format, or null when it reads
     */
    public record Read(List<Object> pieces, Text problem) {

        public Read {
            pieces = List.copyOf(pieces);
        }

        /** How many values the format takes. */
        public int holes() {
            int count = 0;
            for (final Object piece : this.pieces) {
                count += piece instanceof Hole ? 1 : 0;
            }
            return count;
        }
    }

    /** Reads a format. */
    public static Read read(final String format) {
        final List<Object> pieces = new ArrayList<>();
        final StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < format.length()) {
            final char c = format.charAt(i++);
            if (c != '%') {
                text.append(c);
                continue;
            }
            if (i < format.length() && format.charAt(i) == '%') {
                text.append('%');
                i++;
                continue;
            }
            int at = skip(format, i, FLAGS);
            at = skipDigits(format, at);
            if (at < format.length() && format.charAt(at) == '.') {
                at = skipDigits(format, at + 1);
            }
            if (at < format.length() && format.charAt(at) == '*') {
                return new Read(pieces, FROM_A_VALUE.text());
            }
            final String shape = format.substring(i, at);
            final int lengthStart = at;
            at = skip(format, at, "lh");
            final String length = format.substring(lengthStart, at);
            if (!LENGTHS.contains(length)) {
                return new Read(pieces, NO_SUCH_HOLE.with(length));
            }
            if (at >= format.length()) {
                return new Read(pieces, ENDS_ON_PERCENT.text());
            }
            final char letter = format.charAt(at++);
            final Wants wants = wantsOf(letter);
            if (wants == null) {
                return new Read(pieces, NO_SUCH_HOLE.with(letter));
            }
            final String spec = "%" + shape + letter;
            if (HoleFormat.read(spec) == null) {
                return new Read(pieces, TOO_WIDE.with(HoleFormat.MOST));
            }
            if (!text.isEmpty()) {
                pieces.add(text.toString());
                text.setLength(0);
            }
            final boolean first = shape.isEmpty() && (length.isEmpty() || LONG.equals(length))
                    && PLAIN_LETTERS.indexOf(letter) >= 0;
            pieces.add(new Hole(letter, wants, format.substring(i - 1, at), spec,
                    first ? SigmaVersions.FIRST : WIDTHS_CAME));
            i = at;
        }
        if (!text.isEmpty()) {
            pieces.add(text.toString());
        }
        return new Read(pieces, null);
    }

    private static Wants wantsOf(final char letter) {
        return switch (letter) {
            case 'd', 'i', 'u', 'x', 'X', 'o' -> Wants.WHOLE_NUMBER;
            case 'f', 'e', 'E' -> Wants.FRACTION;
            case 's' -> Wants.TEXT;
            case 'c' -> Wants.CHARACTER;
            default -> null;
        };
    }

    /** Where the run of characters from {@code set} that starts at {@code from} ends. */
    private static int skip(final String format, final int from, final String set) {
        int at = from;
        while (at < format.length() && set.indexOf(format.charAt(at)) >= 0) {
            at++;
        }
        return at;
    }

    private static int skipDigits(final String format, final int from) {
        return skip(format, from, "0123456789");
    }
}
