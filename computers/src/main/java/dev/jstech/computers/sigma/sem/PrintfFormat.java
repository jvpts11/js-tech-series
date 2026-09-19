/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import java.util.ArrayList;
import java.util.List;

/**
 * A format as {@code printf} reads one: text, with a hole wherever a percent sign says what goes there.
 *
 * <p>The holes are the ones anybody who wrote for those machines has in their fingers: {@code %d} and
 * {@code %i} for a whole number, {@code %f} for one with a fraction, {@code %s} for text, {@code %c} for a
 * character, and {@code %%} for the sign itself. An {@code l} before the letter is taken and means nothing,
 * since {@code %ld} is what the hand writes for a long. Widths and precisions are not here: a format that asks
 * for one is told so, rather than printed wrong.
 *
 * <p>It is read when the program is compiled and never while it runs, which is why the format has to be written
 * out where it is used. What comes of it is pieces to be joined, so a line printed this way is the very line
 * somebody adding the pieces together by hand would have written.
 */
public final class PrintfFormat {

    private PrintfFormat() {
    }

    /** What a hole takes. */
    public enum Wants {
        WHOLE_NUMBER("a whole number"),
        FRACTION("a number"),
        TEXT("text"),
        CHARACTER("a character");

        private final String words;

        Wants(final String words) {
            this.words = words;
        }

        /** The kind of value, in the words a message uses. */
        public String words() {
            return this.words;
        }
    }

    /**
     * A hole, by the letter it was written with.
     *
     * @param letter the letter after the percent sign, which a message quotes back
     */
    public record Hole(char letter, Wants wants) {
    }

    /**
     * A format that was read.
     *
     * @param pieces  the text and the holes in the order they come, each a {@code String} or a {@link Hole}
     * @param problem what is wrong with the format, or null when it reads
     */
    public record Read(List<Object> pieces, String problem) {

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
        for (int i = 0; i < format.length(); i++) {
            final char c = format.charAt(i);
            if (c != '%') {
                text.append(c);
                continue;
            }
            if (i + 1 >= format.length()) {
                return new Read(pieces, "the format ends on a '%' with no letter after it");
            }
            char letter = format.charAt(++i);
            if (letter == '%') {
                text.append('%');
                continue;
            }
            if (letter == 'l' && i + 1 < format.length()) {
                letter = format.charAt(++i);
            }
            final Wants wants = wantsOf(letter);
            if (wants == null) {
                return new Read(pieces, Character.isDigit(letter) || letter == '.' || letter == '-'
                        ? "there are no widths or precisions here; write %d, %f, %s, %c or %%"
                        : "'%" + letter + "' is no hole this printf has; there is %d, %f, %s, %c and %%");
            }
            if (!text.isEmpty()) {
                pieces.add(text.toString());
                text.setLength(0);
            }
            pieces.add(new Hole(letter, wants));
        }
        if (!text.isEmpty()) {
            pieces.add(text.toString());
        }
        return new Read(pieces, null);
    }

    private static Wants wantsOf(final char letter) {
        return switch (letter) {
            case 'd', 'i' -> Wants.WHOLE_NUMBER;
            case 'f' -> Wants.FRACTION;
            case 's' -> Wants.TEXT;
            case 'c' -> Wants.CHARACTER;
            default -> null;
        };
    }
}
