/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import org.jetbrains.annotations.Nullable;

/**
 * A run of text in one colour, which is what a line at a terminal is made of.
 *
 * <p>Real tools colour parts of a line, not lines: the green arrows a package manager opens with, the blue
 * brackets round its {@code ok}, a flag in red beside one in blue. A line that can only be one colour cannot
 * say any of that, so a line is a list of these.
 *
 * <p>The words are {@link Text}, put in a language only where the line is read: a player's screen shows them in
 * that player's language, and what the machine keeps (a pipe, a file) has them in English. A run can also be a
 * {@link Fill}, the dots between a label and its value, whose length is only known once the words around it are.
 *
 * @param fill the dots this run stands for, or null for a run of words
 */
public record CliSpan(Text text, CliStyle style, @Nullable Fill fill) {

    public CliSpan {
        if (text == null) {
            text = Text.EMPTY;
        }
        if (style == null) {
            style = CliStyle.PLAIN;
        }
    }

    /** A run of those words in that colour. */
    public CliSpan(final Text text, final CliStyle style) {
        this(text, style, null);
    }

    /** A run of words that are data, the same in every language: a name, a path, a number, what was typed. */
    public CliSpan(final String text, final CliStyle style) {
        this(Text.literal(text), style, null);
    }

    /** A run in the terminal's ordinary colour. */
    public static CliSpan plain(final String text) {
        return new CliSpan(text, CliStyle.PLAIN);
    }

    /** A run in the terminal's ordinary colour. */
    public static CliSpan plain(final Text text) {
        return new CliSpan(text, CliStyle.PLAIN);
    }

    /** A declared sentence in that colour. */
    public static CliSpan of(final TextKey key, final CliStyle style) {
        return new CliSpan(key.text(), style);
    }

    /** The dots that carry what follows them to that column; see {@link Fill}. */
    public static CliSpan fill(final int column, final boolean closing) {
        return new CliSpan(Text.EMPTY, CliStyle.PLAIN, new Fill(column, closing));
    }

    /** The run in English, the machine's language; a fill reads as nothing until it is laid out in a line. */
    public String english() {
        return this.text.english();
    }

    /**
     * Dots that line up what follows them, laid out once the words on either side are in a language.
     *
     * <p>Two ways of lining up. A closing fill pushes what follows it to end at {@code column}, which is how a figure
     * is read off a listing, right against the edge. An opening one makes what follows it begin at {@code column},
     * which is how a list of names and what each does keeps every description starting in the same place.
     *
     * @param column  where what follows ends (closing) or begins (opening)
     * @param closing whether what follows ends at the column rather than begins at it
     */
    public record Fill(int column, boolean closing) {

        /**
         * The dots, with a space either side, given how much of the line comes before them and after them. A line
         * with no room for dots keeps the words apart all the same: two spaces before a figure, one before a
         * description, so each still reads as two things rather than one.
         */
        public String render(final int before, final int after) {
            final int room = this.closing ? this.column - before - after : this.column - before;
            if (room < 2) {
                return this.closing ? "  " : " ";
            }
            return " " + ".".repeat(room - 2) + " ";
        }
    }
}
