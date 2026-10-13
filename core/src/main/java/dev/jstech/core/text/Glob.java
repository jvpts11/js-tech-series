/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.Locale;

/**
 * Patterns of stars and question marks, matched against a name without regard to case: a star stands for any run of
 * characters, none included, and a question mark, where the pattern's language has one, for any single character.
 *
 * <p>Matched in one pass with one step back to the last star, never by trying every way a star could be split: a
 * pattern a player types can hold any number of stars, and the obvious recursive match costs a number of steps that
 * grows with the name raised to the number of stars. Here the cost is at most the pattern's length times the name's.
 */
public final class Glob {

    private Glob() {
    }

    /**
     * Whether {@code text} answers to {@code pattern}.
     *
     * @param questionMarks whether a question mark stands for any one character; otherwise it is a character like
     *                      any other
     */
    public static boolean matches(final String pattern, final String text, final boolean questionMarks) {
        final String p = pattern.toLowerCase(Locale.ROOT);
        final String t = text.toLowerCase(Locale.ROOT);
        int at = 0;
        int read = 0;
        int lastStar = -1;
        int starRead = 0;
        while (read < t.length()) {
            final char wanted = at < p.length() ? p.charAt(at) : 0;
            if (at < p.length() && wanted != '*' && (wanted == t.charAt(read) || questionMarks && wanted == '?')) {
                at++;
                read++;
            } else if (at < p.length() && wanted == '*') {
                lastStar = at++;
                starRead = read;
            } else if (lastStar >= 0) {
                // The last star takes one character more, and the pattern after it is tried again from there.
                at = lastStar + 1;
                read = ++starRead;
            } else {
                return false;
            }
        }
        while (at < p.length() && p.charAt(at) == '*') {
            at++;
        }
        return at == p.length();
    }

    /** Whether a word holds a star, or a question mark where those stand for a character, so it is a pattern. */
    public static boolean isPattern(final String word, final boolean questionMarks) {
        return word.indexOf('*') >= 0 || questionMarks && word.indexOf('?') >= 0;
    }
}
