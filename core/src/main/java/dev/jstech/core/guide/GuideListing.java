/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A listing's lines cut to the width of a page, before they are placed. A line that fits is one row; one too long is
 * broken at the last space, opening bracket, comma or point that fits, never inside a word, and goes on in rows under
 * itself, set in two places past where it started and with no number, so a reader sees it is one line. Only a word
 * wider than the whole row is cut where it has to be. Every character keeps the colour its language gave it.
 */
final class GuideListing {

    /** How far a line that goes on is set in past where it started. */
    static final int GOES_ON = 2;
    /** What a line too long for its row may break after when no space fits. */
    private static final String BREAKS_AFTER = "(,.";

    private GuideListing() {
    }

    /**
     * The rows of a listing's lines, coloured as {@code colours} says, at most {@code columns} characters each.
     *
     * @param lines   the lines as typed
     * @param colours the runs of each line, as {@link GuideCode#runs} gives them
     * @param columns how many characters a row holds
     */
    static List<Row> rows(final List<String> lines, final List<List<GuideCode.Run>> colours, final int columns) {
        final int room = Math.max(1, columns);
        final List<Row> rows = new ArrayList<>();
        for (int number = 1; number <= lines.size(); number++) {
            final String line = lines.get(number - 1);
            final GuideCode.Ink[] inks = inksOf(line, number - 1 < colours.size() ? colours.get(number - 1)
                    : List.of());
            final int indent = line.length() - line.stripLeading().length();
            String rest = line;
            GuideCode.Ink[] restInks = inks;
            boolean first = true;
            while (true) {
                final int cut = rest.length() <= room ? rest.length() : cutAt(rest, room);
                final String part = rest.substring(0, cut).stripTrailing();
                rows.add(new Row(first ? number : 0, segments(part, restInks)));
                first = false;
                final String after = rest.substring(cut);
                if (after.isBlank()) {
                    break;
                }
                // The rest goes on set in past where the line began, its own leading spaces dropped.
                final int dropped = after.length() - after.stripLeading().length();
                final int setIn = Math.min(indent + GOES_ON, room - 1);
                rest = " ".repeat(setIn) + after.stripLeading();
                final GuideCode.Ink[] next = new GuideCode.Ink[rest.length()];
                for (int i = 0; i < next.length; i++) {
                    next[i] = i < setIn ? GuideCode.Ink.PLAIN : restInks[cut + dropped + i - setIn];
                }
                restInks = next;
            }
        }
        return rows;
    }

    /**
     * Where a line too long for the row breaks: at its last space that fits, or after its last opening bracket, comma
     * or point, so a name such as Console.PrintLine goes on whole at its point rather than cut inside a word.
     */
    private static int cutAt(final String line, final int room) {
        final int indent = line.length() - line.stripLeading().length();
        int cut = line.lastIndexOf(' ', room);
        for (int mark = 0; mark < BREAKS_AFTER.length(); mark++) {
            cut = Math.max(cut, line.lastIndexOf(BREAKS_AFTER.charAt(mark), room - 1) + 1);
        }
        // Nowhere to break but inside the first word: the word is cut where it has to be.
        return cut <= indent ? room : cut;
    }

    /** The colour of each character of a line, from the runs its language gave it. */
    private static GuideCode.Ink[] inksOf(final String line, final List<GuideCode.Run> runs) {
        final GuideCode.Ink[] inks = new GuideCode.Ink[line.length()];
        Arrays.fill(inks, GuideCode.Ink.PLAIN);
        for (final GuideCode.Run run : runs) {
            for (int i = Math.max(0, run.start()); i < Math.min(line.length(), run.start() + run.length()); i++) {
                inks[i] = run.ink();
            }
        }
        return inks;
    }

    /** A row's text cut into stretches of one colour each. */
    private static List<Segment> segments(final String text, final GuideCode.Ink[] inks) {
        final List<Segment> segments = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (i == text.length() || inks[i] != inks[start]) {
                segments.add(new Segment(text.substring(start, i), inks[start]));
                start = i;
            }
        }
        return segments;
    }

    /**
     * A row of a listing.
     *
     * @param number   the number of the line it starts, from one, or 0 for a row a line goes on in
     * @param segments its text, in stretches of one colour
     */
    record Row(int number, List<Segment> segments) {

        Row {
            segments = List.copyOf(segments);
        }

        /** Its text whole. */
        String text() {
            final StringBuilder out = new StringBuilder();
            for (final Segment segment : this.segments) {
                out.append(segment.text());
            }
            return out.toString();
        }
    }

    /** A stretch of a row in one colour. */
    record Segment(String text, GuideCode.Ink ink) {
    }
}
