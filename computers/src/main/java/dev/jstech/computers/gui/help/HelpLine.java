/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import java.util.ArrayList;
import java.util.List;

/**
 * A line of a help page shown at a terminal: pieces of text side by side, each in the ink of what it is, some of them
 * links to another page.
 *
 * <p>The page is worked out once for the width of the glass, and the viewer that shows it decides what each ink looks
 * like: yellow headings on blue for the DOS family's help, plain grey on black for info.
 */
public record HelpLine(List<Span> spans) {

    /** A line with nothing on it. */
    public static final HelpLine BLANK = new HelpLine(List.of());

    public HelpLine {
        spans = List.copyOf(spans);
    }

    /** A line of one piece, starting that many columns in. */
    public static HelpLine of(final int indent, final String text, final Ink ink) {
        return new HelpLine(indent > 0 ? List.of(new Span(" ".repeat(indent), Ink.BODY, ""), new Span(text, ink, ""))
                : List.of(new Span(text, ink, "")));
    }

    /** Its text, every piece run together, as the glass shows it. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        for (final Span span : this.spans) {
            out.append(span.text());
        }
        return out.toString();
    }

    /** Where each link on the line is, as the column it starts at with the piece that is the link. */
    public List<Placed> links() {
        final List<Placed> out = new ArrayList<>();
        int column = 0;
        for (final Span span : this.spans) {
            if (!span.link().isEmpty()) {
                out.add(new Placed(column, span));
            }
            column += span.text().length();
        }
        return out;
    }

    /** What a piece of a line is, which says the ink it is drawn in. */
    public enum Ink {
        /** The page's title. */
        TITLE,
        /** The rule under the title. */
        RULE,
        /** A heading such as "What it is". */
        HEADING,
        /** Running text. */
        BODY,
        /** A note set apart. */
        NOTE,
        /** A warning, or what can go wrong. */
        WARNING,
        /** A table's caption and its rows. */
        TABLE,
        /** A link to another page. */
        LINK,
        /** What is said quietly: a caption, an empty list. */
        DIM
    }

    /** A piece of a line: its text, its ink, and where it leads when it is a link, or empty. */
    public record Span(String text, Ink ink, String link) {
    }

    /** A link where it stands on its line. */
    public record Placed(int column, Span span) {
    }
}
