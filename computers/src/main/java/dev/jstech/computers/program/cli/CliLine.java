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
 * One line of console output: the coloured runs it is made of, in order. Immutable, so a command's output can
 * be streamed to the client and replayed without surprises.
 *
 * <p>Nearly every line is one run in one colour, and is still written that way. The ones that are not are the
 * ones a real tool colours in parts, which is how a terminal full of a package manager's output looks like one.
 */
public record CliLine(List<CliSpan> spans) {

    public CliLine {
        spans = spans == null ? List.of() : List.copyOf(spans);
    }

    /** A line that is one run in one colour. */
    public CliLine(final String text, final CliStyle style) {
        this(List.of(new CliSpan(text, style)));
    }

    public static CliLine plain(final String text) {
        return new CliLine(text, CliStyle.PLAIN);
    }

    /** A line made of those runs, in that order. */
    public static CliLine of(final CliSpan... spans) {
        return new CliLine(List.of(spans));
    }

    /** Starts a line that is put together a run at a time. */
    public static Builder build() {
        return new Builder();
    }

    /** What the line says, colours aside. */
    public String text() {
        if (this.spans.size() == 1) {
            return this.spans.getFirst().text();
        }
        final StringBuilder out = new StringBuilder();
        for (final CliSpan span : this.spans) {
            out.append(span.text());
        }
        return out.toString();
    }

    /**
     * The colour the line opens in, which for a line of one run is simply its colour.
     *
     * <p>Kept for whatever only ever asks one thing of a line: whether it is an error, a warning, a heading.
     */
    public CliStyle style() {
        return this.spans.isEmpty() ? CliStyle.PLAIN : this.spans.getFirst().style();
    }

    /** Puts a line together a run at a time, which reads better than a list of constructors. */
    public static final class Builder {

        private final List<CliSpan> spans = new ArrayList<>();

        private Builder() {
        }

        public Builder plain(final String text) {
            return this.add(text, CliStyle.PLAIN);
        }

        public Builder add(final String text, final CliStyle style) {
            if (text != null && !text.isEmpty()) {
                this.spans.add(new CliSpan(text, style));
            }
            return this;
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
