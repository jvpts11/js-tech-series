/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line into tokens on whitespace, honouring single and double quotes so an item name with spaces can
 * be passed as one argument ({@code select 4 "oak planks"}). Pure and side-effect free.
 */
public final class CliTokenizer {

    private CliTokenizer() {
    }

    /** The tokens of a line, as text. */
    public static List<String> tokenize(final String line) {
        final List<Token> tokens = words(line);
        final List<String> texts = new ArrayList<>(tokens.size());
        for (final Token token : tokens) {
            texts.add(token.text());
        }
        return texts;
    }

    /**
     * The tokens of a line, each saying whether any of it was written in quotes: a quoted {@code ">"} is a word to
     * hand to a command, where the same arrow written bare is the shell's own.
     */
    public static List<Token> words(final String line) {
        final List<Token> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }
        final StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean inToken = false;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
                inToken = true;
                quoted = true;
            } else if (Character.isWhitespace(c)) {
                if (inToken) {
                    tokens.add(new Token(current.toString(), quoted));
                    current.setLength(0);
                    inToken = false;
                    quoted = false;
                }
            } else {
                current.append(c);
                inToken = true;
            }
        }
        if (inToken) {
            tokens.add(new Token(current.toString(), quoted));
        }
        return tokens;
    }

    /**
     * One token of a line.
     *
     * @param text   what it says, its quotes taken off
     * @param quoted whether any of it was written in quotes
     */
    public record Token(String text, boolean quoted) {
    }
}
