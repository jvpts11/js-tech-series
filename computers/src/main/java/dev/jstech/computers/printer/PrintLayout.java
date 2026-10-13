/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import java.util.ArrayList;
import java.util.List;

/**
 * How text goes onto pages: a page holds so many lines of so many characters, a line too long for it wraps at its last
 * space (or is cut where it has none), and the first page opens with the document's title and a blank line, as the
 * printouts of a text editor do. A table goes down as columns padded to their widest cell.
 *
 * <p>Pure: the dialogs count the sheets a document takes with it, and the printer lays the pages out with it.
 */
public final class PrintLayout {

    /** Characters on a line of a page standing upright. */
    public static final int PORTRAIT_COLUMNS = 48;
    /** Lines on a page standing upright. */
    public static final int PORTRAIT_LINES = 30;
    /** Characters on a line of a page lying on its side. */
    public static final int LANDSCAPE_COLUMNS = 66;
    /** Lines on a page lying on its side. */
    public static final int LANDSCAPE_LINES = 20;
    /** The widest a table's column grows before its cells are cut. */
    public static final int MAX_COLUMN = 24;
    /** The spaces between two columns of a table. */
    private static final String GUTTER = "  ";
    /** How many spaces a tab stands for. */
    private static final int TAB = 4;

    private PrintLayout() {
    }

    /** The pages {@code text} fills under {@code title}, upright or on its side, at most a document's worth. */
    public static List<String> pages(final String title, final String text, final boolean landscape) {
        final int columns = landscape ? LANDSCAPE_COLUMNS : PORTRAIT_COLUMNS;
        final int lines = landscape ? LANDSCAPE_LINES : PORTRAIT_LINES;
        final List<String> all = new ArrayList<>();
        if (!title.isBlank()) {
            all.addAll(wrap(title, columns));
            all.add("");
        }
        // Nothing past the last page a document may hold is ever printed, so laying it out would only cost time.
        final int mostLines = PrintedDocument.MAX_PAGES * lines;
        boolean cut = false;
        for (final String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            if (all.size() >= mostLines) {
                cut = true;
                break;
            }
            all.addAll(wrap(line.replace("\t", " ".repeat(TAB)), columns));
        }
        // A cut text has more behind it, so what ends the kept part is not the end of the document.
        while (!cut && all.size() > 1 && all.getLast().isBlank()) {
            all.removeLast();
        }
        final List<String> pages = new ArrayList<>();
        for (int i = 0; i < all.size() && pages.size() < PrintedDocument.MAX_PAGES; i += lines) {
            pages.add(String.join("\n", all.subList(i, Math.min(all.size(), i + lines))));
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
        return pages;
    }

    /** How many sheets one copy of {@code text} takes. */
    public static int sheets(final String title, final String text, final boolean landscape) {
        return pages(title, text, landscape).size();
    }

    /**
     * A table as lines of text: each column as wide as its widest cell (at most {@link #MAX_COLUMN}), the cells padded
     * to it and two spaces between, the first row underlined when {@code header} is set.
     */
    public static String table(final List<List<String>> rows, final boolean header) {
        final List<Integer> widths = new ArrayList<>();
        for (final List<String> row : rows) {
            for (int c = 0; c < row.size(); c++) {
                final int w = Math.min(MAX_COLUMN, row.get(c).length());
                if (c >= widths.size()) {
                    widths.add(w);
                } else if (w > widths.get(c)) {
                    widths.set(c, w);
                }
            }
        }
        final StringBuilder out = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            final List<String> row = rows.get(r);
            final StringBuilder line = new StringBuilder();
            for (int c = 0; c < row.size(); c++) {
                final String cell = row.get(c).length() > MAX_COLUMN ? row.get(c).substring(0, MAX_COLUMN)
                        : row.get(c);
                line.append(cell);
                if (c < row.size() - 1) {
                    line.append(" ".repeat(widths.get(c) - cell.length())).append(GUTTER);
                }
            }
            out.append(line.toString().stripTrailing()).append('\n');
            if (r == 0 && header) {
                int total = 0;
                for (int c = 0; c < widths.size(); c++) {
                    total += widths.get(c) + (c < widths.size() - 1 ? GUTTER.length() : 0);
                }
                out.append("-".repeat(total)).append('\n');
            }
        }
        return out.toString();
    }

    /** A line as the page's lines: whole when it fits, otherwise broken at the last space before the edge. */
    static List<String> wrap(final String line, final int columns) {
        final List<String> out = new ArrayList<>();
        String rest = line.stripTrailing();
        while (rest.length() > columns) {
            int cut = rest.lastIndexOf(' ', columns);
            if (cut <= 0) {
                cut = columns;
            }
            out.add(rest.substring(0, cut).stripTrailing());
            rest = rest.substring(cut).stripLeading();
        }
        out.add(rest);
        return out;
    }
}
