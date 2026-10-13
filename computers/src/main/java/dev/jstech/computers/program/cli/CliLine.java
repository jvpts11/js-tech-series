/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * One line of console output: the coloured runs it is made of, in order. Immutable, so a command's output can
 * be streamed to the client and replayed without surprises.
 *
 * <p>Nearly every line is one run in one colour, and is still written that way. The ones that are not are the
 * ones a real tool colours in parts, which is how a terminal full of a package manager's output looks like one.
 *
 * <p>A line is words not yet in a language: {@link #resolve} puts it in one, which is what a player's screen does
 * with the player's own, and {@link #text()} is what it says in English, the language the machine keeps it in when
 * it is piped on or written down.
 */
public record CliLine(List<CliSpan> spans) {

    public CliLine {
        spans = spans == null ? List.of() : List.copyOf(spans);
    }

    /** A line of words that are data, in one colour. */
    public CliLine(final String text, final CliStyle style) {
        this(List.of(new CliSpan(text, style)));
    }

    /** A line of those words in one colour. */
    public CliLine(final Text text, final CliStyle style) {
        this(List.of(new CliSpan(text, style)));
    }

    public static CliLine plain(final String text) {
        return new CliLine(text, CliStyle.PLAIN);
    }

    public static CliLine plain(final Text text) {
        return new CliLine(text, CliStyle.PLAIN);
    }

    /** A declared sentence as a line in that colour. */
    public static CliLine of(final TextKey key, final CliStyle style) {
        return new CliLine(key.text(), style);
    }

    /** A line made of those runs, in that order. */
    public static CliLine of(final CliSpan... spans) {
        return new CliLine(List.of(spans));
    }

    /** Starts a line that is put together a run at a time. */
    public static Builder build() {
        return new Builder();
    }

    /**
     * The line in that language, run by run, with its dots laid out now that the words either side of them are
     * known. Runs that come to nothing are left out.
     */
    public List<CliRun> resolve(final ITextLanguage language) {
        final String[] words = new String[this.spans.size()];
        for (int i = 0; i < words.length; i++) {
            final CliSpan span = this.spans.get(i);
            words[i] = span.fill() == null ? span.text().resolve(language) : null;
        }
        int before = 0;
        for (int i = 0; i < words.length; i++) {
            final CliSpan.Fill fill = this.spans.get(i).fill();
            if (fill != null) {
                words[i] = fill.render(before, wordsAfter(words, i));
            }
            before += words[i].length();
        }
        final List<CliRun> out = new ArrayList<>(words.length);
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                out.add(new CliRun(words[i], this.spans.get(i).style()));
            }
        }
        return out;
    }

    /** What the line says in that language, colours aside. */
    public String text(final ITextLanguage language) {
        final StringBuilder out = new StringBuilder();
        for (final CliRun run : resolve(language)) {
            out.append(run.text());
        }
        return out.toString();
    }

    /** What the line says in English, colours aside: what a pipe carries on and a file keeps. */
    public String text() {
        return text(ITextLanguage.ENGLISH);
    }

    /**
     * The colour the line opens in, which for a line of one run is simply its colour.
     *
     * <p>Kept for whatever only ever asks one thing of a line: whether it is an error, a warning, a heading.
     */
    public CliStyle style() {
        return this.spans.isEmpty() ? CliStyle.PLAIN : this.spans.getFirst().style();
    }

    /** How long the words after a fill come to; a later fill counts as nothing, since it is laid out after. */
    private static int wordsAfter(final String[] words, final int at) {
        int after = 0;
        for (int i = at + 1; i < words.length; i++) {
            if (words[i] != null) {
                after += words[i].length();
            }
        }
        return after;
    }

    /** Puts a line together a run at a time, which reads better than a list of constructors. */
    public static final class Builder {

        private final List<CliSpan> spans = new ArrayList<>();

        private Builder() {
        }

        public Builder plain(final String text) {
            return this.add(text, CliStyle.PLAIN);
        }

        public Builder plain(final Text text) {
            return this.add(text, CliStyle.PLAIN);
        }

        /** Words that are data, in that colour; nothing is added for none. */
        public Builder add(final String text, final CliStyle style) {
            if (text != null && !text.isEmpty()) {
                this.spans.add(new CliSpan(text, style));
            }
            return this;
        }

        public Builder add(final Text text, final CliStyle style) {
            this.spans.add(new CliSpan(text, style));
            return this;
        }

        public Builder add(final TextKey key, final CliStyle style) {
            return this.add(key.text(), style);
        }

        /** Every run of another line, after what is here already. */
        public Builder add(final CliLine line) {
            this.spans.addAll(line.spans());
            return this;
        }

        public CliLine done() {
            return new CliLine(this.spans);
        }
    }
}
