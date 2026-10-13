/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import dev.jstech.computers.program.cli.CliTokenizer;
import java.util.ArrayList;
import java.util.List;

/**
 * A word of a line as the shell holds it: what it says, and whether it is one of the shell's own marks (a pipe, an
 * arrow, the ampersand that sends a line to the background) or a word for a command.
 *
 * <p>Which one it is was settled by how it was typed, before any name in it was put in or any star opened out: a
 * file named {@code >old.txt} that a star matched, or a name whose value is a pipe, is a word like any other, and
 * an arrow written in quotes is one too. Read from the text afterwards instead, it could make a command write over
 * a file nobody named.
 *
 * @param text     what it says
 * @param operator whether it is one of the shell's marks
 */
public record ShWord(String text, boolean operator) {

    /** The word as typed: a mark only when it was written bare and reads as one. */
    public static ShWord typed(final CliTokenizer.Token token) {
        return new ShWord(token.text(), !token.quoted() && isOperator(token.text()));
    }

    /** Every token of a line as typed. */
    public static List<ShWord> typed(final List<CliTokenizer.Token> tokens) {
        final List<ShWord> words = new ArrayList<>(tokens.size());
        for (final CliTokenizer.Token token : tokens) {
            words.add(typed(token));
        }
        return words;
    }

    /** A word for a command, whatever it reads as. */
    public static ShWord plain(final String text) {
        return new ShWord(text, false);
    }

    /** What the words say, in order. */
    public static List<String> texts(final List<ShWord> words) {
        final List<String> texts = new ArrayList<>(words.size());
        for (final ShWord word : words) {
            texts.add(word.text());
        }
        return texts;
    }

    /** Whether a bare word reads as one of the shell's marks: a pipe, an ampersand, or an arrow with its file. */
    public static boolean isOperator(final String text) {
        return text.equals("|") || text.equals("&") || text.startsWith(">") || text.startsWith("<");
    }

    /** Whether this is the mark that sends a line to the background. */
    public boolean isBackground() {
        return this.operator && this.text.equals("&");
    }
}
