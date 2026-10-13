/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A statement of NextgreIQL taken apart: whether it asks for its plan to be shown ({@code EXPLAIN}), and run as well
 * ({@code EXPLAIN ANALYZE}); the statement of the network's language it is about; and the hints written after it,
 * which change the plan the engine makes ({@code PREFER SOURCE 'Storage B' MAX PARALLEL 2}).
 *
 * <p>Hints only stand at the end of a statement and outside its quotes, so a name in quotes that happens to read
 * like a hint stays a name. Which hints there are is the engine's own list and whatever other mods add to it; this
 * reads the words and leaves what they mean to the engine. Pure logic, so its tests run without the game.
 *
 * @param explain whether the plan is to be shown
 * @param analyze whether it is to be run as well, and shown with what it took
 * @param core    the statement of the network's language the plan is for, without the hints
 * @param hints   the hints, in the order they were written
 */
public record NextgreStatement(boolean explain, boolean analyze, String core, List<Hint> hints) {

    /** Takes the raw materials from the named server first. */
    public static final String PREFER_SOURCE = "PREFER SOURCE";
    /** Takes nothing from the named server while another one has it. */
    public static final String AVOID_SOURCE = "AVOID SOURCE";
    /** Runs at most that many stages of the craft at once. */
    public static final String MAX_PARALLEL = "MAX PARALLEL";
    /** Makes on a machine whatever both a machine and a bench make. */
    public static final String PREFER_MACHINE = "PREFER MACHINE";
    /** Makes on a bench whatever both a machine and a bench make. */
    public static final String PREFER_BENCH = "PREFER BENCH";

    /** The hints the engine knows of its own. */
    public static final List<Operator> BUILT_IN = List.of(new Operator(PREFER_SOURCE, true),
            new Operator(AVOID_SOURCE, true), new Operator(MAX_PARALLEL, true), new Operator(PREFER_MACHINE, false),
            new Operator(PREFER_BENCH, false));

    private static final String EXPLAIN = "EXPLAIN";
    private static final String ANALYZE = "ANALYZE";

    public NextgreStatement {
        hints = List.copyOf(hints);
    }

    /**
     * Reads {@code input}, knowing the hints {@code operators} beside the engine's own.
     *
     * @throws IllegalArgumentException when there is an EXPLAIN with nothing to explain
     */
    public static NextgreStatement parse(final String input, final List<Operator> operators) {
        String rest = input.strip();
        while (rest.endsWith(";")) {
            rest = rest.substring(0, rest.length() - 1).strip();
        }
        boolean explain = false;
        boolean analyze = false;
        if (startsWithWord(rest, EXPLAIN)) {
            explain = true;
            rest = rest.substring(EXPLAIN.length()).strip();
            if (startsWithWord(rest, ANALYZE)) {
                analyze = true;
                rest = rest.substring(ANALYZE.length()).strip();
            }
            if (rest.isEmpty()) {
                throw new IllegalArgumentException("EXPLAIN needs a statement to explain");
            }
        }
        final List<Operator> known = new ArrayList<>(BUILT_IN);
        known.addAll(operators);
        final List<Token> tokens = tokens(rest);
        for (int from = 1; from < tokens.size(); from++) {
            final List<Hint> hints = hintsFrom(tokens, from, known);
            if (hints != null) {
                return new NextgreStatement(explain, analyze, rest.substring(0, tokens.get(from).start()).strip(),
                        hints);
            }
        }
        return new NextgreStatement(explain, analyze, rest, List.of());
    }

    /** Whether {@code keyword} is among the hints, in any case. */
    public boolean has(final String keyword) {
        return value(keyword) != null;
    }

    /** The value written after {@code keyword} (empty for a hint that takes none), or null when it is not there. */
    public String value(final String keyword) {
        for (final Hint hint : hints) {
            if (hint.keyword().equalsIgnoreCase(keyword)) {
                return hint.value();
            }
        }
        return null;
    }

    /* The hints making up tokens[from..] exactly, or null when they do not. */
    private static List<Hint> hintsFrom(final List<Token> tokens, final int from, final List<Operator> known) {
        final List<Hint> out = new ArrayList<>();
        int at = from;
        while (at < tokens.size()) {
            Operator matched = null;
            int after = at;
            for (final Operator operator : known) {
                final String[] words = operator.keyword().split(" ");
                if (at + words.length > tokens.size()) {
                    continue;
                }
                boolean all = true;
                for (int w = 0; w < words.length; w++) {
                    final Token token = tokens.get(at + w);
                    all &= !token.quoted() && token.text().equalsIgnoreCase(words[w]);
                }
                // The longest keyword that fits wins, so one hint's words cannot cut another's short.
                if (all && (matched == null || words.length > matched.keyword().split(" ").length)) {
                    matched = operator;
                    after = at + words.length;
                }
            }
            if (matched == null) {
                return null;
            }
            String value = "";
            if (matched.takesValue()) {
                if (after >= tokens.size()) {
                    return null;
                }
                value = tokens.get(after).text();
                after++;
            }
            out.add(new Hint(matched.keyword(), value));
            at = after;
        }
        return out;
    }

    private static boolean startsWithWord(final String text, final String word) {
        return text.length() >= word.length() && text.substring(0, word.length()).equalsIgnoreCase(word)
                && (text.length() == word.length() || Character.isWhitespace(text.charAt(word.length())));
    }

    /* The words and quoted names of a statement, each with where it starts. */
    private static List<Token> tokens(final String text) {
        final List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            final char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            final int start = i;
            if (c == '\'' || c == '"') {
                int end = text.indexOf(c, i + 1);
                if (end < 0) {
                    end = text.length();
                }
                out.add(new Token(text.substring(i + 1, end), start, true));
                i = Math.min(text.length(), end + 1);
                continue;
            }
            while (i < text.length() && !Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '\''
                    && text.charAt(i) != '"') {
                i++;
            }
            out.add(new Token(text.substring(start, i), start, false));
        }
        return out;
    }

    /**
     * A hint as written.
     *
     * @param keyword the hint's words, as the engine or the mod that brought it spells them
     * @param value   what follows them, without its quotes, or empty for a hint that takes nothing
     */
    public record Hint(String keyword, String value) {

        /** The value as a whole number, or -1 when it is not one. */
        public int number() {
            try {
                return Integer.parseInt(value.strip());
            } catch (final NumberFormatException e) {
                return -1;
            }
        }

        /** How the hint reads, as a chip on the plan shows it. */
        public String written() {
            return value.isEmpty() ? keyword : keyword + " " + value;
        }
    }

    /**
     * A hint the dialect accepts.
     *
     * @param keyword    its words, in capitals, separated by single spaces
     * @param takesValue whether a value follows the words
     */
    public record Operator(String keyword, boolean takesValue) {

        public Operator {
            keyword = keyword.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        }
    }

    /* A word or a quoted name, and where it starts. */
    private record Token(String text, int start, boolean quoted) {
    }
}
