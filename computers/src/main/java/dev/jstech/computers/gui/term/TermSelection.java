/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import java.util.List;

/**
 * What is picked out on a terminal's glass, from the cell a drag started in to the cell it is in now.
 *
 * <p>Held as the two cells rather than as a tidy range, because which one came first is what decides which end
 * moves: a drag back over the start of a selection turns it round, and so does Shift with an arrow. Everything
 * that reads it asks for it the right way round instead.
 *
 * <p>Cells, not pixels, and rows of the buffer, not rows of the window: the glass is a grid, so what is picked
 * out is worked out by counting and can be held to account without a game running. The screens turn a pointer
 * into a cell and hand it here.
 *
 * @param fromRow    the row the selection was started in, counting from the top of the buffer
 * @param fromColumn how many cells across that row it was started
 * @param toRow      the row it reaches now
 * @param toColumn   how many cells across that row it reaches, the cell after the last one taken
 */
public record TermSelection(int fromRow, int fromColumn, int toRow, int toColumn) {

    /** Nothing picked out, which is what a terminal has until somebody drags across it. */
    public static final TermSelection NONE = new TermSelection(0, 0, 0, 0);

    /** A selection of the one cell, which is what a press with no drag yet comes to. */
    public static TermSelection at(final int row, final int column) {
        return new TermSelection(row, column, row, column);
    }

    /** The same selection with its far end moved, which is what a drag and Shift with an arrow both do. */
    public TermSelection reachingTo(final int row, final int column) {
        return new TermSelection(this.fromRow, this.fromColumn, row, column);
    }

    /** Whether it takes in nothing, in which case there is nothing to draw and nothing to copy. */
    public boolean isEmpty() {
        return this.fromRow == this.toRow && this.fromColumn == this.toColumn;
    }

    /** The row the selection starts on once it is the right way round. */
    public int startRow() {
        return backwards() ? this.toRow : this.fromRow;
    }

    /** The cell it starts at on that row. */
    public int startColumn() {
        return backwards() ? this.toColumn : this.fromColumn;
    }

    /** The row it ends on. */
    public int endRow() {
        return backwards() ? this.fromRow : this.toRow;
    }

    /** The cell after the last one it takes on that row. */
    public int endColumn() {
        return backwards() ? this.fromColumn : this.toColumn;
    }

    /** Whether that cell is taken, which is what the glass asks of every cell it draws. */
    public boolean covers(final int row, final int column) {
        if (isEmpty() || row < startRow() || row > endRow()) {
            return false;
        }
        final boolean afterTheStart = row > startRow() || column >= startColumn();
        final boolean beforeTheEnd = row < endRow() || column < endColumn();
        return afterTheStart && beforeTheEnd;
    }

    /**
     * What is picked out, as text, taking the rows from the buffer.
     *
     * <p>A row is cut at what it says and not at the width of the glass, so the spaces a table pads its last
     * column with do not come along, which is what makes a copied listing paste as a listing. Rows are joined
     * with a newline, the way a terminal has always handed several rows over.
     */
    public String textOf(final List<TermRow> rows) {
        if (isEmpty()) {
            return "";
        }
        final StringBuilder out = new StringBuilder();
        for (int row = Math.max(0, startRow()); row <= Math.min(rows.size() - 1, endRow()); row++) {
            final String text = rows.get(row).text();
            final int from = row == startRow() ? Math.min(startColumn(), text.length()) : 0;
            final int to = row == endRow() ? Math.min(endColumn(), text.length()) : text.length();
            if (!out.isEmpty()) {
                out.append('\n');
            }
            if (from < to) {
                out.append(trailing(text.substring(from, to)));
            }
        }
        return out.toString();
    }

    /**
     * The word around that cell, which is what a second click in the same place takes.
     *
     * <p>A word here is everything up to the spaces on either side of it, which is what a person means by one
     * at a terminal: {@code minecraft:oak_log} and {@code C:\progs\hello.sgs} each come out whole rather than
     * cut at their punctuation. A click on a run of spaces takes that run.
     */
    public static TermSelection wordAt(final List<TermRow> rows, final int row, final int column) {
        if (row < 0 || row >= rows.size()) {
            return NONE;
        }
        final String text = rows.get(row).text();
        if (column < 0 || column >= text.length()) {
            return NONE;
        }
        final boolean spaces = text.charAt(column) == ' ';
        int from = column;
        while (from > 0 && partOfAWord(text.charAt(from - 1)) != spaces) {
            from--;
        }
        int to = column;
        while (to < text.length() && partOfAWord(text.charAt(to)) != spaces) {
            to++;
        }
        return new TermSelection(row, from, row, to);
    }

    /** The whole row, which is what a third click takes. */
    public static TermSelection lineAt(final List<TermRow> rows, final int row) {
        if (row < 0 || row >= rows.size()) {
            return NONE;
        }
        return new TermSelection(row, 0, row, rows.get(row).text().length());
    }

    /** Whether the far end of the selection is before the near one, the way a drag upwards leaves it. */
    private boolean backwards() {
        return this.toRow < this.fromRow || (this.toRow == this.fromRow && this.toColumn < this.fromColumn);
    }

    private static boolean partOfAWord(final char ch) {
        return ch != ' ';
    }

    /** A row without the spaces it was padded out with, so a copied table pastes as one. */
    private static String trailing(final String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        return text.substring(0, end);
    }
}
