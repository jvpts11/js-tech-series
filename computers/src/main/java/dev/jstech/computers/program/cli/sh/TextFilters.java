/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * What the tools that work on lines actually do.
 *
 * <p>One answer per question, written once: which lines hold a word, how many there are, the first or last few,
 * and them in order. Each family of systems has its own name and its own flags for each of these, and they all
 * come here, so {@code grep} and {@code FIND} really are the same tool wearing two coats rather than two tools
 * that might one day disagree.
 *
 * <p>Pure: lines in, lines out. What the lines came from, and where they go, is the shell's business.
 */
public final class TextFilters {

    private TextFilters() {
    }

    /** The lines that hold that text; with {@code invert}, the ones that do not. */
    public static List<String> holding(final List<String> lines, final String text, final boolean ignoreCase,
                                       final boolean invert) {
        final String wanted = ignoreCase ? text.toLowerCase(Locale.ROOT) : text;
        final List<String> out = new ArrayList<>();
        for (final String line : lines) {
            final boolean has = (ignoreCase ? line.toLowerCase(Locale.ROOT) : line).contains(wanted);
            if (has != invert) {
                out.add(line);
            }
        }
        return out;
    }

    /** The first {@code many} lines, or all of them when there are fewer. */
    public static List<String> first(final List<String> lines, final int many) {
        return lines.size() <= Math.max(0, many) ? List.copyOf(lines) : List.copyOf(lines.subList(0, many));
    }

    /** The last {@code many} lines. */
    public static List<String> last(final List<String> lines, final int many) {
        final int from = Math.max(0, lines.size() - Math.max(0, many));
        return List.copyOf(lines.subList(from, lines.size()));
    }

    /**
     * The lines in order, without regard to case, which is the order a person reads a list in.
     *
     * <p>With {@code unique}, a line that is the same as the one before it is left out, which is what makes
     * a sorted listing worth counting.
     */
    public static List<String> ordered(final List<String> lines, final boolean reverse, final boolean unique) {
        final List<String> out = new ArrayList<>(lines);
        out.sort(String::compareToIgnoreCase);
        if (reverse) {
            Collections.reverse(out);
        }
        if (!unique) {
            return out;
        }
        final List<String> once = new ArrayList<>(out.size());
        for (final String line : out) {
            if (once.isEmpty() || !once.get(once.size() - 1).equals(line)) {
                once.add(line);
            }
        }
        return once;
    }

    /** How many lines, words and characters there are, in that order. */
    public static Counts counted(final List<String> lines) {
        long words = 0L;
        long letters = 0L;
        for (final String line : lines) {
            letters += line.length() + 1;
            for (final String word : line.split("\\s+")) {
                if (!word.isEmpty()) {
                    words++;
                }
            }
        }
        return new Counts(lines.size(), words, letters);
    }

    /** What {@code wc} counted. */
    public record Counts(long lines, long words, long letters) {
    }
}
