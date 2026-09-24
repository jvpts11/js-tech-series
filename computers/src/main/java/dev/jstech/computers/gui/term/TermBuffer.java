/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliRun;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.text.ITextLanguage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * What a terminal has on its glass: the lines it was sent, cut into rows of the columns it has.
 *
 * <p>A terminal is a grid. Every character takes one cell, so how a line wraps is a matter of counting and
 * needs no font to work out, which is what lets this be held to account without a game running. Both
 * terminals keep one of these: the prompt that is the whole glass of a machine with no desktop, and the window
 * on one that has.
 *
 * <p>The lines are kept as they arrived and the rows are made from them, because two things need the line
 * rather than its rows: a line redrawn in place replaces every row the old one took, however many that was,
 * and a window dragged wider wraps everything again at the new width. The rows are in the language the buffer
 * was made for, the reader's, since that is what decides how long each line is.
 */
public final class TermBuffer {

    private final Deque<Kept> lines = new ArrayDeque<>();
    private final List<TermRow> rows = new ArrayList<>();
    private final int most;
    private final ITextLanguage language;
    private int columns;
    private int generation;

    /**
     * How many columns the glass of a monitor has.
     *
     * <p>Fixed rather than worked out from the window, because the tools that run at a terminal measure it
     * and size their bars and tables to it, the way real ones do, and the machine has to tell them the same
     * number the screen is going to draw, wherever the player happens to be standing.
     *
     * <p>Eighty, which is what a terminal has had since there were terminals and what the tools that run at
     * one lay their output out for. It is also what fits: a monitor's glass at the size its text is drawn.
     */
    public static final int MONITOR_COLUMNS = 80;

    /** How far apart tab stops are, which is what the tools that indent with a tab are counting on. */
    private static final int TAB = 8;

    /**
     * @param most     how many lines are kept before the oldest scrolls away for good
     * @param columns  how many cells wide the glass is
     * @param language the language the lines are read in
     */
    public TermBuffer(final int most, final int columns, final ITextLanguage language) {
        this.most = Math.max(1, most);
        this.columns = Math.max(8, columns);
        this.language = language;
    }

    /** Adds a line below everything else. */
    public void push(final CliLine line) {
        final List<TermRow> made = wrap(line.resolve(this.language), this.columns);
        this.lines.addLast(new Kept(line, made.size()));
        this.rows.addAll(made);
        while (this.lines.size() > this.most) {
            final Kept gone = this.lines.removeFirst();
            this.rows.subList(0, gone.rows()).clear();
        }
        this.generation++;
    }

    /**
     * Draws a line over the last one, which is how a bar grows where it stands.
     *
     * <p>With nothing there to draw over it is simply the first line.
     */
    public void replaceLast(final CliLine line) {
        final Kept last = this.lines.pollLast();
        if (last != null) {
            this.rows.subList(this.rows.size() - last.rows(), this.rows.size()).clear();
        }
        this.push(line);
    }

    public void clear() {
        this.lines.clear();
        this.rows.clear();
        this.generation++;
    }

    /** Wraps everything again at a new width; asking for the width it already has costs nothing. */
    public void setColumns(final int columns) {
        final int wanted = Math.max(8, columns);
        if (wanted == this.columns) {
            return;
        }
        this.columns = wanted;
        this.rows.clear();
        final List<Kept> again = new ArrayList<>(this.lines);
        this.lines.clear();
        for (final Kept kept : again) {
            final List<TermRow> made = wrap(kept.line().resolve(this.language), wanted);
            this.lines.addLast(new Kept(kept.line(), made.size()));
            this.rows.addAll(made);
        }
        this.generation++;
    }

    public int columns() {
        return this.columns;
    }

    /** The rows on the glass, oldest first. */
    public List<TermRow> rows() {
        return this.rows;
    }

    /** The lines as they arrived, oldest first, which is what a terminal that is reopened is given back. */
    public List<CliLine> lines() {
        final List<CliLine> out = new ArrayList<>(this.lines.size());
        for (final Kept kept : this.lines) {
            out.add(kept.line());
        }
        return out;
    }

    /** Changes whenever what is on the glass does, so a view can tell whether it has to look again. */
    public int generation() {
        return this.generation;
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }

    /**
     * Cuts one line into rows of that many cells.
     *
     * <p>Broken at the last space when there is one reasonably far in, the way a terminal's own wrapping
     * reads best, and cut where the row ends when there is not: a path or a long flag is one word and has to
     * be cut somewhere. A line whose words carry a line break of their own, a sentence a translator wrote over
     * two lines, starts a row at each break.
     */
    static List<TermRow> wrap(final List<CliRun> line, final int columns) {
        final List<TermRow> out = new ArrayList<>(1);
        for (final List<CliRun> part : brokenAtNewlines(line)) {
            wrapInto(out, cellsOf(part), columns);
        }
        return out;
    }

    /** The runs of a line split where their words break onto a new line, each part keeping its colours. */
    static List<List<CliRun>> brokenAtNewlines(final List<CliRun> line) {
        final List<List<CliRun>> parts = new ArrayList<>(1);
        List<CliRun> part = new ArrayList<>();
        for (final CliRun run : line) {
            final String[] pieces = run.text().split("\r?\n", -1);
            for (int i = 0; i < pieces.length; i++) {
                if (i > 0) {
                    parts.add(part);
                    part = new ArrayList<>();
                }
                if (!pieces[i].isEmpty()) {
                    part.add(new CliRun(pieces[i], run.style()));
                }
            }
        }
        parts.add(part);
        return parts;
    }

    /**
     * One line with no breaks in it, cut into rows of that many cells and added to {@code out}.
     *
     * <p>A line set in from the edge, a paragraph of a manual page, keeps its place: the rows it wraps onto are set
     * in as far as it is, so the paragraph reads as one block rather than running back under its own heading. A
     * line set in by more than half the glass has no room to do that and wraps from the edge.
     */
    private static void wrapInto(final List<TermRow> out, final List<Cell> cells, final int columns) {
        int indent = 0;
        while (indent < cells.size() && cells.get(indent).ch() == ' ') {
            indent++;
        }
        if (indent == cells.size() || indent > columns / 2) {
            indent = 0;
        }
        int from = 0;
        int room = columns;
        while (cells.size() - from > room) {
            int cut = from + room;
            boolean atSpace = false;
            for (int i = cut; i > from + room / 2; i--) {
                if (cells.get(i - 1).ch() == ' ') {
                    cut = i;
                    atSpace = true;
                    break;
                }
            }
            out.add(setIn(rowOf(cells, from, cut), room == columns ? 0 : indent));
            from = cut;
            /* A row broken at a space does not open the next one with more of them. */
            while (atSpace && from < cells.size() && cells.get(from).ch() == ' ') {
                from++;
            }
            room = columns - indent;
        }
        out.add(setIn(rowOf(cells, from, cells.size()), room == columns ? 0 : indent));
    }

    /** The row set in by that many blank cells. */
    private static TermRow setIn(final TermRow row, final int cells) {
        if (cells == 0) {
            return row;
        }
        final List<CliRun> runs = new ArrayList<>(row.runs().size() + 1);
        runs.add(CliRun.plain(" ".repeat(cells)));
        runs.addAll(row.runs());
        return new TermRow(runs);
    }

    /** The line, already in a language, a cell at a time, its tabs opened out to the next stop. */
    static List<Cell> cellsOf(final List<CliRun> line) {
        final List<Cell> cells = new ArrayList<>();
        for (final CliRun run : line) {
            final String text = run.text();
            for (int i = 0; i < text.length(); i++) {
                final char ch = text.charAt(i);
                if (ch == '\t') {
                    do {
                        cells.add(new Cell(' ', run.style()));
                    } while (cells.size() % TAB != 0);
                } else if (ch != '\r' && ch != '\n') {
                    cells.add(new Cell(ch, run.style()));
                }
            }
        }
        return cells;
    }

    /** The cells from one to another as a row, runs of one colour joined back together. */
    static TermRow rowOf(final List<Cell> cells, final int from, final int to) {
        final List<CliRun> runs = new ArrayList<>();
        final StringBuilder run = new StringBuilder();
        CliStyle of = null;
        for (int i = from; i < to; i++) {
            final Cell cell = cells.get(i);
            if (of != null && cell.style() != of) {
                runs.add(new CliRun(run.toString(), of));
                run.setLength(0);
            }
            of = cell.style();
            run.append(cell.ch());
        }
        if (of != null) {
            runs.add(new CliRun(run.toString(), of));
        }
        return new TermRow(runs);
    }

    /** A line as it arrived, and how many rows it came to at the width the glass has now. */
    private record Kept(CliLine line, int rows) {
    }

    /** One character and the colour of the run it came out of. */
    record Cell(char ch, CliStyle style) {
    }
}
