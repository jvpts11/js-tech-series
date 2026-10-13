/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.os.edit.CodeRuns;
import java.util.ArrayList;
import java.util.List;

/**
 * How an editor colours the network's language: its words, quoted text, numbers, comments and the signs between
 * them, the way a query editor has always coloured a query. Pure logic, no Minecraft.
 */
public final class IqlColouring {

    private IqlColouring() {
    }

    /** The pieces of {@code lines}, each with what it is, lines and columns counted from one. */
    public static List<CodeRuns.Span> spans(final List<String> lines) {
        final List<CodeRuns.Span> spans = new ArrayList<>();
        for (int row = 0; row < lines.size(); row++) {
            line(spans, row + 1, lines.get(row));
        }
        return spans;
    }

    private static void line(final List<CodeRuns.Span> out, final int row, final String line) {
        final int n = line.length();
        int i = 0;
        while (i < n) {
            final char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '-' && i + 1 < n && line.charAt(i + 1) == '-') {
                out.add(new CodeRuns.Span(row, i + 1, n - i, CodeRuns.Ink.COMMENT));
                return;
            }
            final int start = i;
            if (c == '"' || c == '\'') {
                i++;
                while (i < n && line.charAt(i) != c) {
                    i++;
                }
                i = Math.min(n, i + 1);
                out.add(new CodeRuns.Span(row, start + 1, i - start, CodeRuns.Ink.TEXT));
                continue;
            }
            if (wordChar(c)) {
                while (i < n && wordChar(line.charAt(i))) {
                    i++;
                }
                final String word = line.substring(start, i);
                final CodeRuns.Ink ink = IqlScript.keyword(word) ? CodeRuns.Ink.KEYWORD
                        : word.matches("-?\\d+%?") ? CodeRuns.Ink.NUMBER : CodeRuns.Ink.NAME;
                out.add(new CodeRuns.Span(row, start + 1, i - start, ink));
                continue;
            }
            out.add(new CodeRuns.Span(row, start + 1, 1, CodeRuns.Ink.SYMBOL));
            i++;
        }
    }

    private static boolean wordChar(final char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.' || c == '#' || c == '%';
    }
}
