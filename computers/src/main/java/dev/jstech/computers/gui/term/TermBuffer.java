/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
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
 * and a window dragged wider wraps everything again at the new width.
 */
public final class TermBuffer {

    private final Deque<Kept> lines = new ArrayDeque<>();
    private final List<TermRow> rows = new ArrayList<>();
    private final int most;
    private int columns;
    private int generation;

    /**
     * How many columns the glass of a monitor has.
     *
     * <p>Fixed rather than worked out from the window, because the tools that run at a terminal measure it
     * and size their bars and tables to it, the way real ones do, and the machine has to tell them the same
     * number the screen is going to draw, wherever the player happens to be standing.
     */
    public static final int MONITOR_COLUMNS = 64;

    /** How far apart tab stops are, which is what the tools that indent with a tab are counting on. */
    private static final int TAB = 8;

    /**
     * @param most    how many lines are kept before the oldest scrolls away for good
     * @param columns how many cells wide the glass is
     */
    public TermBuffer(final int most, final int columns) {
        this.most = Math.max(1, most);
        this.columns = Math.max(8, columns);
    }

    /** Adds a line below everything else. */
    public void push(final CliLine line) {
        final List<TermRow> made = wrap(line, this.columns);
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
            final List<TermRow> made = wrap(kept.line(), wanted);
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
     * be cut somewhere.
     */
    static List<TermRow> wrap(final CliLine line, final int columns) {
        final List<Cell> cells = cellsOf(line);
        final List<TermRow> out = new ArrayList<>(1);
        int from = 0;
        while (cells.size() - from > columns) {
            int cut = from + columns;
            boolean atSpace = false;
            for (int i = cut; i > from + columns / 2; i--) {
                if (cells.get(i - 1).ch() == ' ') {
                    cut = i;
                    atSpace = true;
                    break;
                }
            }
            out.add(rowOf(cells, from, cut));
            from = cut;
            /* A row broken at a space does not open the next one with more of them. */
            while (atSpace && from < cells.size() && cells.get(from).ch() == ' ') {
                from++;
            }
        }
        out.add(rowOf(cells, from, cells.size()));
        return out;
    }

    /** The line a cell at a time, its tabs opened out to the next stop. */
    private static List<Cell> cellsOf(final CliLine line) {
        final List<Cell> cells = new ArrayList<>();
        for (final CliSpan span : line.spans()) {
            final String text = span.text();
            for (int i = 0; i < text.length(); i++) {
                final char ch = text.charAt(i);
                if (ch == '\t') {
                    do {
                        cells.add(new Cell(' ', span));
                    } while (cells.size() % TAB != 0);
                } else if (ch != '\r' && ch != '\n') {
                    cells.add(new Cell(ch, span));
                }
            }
        }
        return cells;
    }

    /** The cells from one to another as a row, runs of one colour joined back together. */
    private static TermRow rowOf(final List<Cell> cells, final int from, final int to) {
        final List<CliSpan> runs = new ArrayList<>();
        final StringBuilder run = new StringBuilder();
        CliSpan of = null;
        for (int i = from; i < to; i++) {
            final Cell cell = cells.get(i);
            if (of != null && cell.of().style() != of.style()) {
                runs.add(new CliSpan(run.toString(), of.style()));
                run.setLength(0);
            }
            of = cell.of();
            run.append(cell.ch());
        }
        if (of != null) {
            runs.add(new CliSpan(run.toString(), of.style()));
        }
        return new TermRow(runs);
    }

    /** A line as it arrived, and how many rows it came to at the width the glass has now. */
    private record Kept(CliLine line, int rows) {
    }

    /** One character and the run it came out of, which is where its colour is. */
    private record Cell(char ch, CliSpan of) {
    }
}
