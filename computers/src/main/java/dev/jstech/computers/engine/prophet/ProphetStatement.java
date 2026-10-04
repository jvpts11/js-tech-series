/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * A statement of YourIQL that the language's core does not have: a state the network is to keep, a watch, a state or
 * a watch let go, or the list of them.
 *
 * <pre>
 * KEEP steel_ingot &gt;= 10000
 * KEEP uranium_fuel BETWEEN 500 AND 1000
 * WATCH redstone &lt; 500 DO CRAFT redstone TO 1000
 * FORGET steel_ingot
 * FORGET WATCH 2
 * SHOW STATES
 * </pre>
 *
 * <p>Anything else is the language's core, which the engine hands on as it came. Pure logic, so its tests run
 * without the game.
 */
public sealed interface ProphetStatement {

    /** A KEEP with no upper end to its band. */
    long UNBOUNDED = Long.MAX_VALUE;

    /** What a statement that starts with one of YourIQL's words but goes on wrong is about, for the usage to show. */
    String BAD_KEEP = "keep";
    String BAD_WATCH = "watch";
    String BAD_FORGET = "forget";

    /**
     * Reads {@code input}: the statement it is, a {@link Malformed} one when it starts with one of YourIQL's own words
     * and goes on wrong, or null when it is the language's core.
     */
    @Nullable
    static ProphetStatement parse(final String input) {
        String text = input.strip();
        while (text.endsWith(";")) {
            text = text.substring(0, text.length() - 1).strip();
        }
        final String[] words = text.split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return null;
        }
        final String first = words[0].toUpperCase(Locale.ROOT);
        return switch (first) {
            case "KEEP" -> keep(words);
            case "WATCH" -> watch(text, words);
            case "FORGET" -> forget(words);
            case "SHOW" -> words.length == 2 && words[1].equalsIgnoreCase("STATES") ? new ShowStates() : null;
            default -> null;
        };
    }

    private static ProphetStatement keep(final String[] words) {
        if (words.length == 4 && words[2].equals(">=")) {
            final long lower = number(words[3]);
            return lower < 0 ? new Malformed(BAD_KEEP) : new Keep(words[1], lower, UNBOUNDED);
        }
        if (words.length == 6 && words[2].equalsIgnoreCase("BETWEEN") && words[4].equalsIgnoreCase("AND")) {
            final long lower = number(words[3]);
            final long upper = number(words[5]);
            return lower < 0 || upper < lower ? new Malformed(BAD_KEEP) : new Keep(words[1], lower, upper);
        }
        return new Malformed(BAD_KEEP);
    }

    private static ProphetStatement watch(final String text, final String[] words) {
        if (words.length < 6 || !words[4].equalsIgnoreCase("DO")) {
            return new Malformed(BAD_WATCH);
        }
        final Comparison comparison = Comparison.of(words[2]);
        final long threshold = number(words[3]);
        if (comparison == null || threshold < 0) {
            return new Malformed(BAD_WATCH);
        }
        // The action is the rest of the statement as written, from the word after DO.
        final int doAt = text.toUpperCase(Locale.ROOT).indexOf(" DO ");
        final String action = doAt < 0 ? "" : text.substring(doAt + 4).strip();
        return action.isEmpty() ? new Malformed(BAD_WATCH) : new Watch(words[1], comparison, threshold, action);
    }

    private static ProphetStatement forget(final String[] words) {
        if (words.length == 3 && words[1].equalsIgnoreCase("WATCH")) {
            final long number = number(words[2]);
            return number <= 0 || number > Integer.MAX_VALUE ? new Malformed(BAD_FORGET)
                    : new ForgetWatch((int) number);
        }
        return words.length == 2 ? new Forget(words[1]) : new Malformed(BAD_FORGET);
    }

    /* A whole number written in digits, or -1 when it is not one. */
    private static long number(final String word) {
        if (word.isEmpty() || word.length() > 18) {
            return -1;
        }
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isDigit(word.charAt(i))) {
                return -1;
            }
        }
        return Long.parseLong(word);
    }

    /**
     * Keep at least {@code lower} of {@code item}, counting what is on its way; an upper end only says where the
     * level stops being where it should be, since nothing is thrown away to bring it down.
     */
    record Keep(String item, long lower, long upper) implements ProphetStatement {
    }

    /** When {@code item} meets the comparison, run {@code action} once, and again only after it stopped meeting it. */
    record Watch(String item, Comparison comparison, long threshold, String action) implements ProphetStatement {
    }

    /** Let go of the state kept for {@code item}. */
    record Forget(String item) implements ProphetStatement {
    }

    /** Let go of watch number {@code number}. */
    record ForgetWatch(int number) implements ProphetStatement {
    }

    /** List the states and the watches. */
    record ShowStates() implements ProphetStatement {
    }

    /** One of YourIQL's own statements written wrong: {@code what} is one of the {@code BAD_} words. */
    record Malformed(String what) implements ProphetStatement {
    }

    /** How a watch compares a level with its threshold. */
    enum Comparison {
        BELOW("<"),
        AT_MOST("<="),
        ABOVE(">"),
        AT_LEAST(">=");

        private final String symbol;

        Comparison(final String symbol) {
            this.symbol = symbol;
        }

        /** The comparison written {@code symbol}, or null when there is none. */
        @Nullable
        static Comparison of(final String symbol) {
            for (final Comparison comparison : List.of(BELOW, AT_MOST, ABOVE, AT_LEAST)) {
                if (comparison.symbol.equals(symbol)) {
                    return comparison;
                }
            }
            return null;
        }

        /** How it is written. */
        String symbol() {
            return symbol;
        }

        /** Whether {@code level} meets it against {@code threshold}. */
        boolean test(final long level, final long threshold) {
            return switch (this) {
                case BELOW -> level < threshold;
                case AT_MOST -> level <= threshold;
                case ABOVE -> level > threshold;
                case AT_LEAST -> level >= threshold;
            };
        }
    }
}
