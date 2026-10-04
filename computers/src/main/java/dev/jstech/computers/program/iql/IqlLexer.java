/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Splits IQL source text into tokens for the parser. Pure logic, no Minecraft, so it is unit-tested
 * directly.
 *
 * <p>It does more than split on whitespace: it isolates the structural pieces the grammar needs even
 * when they are glued to their operands.
 * <ul>
 *   <li>Comparison operators ({@code = != < > <= >=}) split off, so {@code qty<100} reads as three
 *       tokens.</li>
 *   <li>Grouping parentheses ({@code (a OR b)}) become their own tokens.</li>
 *   <li>A parenthesis glued to the right of an identifier is a function call and stays inside the word,
 *       so {@code qty(cobblestone)} is one token and is never confused with grouping.</li>
 *   <li>Double-quoted runs become a single string token, letting a value carry spaces; so do single-quoted
 *       ones, a name as the SQL family writes it, two quotes standing for one inside it.</li>
 * </ul>
 */
final class IqlLexer {

    private IqlLexer() {
    }

    /** The lexical category of a token; the parser dispatches on it. */
    enum Type { WORD, NUMBER, OPERATOR, STRING, LPAREN, RPAREN }

    /** One lexical unit: its category and the exact source text it covers. */
    record Token(Type type, String text) {
    }

    static List<Token> lex(final String input) {
        return scan(input, null);
    }

    /**
     * Where each token {@link #lex} would read lies in {@code input}: its first character and the one past its
     * last, quotes included, in the same order, so the token a parser stopped on can be underlined.
     */
    static List<int[]> spans(final String input) {
        final List<int[]> spans = new ArrayList<>();
        scan(input, spans);
        return spans;
    }

    /* Reads the tokens, and where each lies when {@code spans} is given. */
    private static List<Token> scan(final String input, @Nullable final List<int[]> spans) {
        final List<Token> tokens = new ArrayList<>();
        final int n = input.length();
        int i = 0;
        while (i < n) {
            if (Character.isWhitespace(input.charAt(i))) {
                i++;
                continue;
            }
            final int start = i;
            final int before = tokens.size();
            i = next(input, i, tokens);
            if (spans != null && tokens.size() > before) {
                spans.add(new int[] {start, i});
            }
        }
        return tokens;
    }

    /* Reads the one token at {@code i}, which is not a space, and returns the index past it. */
    private static int next(final String input, final int i, final List<Token> tokens) {
        final int n = input.length();
        final char c = input.charAt(i);
        if (c == '"') {
            final int start = i + 1;
            int j = start;
            while (j < n && input.charAt(j) != '"') {
                j++;
            }
            tokens.add(new Token(Type.STRING, input.substring(start, j)));
            return (j < n) ? j + 1 : j; // step past the closing quote when present
        }
        if (c == '\'') {
            return readQuoted(input, i + 1, tokens);
        }
        if (c == '(') {
            tokens.add(new Token(Type.LPAREN, "("));
            return i + 1;
        }
        if (c == ')') {
            tokens.add(new Token(Type.RPAREN, ")"));
            return i + 1;
        }
        if (isOperatorChar(c)) {
            int j = i;
            while (j < n && isOperatorChar(input.charAt(j))) {
                j++;
            }
            tokens.add(new Token(Type.OPERATOR, input.substring(i, j)));
            return j;
        }
        return readWord(input, i, tokens);
    }

    /**
     * Reads a word, number, or function call starting at {@code start}. A '(' that follows word
     * characters opens a function call and is consumed (balanced) into the word; a '(' with no word
     * before it is left for the caller to read as a structural {@code LPAREN}.
     */
    private static int readWord(final String input, final int start, final List<Token> tokens) {
        final int n = input.length();
        final StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < n) {
            final char ch = input.charAt(i);
            if (Character.isWhitespace(ch) || isOperatorChar(ch) || ch == ')' || ch == '"') {
                break;
            }
            if (ch == '(') {
                if (sb.length() == 0) {
                    break; // structural '(', the main loop reads it as LPAREN
                }
                i = consumeBalancedCall(input, i, sb);
                break; // a function call ends the word
            }
            sb.append(ch);
            i++;
        }
        if (sb.length() > 0) {
            tokens.add(new Token(classify(sb.toString()), sb.toString()));
        }
        return i;
    }

    /** Appends a balanced {@code (...)} run to {@code sb}, returning the index just past it. */
    private static int consumeBalancedCall(final String input, final int start, final StringBuilder sb) {
        final int n = input.length();
        int depth = 0;
        int i = start;
        while (i < n) {
            final char cc = input.charAt(i);
            sb.append(cc);
            i++;
            if (cc == '(') {
                depth++;
            } else if (cc == ')') {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
        }
        return i;
    }

    /*
     * A single-quoted name, as the SQL family writes one: two quotes in a row stand for one inside it. Reads from
     * {@code start}, just past the opening quote, and returns the index past the closing one.
     */
    private static int readQuoted(final String input, final int start, final List<Token> tokens) {
        final int n = input.length();
        final StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < n) {
            final char ch = input.charAt(i);
            if (ch == '\'') {
                if (i + 1 < n && input.charAt(i + 1) == '\'') {
                    sb.append('\'');
                    i += 2;
                    continue;
                }
                i++;
                break;
            }
            sb.append(ch);
            i++;
        }
        tokens.add(new Token(Type.STRING, sb.toString()));
        return i;
    }

    private static boolean isOperatorChar(final char c) {
        return c == '=' || c == '!' || c == '<' || c == '>';
    }

    private static Type classify(final String text) {
        /*
         * A bare integer (optionally a percentage, e.g. "50%") is a number; everything else (item ids,
         * keywords, '*', function calls) is a word the parser interprets by position.
         */
        return text.matches("-?\\d+%?") ? Type.NUMBER : Type.WORD;
    }
}
