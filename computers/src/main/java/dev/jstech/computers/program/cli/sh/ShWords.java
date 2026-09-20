/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a shell does to a word before anything sees it: puts in what a name stands for, and opens out a name
 * with a star in it into the names it matches.
 *
 * <p>Both are as old as shells and neither is the machine's business: this is given the words and, for the
 * stars, the names there are, and answers with words. What names there are is asked of the disk by whoever
 * calls this, which is why nothing here knows what a disk is.
 */
public final class ShWords {

    private ShWords() {
    }

    /**
     * Puts in what each name stands for: {@code $HOME} and {@code ${HOME}} alike, and {@code %HOME%} for the
     * family that writes it that way.
     *
     * <p>A name nothing stands for comes out as nothing, which is what every shell does and what makes a
     * misspelt name show itself at once.
     */
    public static String expand(final String word, final Map<String, String> named, final boolean dosStyle) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return dosStyle ? expandPercent(word, named) : expandDollar(word, named);
    }

    /**
     * The names that word matches, or the word itself when it matches nothing and when it has no star in it.
     *
     * <p>A pattern that matches nothing is left alone rather than dropped, so {@code ls *.sgs} on a folder with
     * none of them says there is no such file instead of listing everything, which is what a person means.
     */
    public static List<String> glob(final String word, final List<String> names) {
        if (word == null || (word.indexOf('*') < 0 && word.indexOf('?') < 0)) {
            return List.of(word == null ? "" : word);
        }
        final List<String> matched = new ArrayList<>();
        for (final String name : names) {
            if (matches(word, name)) {
                matched.add(name);
            }
        }
        return matched.isEmpty() ? List.of(word) : List.copyOf(matched);
    }

    /**
     * Whether a name answers to a pattern of stars and question marks.
     *
     * <p>Walked rather than turned into a regular expression: a name a player typed can hold anything at all,
     * and a pattern that costs nothing to walk cannot be written to cost a server anything either.
     */
    public static boolean matches(final String pattern, final String name) {
        return walk(pattern.toLowerCase(Locale.ROOT), 0, name.toLowerCase(Locale.ROOT), 0);
    }

    private static boolean walk(final String pattern, final int p, final String name, final int n) {
        if (p == pattern.length()) {
            return n == name.length();
        }
        final char ch = pattern.charAt(p);
        if (ch == '*') {
            for (int i = n; i <= name.length(); i++) {
                if (walk(pattern, p + 1, name, i)) {
                    return true;
                }
            }
            return false;
        }
        if (n == name.length()) {
            return false;
        }
        if (ch == '?' || ch == name.charAt(n)) {
            return walk(pattern, p + 1, name, n + 1);
        }
        return false;
    }

    private static String expandDollar(final String word, final Map<String, String> named) {
        final StringBuilder out = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); i++) {
            final char ch = word.charAt(i);
            if (ch != '$' || i + 1 >= word.length()) {
                out.append(ch);
                continue;
            }
            final boolean braced = word.charAt(i + 1) == '{';
            int from = i + (braced ? 2 : 1);
            int to = from;
            while (to < word.length() && (braced ? word.charAt(to) != '}' : nameLetter(word.charAt(to)))) {
                to++;
            }
            if (to == from) {
                out.append(ch);
                continue;
            }
            out.append(standingFor(word.substring(from, to), named));
            i = braced ? to : to - 1;
        }
        return out.toString();
    }

    private static String expandPercent(final String word, final Map<String, String> named) {
        final StringBuilder out = new StringBuilder(word.length());
        int i = 0;
        while (i < word.length()) {
            final int open = word.indexOf('%', i);
            final int close = open < 0 ? -1 : word.indexOf('%', open + 1);
            if (open < 0 || close < 0) {
                out.append(word.substring(i));
                break;
            }
            out.append(word, i, open).append(standingFor(word.substring(open + 1, close), named));
            i = close + 1;
        }
        return out.toString();
    }

    /** Names are read without regard to case, since one family of systems has never regarded it. */
    private static String standingFor(final String name, final Map<String, String> named) {
        final String value = named.get(name);
        if (value != null) {
            return value;
        }
        for (final Map.Entry<String, String> entry : named.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static boolean nameLetter(final char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
