/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The colours of a listing's code, as stretches of each of its lines: what each stretch is (a keyword, a name, text in
 * quotes, a number, a comment) and so which colour the style gives it.
 *
 * <p>A language says what its source is made of as pieces scattered over the whole listing, counted in lines and
 * columns, and says nothing of the spaces between them. A page is laid a line at a time and every character needs a
 * colour, so clipping a piece to its line, filling the gaps and putting the result in reading order happens once,
 * here, where it is tested with no game around it.
 */
public final class GuideCode {

    private GuideCode() {
    }

    /** Every line plain, as a listing in a language nothing colours reads. */
    public static List<List<Run>> plain(final List<String> lines) {
        final List<List<Run>> runs = new ArrayList<>(lines.size());
        for (final String line : lines) {
            runs.add(line.isEmpty() ? List.of() : List.of(new Run(0, line.length(), Ink.PLAIN)));
        }
        return runs;
    }

    /**
     * The runs of every line, each line covered end to end with no gaps and no overlaps. A piece reaching past the end
     * of its line is cut there, and one on a line that does not exist is dropped. Where two pieces want the same
     * characters the one starting earlier keeps them; on a tie the longer goes first.
     */
    public static List<List<Run>> runs(final List<String> lines, final List<Span> spans) {
        final List<List<Span>> perLine = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            perLine.add(new ArrayList<>());
        }
        for (final Span span : spans) {
            final int index = span.line() - 1;
            if (index >= 0 && index < lines.size() && span.length() > 0 && span.column() >= 1) {
                perLine.get(index).add(span);
            }
        }
        final List<List<Run>> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            out.add(runsOf(lines.get(i).length(), perLine.get(i)));
        }
        return out;
    }

    /** The role of a manual's style a kind of code is coloured in. */
    public static String role(final Ink ink) {
        return switch (ink) {
            case PLAIN, SYMBOL -> GuideStyle.INK;
            case KEYWORD -> GuideStyle.CODE_KEYWORD;
            case NAME -> GuideStyle.CODE_NAME;
            case TEXT -> GuideStyle.CODE_TEXT;
            case NUMBER -> GuideStyle.CODE_NUMBER;
            case COMMENT -> GuideStyle.CODE_COMMENT;
        };
    }

    private static List<Run> runsOf(final int width, final List<Span> spans) {
        final List<Run> runs = new ArrayList<>();
        if (width <= 0) {
            return runs;
        }
        spans.sort(Comparator.comparingInt(Span::column)
                .thenComparing(Comparator.comparingInt(Span::length).reversed()));
        int at = 0;
        for (final Span span : spans) {
            final int start = span.column() - 1;
            final int end = Math.min(width, start + span.length());
            if (start >= width || end <= at) {
                continue;
            }
            if (start > at) {
                runs.add(new Run(at, start - at, Ink.PLAIN));
                at = start;
            }
            runs.add(new Run(at, end - at, span.ink()));
            at = end;
        }
        if (at < width) {
            runs.add(new Run(at, width - at, Ink.PLAIN));
        }
        return runs;
    }

    /** What a stretch of code is, as far as colouring it goes. */
    public enum Ink {
        /** Anything the language did not claim: spaces, and whatever it could not read. */
        PLAIN,
        KEYWORD,
        NAME,
        TEXT,
        NUMBER,
        COMMENT,
        SYMBOL
    }

    /** A piece as a language reports it: line and column from one, length in characters. */
    public record Span(int line, int column, int length, Ink ink) {
    }

    /** A stretch of one line, ready to colour: its start from zero, its length in characters. */
    public record Run(int start, int length, Ink ink) {
    }
}
