/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A table laid out for a reader that writes in lines of text, at a terminal or in a help window, whatever its letters
 * measure: its columns side by side when they fit, or else each row set out under the one before.
 *
 * <p>Side by side, every column is at least as wide as its longest word, so no word is ever cut, and as wide as its
 * longest cell when there is room for that; when there is not, the room over its longest word is shared out by how much
 * more each column holds, and each cell wraps between words inside its column. When the longest words are wider
 * together than the room, or a column would be left too narrow to read, each row is set out under the one before: its
 * first cell, then each further cell set in under it, after its column's head.
 *
 * <p>Pure: the words and a way to measure them in, the lines out.
 */
public final class TextTable {

    private final List<Line> lines;
    private final boolean stacked;

    private TextTable(final List<Line> lines, final boolean stacked) {
        this.lines = List.copyOf(lines);
        this.stacked = stacked;
    }

    /**
     * The table shaped to the room.
     *
     * @param heads   the heads of the columns, or none for a table of properties
     * @param rows    the rows, a cell for every column
     * @param room    how wide the table may be, measured as {@code measure} measures
     * @param gap     the room between two columns
     * @param least   the narrowest a column may be left when its words wrap, before the table is set out row by row
     * @param measure how wide a piece of text is
     */
    public static TextTable shape(final List<String> heads, final List<List<String>> rows, final int room,
                                  final int gap, final int least, final ToIntFunction<String> measure) {
        int columns = heads.size();
        for (final List<String> row : rows) {
            columns = Math.max(columns, row.size());
        }
        final int[] narrowest = new int[columns];
        final int[] widest = new int[columns];
        for (int column = 0; column < columns; column++) {
            for (final String cell : column(heads, rows, column)) {
                widest[column] = Math.max(widest[column], measure.applyAsInt(cell));
                for (final String word : cell.split(" ")) {
                    narrowest[column] = Math.max(narrowest[column], measure.applyAsInt(word));
                }
            }
        }
        final int usable = room - gap * Math.max(0, columns - 1);
        final int[] widths = widths(narrowest, widest, usable);
        boolean fits = widths != null;
        for (int column = 0; fits && column < columns; column++) {
            fits = widths[column] >= widest[column] || widths[column] >= least;
        }
        return fits ? sideBySide(heads, rows, widths, gap, measure) : stacked(heads, rows, room, gap, measure);
    }

    /** Words broken into lines no wider than that, between words; a word wider than a line cut where it ends. */
    public static List<String> wrap(final String words, final int width, final ToIntFunction<String> measure) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : words.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String rest = word;
            while (measure.applyAsInt(rest) > width && rest.length() > 1) {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                int cut = rest.length() - 1;
                while (cut > 1 && measure.applyAsInt(rest.substring(0, cut)) > width) {
                    cut--;
                }
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            if (line.isEmpty()) {
                line.append(rest);
            } else if (measure.applyAsInt(line + " " + rest) <= width) {
                line.append(' ').append(rest);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(rest);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /** Its lines, top to bottom. */
    public List<Line> lines() {
        return this.lines;
    }

    /** Whether its rows are set out one under another rather than in columns. */
    public boolean stacked() {
        return this.stacked;
    }

    /** Every cell of a column, its head first when it has one. */
    private static List<String> column(final List<String> heads, final List<List<String>> rows, final int column) {
        final List<String> cells = new ArrayList<>();
        if (column < heads.size()) {
            cells.add(heads.get(column));
        }
        for (final List<String> row : rows) {
            if (column < row.size()) {
                cells.add(row.get(column));
            }
        }
        return cells;
    }

    /**
     * How wide each column is: its widest cell when all fit, else its longest word and a share of what is left by
     * how much more it holds; none when even the longest words do not fit.
     */
    private static int[] widths(final int[] narrowest, final int[] widest, final int usable) {
        int least = 0;
        int most = 0;
        for (int column = 0; column < narrowest.length; column++) {
            least += narrowest[column];
            most += widest[column];
        }
        if (least > usable) {
            return null;
        }
        if (most <= usable) {
            return widest.clone();
        }
        final int[] widths = narrowest.clone();
        final int spare = usable - least;
        final int wanted = most - least;
        int given = 0;
        for (int column = 0; column < widths.length; column++) {
            final int share = (int) ((long) spare * (widest[column] - narrowest[column]) / wanted);
            widths[column] += share;
            given += share;
        }
        // What the shares left over goes to the column that wants the most, so the table fills its room.
        int hungriest = 0;
        for (int column = 1; column < widths.length; column++) {
            if (widest[column] - widths[column] > widest[hungriest] - widths[hungriest]) {
                hungriest = column;
            }
        }
        widths[hungriest] += spare - given;
        return widths;
    }

    private static TextTable sideBySide(final List<String> heads, final List<List<String>> rows, final int[] widths,
                                        final int gap, final ToIntFunction<String> measure) {
        final int[] starts = new int[widths.length];
        for (int column = 1; column < widths.length; column++) {
            starts[column] = starts[column - 1] + widths[column - 1] + gap;
        }
        final List<Line> lines = new ArrayList<>();
        if (!heads.isEmpty()) {
            lines.addAll(lines(heads, widths, starts, Role.HEAD, measure));
        }
        for (final List<String> row : rows) {
            lines.addAll(lines(row, widths, starts, Role.CELL, measure));
        }
        return new TextTable(lines, false);
    }

    /** A row's cells wrapped in their columns, a line for each line of the tallest. */
    private static List<Line> lines(final List<String> cells, final int[] widths, final int[] starts,
                                    final Role role, final ToIntFunction<String> measure) {
        final List<List<String>> wrapped = new ArrayList<>();
        int tallest = 1;
        for (int column = 0; column < cells.size(); column++) {
            final List<String> cell = wrap(cells.get(column), widths[column], measure);
            wrapped.add(cell);
            tallest = Math.max(tallest, cell.size());
        }
        final List<Line> out = new ArrayList<>();
        for (int at = 0; at < tallest; at++) {
            final List<Cell> line = new ArrayList<>();
            for (int column = 0; column < wrapped.size(); column++) {
                if (at < wrapped.get(column).size()) {
                    line.add(new Cell(starts[column], wrapped.get(column).get(at),
                            role == Role.CELL && column == 0 ? Role.NAME : role));
                }
            }
            out.add(new Line(line, at == 0));
        }
        return out;
    }

    private static TextTable stacked(final List<String> heads, final List<List<String>> rows, final int room,
                                     final int gap, final ToIntFunction<String> measure) {
        final List<Line> lines = new ArrayList<>();
        // The cells under a row's first are set in by two gaps, so each row reads as one block.
        final int setIn = 2 * gap;
        for (final List<String> row : rows) {
            boolean opens = true;
            for (final String part : wrap(row.getFirst(), room, measure)) {
                lines.add(new Line(List.of(new Cell(0, part, Role.NAME)), opens));
                opens = false;
            }
            for (int column = 1; column < row.size(); column++) {
                final String said = column < heads.size() ? heads.get(column) + ": " + row.get(column)
                        : row.get(column);
                for (final String part : wrap(said, room - setIn, measure)) {
                    lines.add(new Line(List.of(new Cell(setIn, part, Role.CELL)), false));
                }
            }
        }
        return new TextTable(lines, true);
    }

    /** What a cell of a line is: a column's head, the first cell of a row (what it names), or another cell. */
    public enum Role {
        HEAD, NAME, CELL
    }

    /**
     * A piece of a line: where it starts across, as {@code measure} measures, and its words.
     *
     * @param x    where it starts, from the table's left edge
     * @param text its words
     * @param role what it is
     */
    public record Cell(int x, String text, Role role) {
    }

    /**
     * A line of the table: its cells, left to right.
     *
     * @param opens whether it is the first line of the heads or of a row, which a reader may rule a line over
     */
    public record Line(List<Cell> cells, boolean opens) {

        public Line {
            cells = List.copyOf(cells);
        }

        /** The line as text in letters of one width, each cell set at its place with spaces before it. */
        public String padded() {
            final StringBuilder out = new StringBuilder();
            for (final Cell cell : this.cells) {
                out.append(" ".repeat(Math.max(out.isEmpty() ? 0 : 1, cell.x() - out.length())));
                out.append(cell.text());
            }
            return out.toString();
        }
    }
}
