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
import java.util.function.Function;

/**
 * A table shaped to the width of a page's column, before it is placed: its columns side by side under a shaded head,
 * or, when they cannot stand side by side, each row laid out under the one before it.
 *
 * <p>Side by side, every column is at least as wide as its longest word, so no word is ever broken; the room left
 * over is shared out by how much each column holds, and a cell wraps between words only. Words are written small and a
 * column of nothing but data in the code font, a line of either eight pixels, the code standing two pixels higher so
 * both sit on one baseline. When the longest words of the columns are wider together than the column of the page, or
 * a column of words would be left too narrow to read, the table is stacked instead: each row's first cell, in the
 * heading's colour, with its second beside it, then each further cell under its column's head, set in.
 *
 * <p>A row is a run of lines, so the layout can end a part of a table between two rows, and cut a row taller than a
 * page between two of its lines.
 */
final class GuideTable {

    private final int width;
    private final boolean stacked;
    private final int headHeight;
    private final List<Cell> head;
    private final List<Row> rows;
    /** The words wider than any room the table had for them, which had to be cut: none, in a table that reads well. */
    private final List<String> cut;

    /** The room between a column's edge and its words, on either side. */
    static final int PAD = 2;
    /** How far apart two lines of a table are: a line of small words, or of code. */
    static final int LINE = TextSize.SMALL.lineHeight();
    /**
     * How much higher a line of code stands than a line of small words: the code font has seven rows above its line
     * at its own size, small words five and a quarter, so code drawn two pixels higher shares their baseline.
     */
    static final int CODE_RISE = 2;
    /** The room over the first line of a row, and under the last before the row's rule. */
    static final int ROW_TOP = 2;
    static final int ROW_BOTTOM = 2;
    /** The same under a stacked row, which stands a little more apart from the next. */
    static final int STACK_BOTTOM = 4;
    /** The room between a stacked row's first line and the cells set in under it. */
    static final int STACK_GAP = 1;
    /** The room between a stacked row's heads and the cells beside them. */
    static final int INDENT_GAP = 4;
    /** The narrowest a column of words may be left: about nine letters of small text, two or three words a line. */
    static final int MIN_PROSE = 40;

    private GuideTable(final int width, final boolean stacked, final int headHeight, final List<Cell> head,
                       final List<Row> rows, final List<String> cut) {
        this.width = width;
        this.stacked = stacked;
        this.headHeight = headHeight;
        this.head = List.copyOf(head);
        this.rows = List.copyOf(rows);
        this.cut = List.copyOf(cut);
    }

    /** The table shaped to {@code width}, its words translated and measured through {@code text}. */
    static GuideTable shape(final GuideBlock.Table table, final int width, final IGuideText text) {
        return shape(table, width, text, target -> null, target -> null);
    }

    /**
     * The table shaped to {@code width}, a link in a cell written as a printed manual's table writes one: its words and
     * the number of the page it leads to, or the book that holds it.
     */
    static GuideTable shape(final GuideBlock.Table table, final int width, final IGuideText text,
                            final Function<String, String> numbers, final Function<String, String> elsewhere) {
        final List<String> heads = new ArrayList<>();
        if (table.headKeys().isEmpty()) {
            heads.add(text.text(GuideTexts.PROPERTY.key()));
            heads.add(text.text(GuideTexts.VALUE.key()));
        } else {
            for (final String key : table.headKeys()) {
                heads.add(text.text(key));
            }
        }
        final List<List<String>> cells = new ArrayList<>();
        for (final GuideBlock.TableRow row : table.rows()) {
            final List<String> written = new ArrayList<>();
            for (final GuideBlock.GuideValue cell : row.cells()) {
                written.add(cell instanceof GuideBlock.GuideValue.Words
                        ? GuideLinks.plain(text.valueText(cell), numbers, elsewhere,
                                target -> text.titleOf(target).isEmpty() ? null : text.titleOf(target))
                        : text.valueText(cell));
            }
            cells.add(written);
        }
        final TextSize[] sizes = new TextSize[heads.size()];
        for (int column = 0; column < sizes.length; column++) {
            final int at = column;
            final boolean data = !table.rows().isEmpty()
                    && table.rows().stream().allMatch(row -> row.cells().get(at).data());
            sizes[column] = data ? TextSize.CODE : TextSize.SMALL;
        }
        final int[] widths = widths(heads, cells, sizes, width, text);
        return widths == null ? stacked(heads, cells, sizes, width, text)
                : sideBySide(heads, cells, sizes, widths, width, text);
    }

    /** Whether its rows are stacked rather than its columns side by side. */
    boolean stacked() {
        return this.stacked;
    }

    /** The words it had to cut, too wide for the room it had for them. */
    List<String> cut() {
        return this.cut;
    }

    /** How tall its head is; a stacked table has none, each row saying its heads. */
    int headHeight() {
        return this.headHeight;
    }

    int rows() {
        return this.rows.size();
    }

    /** How many lines a row runs to. */
    int lines(final int row) {
        return this.rows.get(row).lines().size();
    }

    /** How tall all its rows are together, its head left out. */
    int height() {
        int height = 0;
        for (int row = 0; row < this.rows.size(); row++) {
            height += this.height(row, 0, this.lines(row));
        }
        return height;
    }

    /** How tall the lines {@code from} to {@code to} of a row are, with the room over and under them. */
    int height(final int row, final int from, final int to) {
        final Row laid = this.rows.get(row);
        int height = ROW_TOP + laid.bottom();
        for (int line = from; line < to; line++) {
            height += laid.lines().get(line).pitch();
        }
        return height;
    }

    /** How many of a row's lines from {@code from} on fit in {@code room}; none when not even one does. */
    int fitting(final int row, final int from, final int room) {
        int to = from;
        while (to < this.lines(row) && this.height(row, from, to + 1) <= room) {
            to++;
        }
        return to - from;
    }

    /** The head, its top left corner at {@code x}, {@code y}: a shaded band with the heads of the columns on it. */
    List<GuidePiece> head(final int x, final int y) {
        final List<GuidePiece> pieces = new ArrayList<>();
        if (this.headHeight == 0) {
            return pieces;
        }
        pieces.add(new GuidePiece.Box(x, y, this.width, this.headHeight, GuideStyle.SHADE, GuideStyle.SHADE));
        for (final Cell cell : this.head) {
            pieces.add(cell.at(x, y + ROW_TOP));
        }
        return pieces;
    }

    /**
     * The lines {@code from} to {@code to} of a row, their top at {@code y}: under the last line of a row, its rule; a
     * row cut between two lines has none until it ends.
     */
    List<GuidePiece> slice(final int x, final int y, final int row, final int from, final int to) {
        final Row laid = this.rows.get(row);
        final List<GuidePiece> pieces = new ArrayList<>();
        int lineY = y + ROW_TOP;
        for (int line = from; line < to; line++) {
            final Line placed = laid.lines().get(line);
            for (final Cell cell : placed.cells()) {
                pieces.add(cell.at(x, lineY));
            }
            lineY += placed.pitch();
        }
        if (to == laid.lines().size()) {
            pieces.add(new GuidePiece.Rule(x, y + this.height(row, from, to) - laid.rule(), this.width,
                    GuideStyle.RULE));
        }
        return pieces;
    }

    /**
     * How wide each column is when they stand side by side, or null when they cannot: when their longest words are
     * wider together than the room, or a column of words would be left too narrow to read.
     */
    private static int[] widths(final List<String> heads, final List<List<String>> cells, final TextSize[] sizes,
                                final int width, final IGuideText text) {
        final int columns = heads.size();
        final int[] least = new int[columns];
        final int[] most = new int[columns];
        int leastSum = 0;
        int mostSum = 0;
        for (int column = 0; column < columns; column++) {
            least[column] = widestWord(heads.get(column), TextSize.SMALL_BOLD, text);
            most[column] = text.width(heads.get(column), TextSize.SMALL_BOLD);
            for (final List<String> row : cells) {
                least[column] = Math.max(least[column], widestWord(row.get(column), sizes[column], text));
                most[column] = Math.max(most[column], text.width(row.get(column), sizes[column]));
            }
            least[column] += 2 * PAD;
            most[column] += 2 * PAD;
            leastSum += least[column];
            mostSum += most[column];
        }
        if (leastSum > width) {
            return null;
        }
        final int[] widths = new int[columns];
        int given = 0;
        for (int column = 0; column < columns; column++) {
            if (mostSum <= width) {
                // Every cell fits on one line: the room left over is shared out by how much each column holds.
                widths[column] = most[column] + (int) ((long) (width - mostSum) * most[column] / mostSum);
            } else {
                widths[column] = least[column] + (int) ((long) (most[column] - least[column]) * (width - leastSum)
                        / (mostSum - leastSum));
            }
            given += widths[column];
        }
        widths[columns - 1] += width - given;
        for (int column = 0; column < columns; column++) {
            if (sizes[column] != TextSize.CODE && widths[column] < most[column] && widths[column] < MIN_PROSE) {
                return null;
            }
        }
        return widths;
    }

    /** The columns side by side: the heads on the shaded band, each row's cells wrapped to their columns. */
    private static GuideTable sideBySide(final List<String> heads, final List<List<String>> cells,
                                         final TextSize[] sizes, final int[] widths, final int width,
                                         final IGuideText text) {
        final List<Cell> head = new ArrayList<>();
        int headLines = 1;
        int x = 0;
        for (int column = 0; column < heads.size(); column++) {
            final List<String> lines = GuideLayout.wrap(heads.get(column), widths[column] - 2 * PAD,
                    TextSize.SMALL_BOLD, text);
            for (int line = 0; line < lines.size(); line++) {
                head.add(new Cell(x + PAD, line * LINE, lines.get(line), TextSize.SMALL_BOLD, GuideStyle.INK));
            }
            headLines = Math.max(headLines, lines.size());
            x += widths[column];
        }
        final List<Row> rows = new ArrayList<>();
        for (final List<String> row : cells) {
            final List<List<String>> wrapped = new ArrayList<>();
            int count = 1;
            for (int column = 0; column < heads.size(); column++) {
                final List<String> lines = GuideLayout.wrap(row.get(column), widths[column] - 2 * PAD, sizes[column],
                        text);
                wrapped.add(lines);
                count = Math.max(count, lines.size());
            }
            final List<Line> lines = new ArrayList<>();
            for (int line = 0; line < count; line++) {
                final List<Cell> placed = new ArrayList<>();
                int cellX = 0;
                for (int column = 0; column < heads.size(); column++) {
                    if (line < wrapped.get(column).size()) {
                        placed.add(new Cell(cellX + PAD, rise(sizes[column]), wrapped.get(column).get(line),
                                sizes[column], GuideStyle.INK));
                    }
                    cellX += widths[column];
                }
                lines.add(new Line(LINE, placed));
            }
            rows.add(new Row(lines, ROW_BOTTOM, 1));
        }
        // Every column is as wide as its longest word, so side by side no word is ever cut.
        return new GuideTable(width, false, ROW_TOP + headLines * LINE, head, rows, List.of());
    }

    /**
     * The rows stacked: each row's first cell in the heading's colour with its second beside it, then each further
     * cell set in beside its column's head, small and faint, as a reference lists a code, what it says, what it means
     * and how to fix it.
     */
    private static GuideTable stacked(final List<String> heads, final List<List<String>> cells,
                                      final TextSize[] sizes, final int width, final IGuideText text) {
        int widest = 0;
        for (final List<String> row : cells) {
            widest = Math.max(widest, text.width(row.getFirst(), sizes[0]));
        }
        for (int column = 2; column < heads.size(); column++) {
            widest = Math.max(widest, text.width(heads.get(column), TextSize.SMALL_BOLD));
        }
        final int indent = Math.min(width / 2, PAD + widest + INDENT_GAP);
        final int headRoom = indent - PAD - INDENT_GAP;
        final int cellRoom = width - indent;
        final List<String> cut = new ArrayList<>();
        for (int column = 2; column < heads.size(); column++) {
            cut.addAll(tooWide(heads.get(column), headRoom, TextSize.SMALL_BOLD, text));
        }
        final List<Row> rows = new ArrayList<>();
        for (final List<String> row : cells) {
            cut.addAll(tooWide(row.getFirst(), headRoom, sizes[0], text));
            for (int column = 1; column < heads.size(); column++) {
                cut.addAll(tooWide(row.get(column), cellRoom, sizes[column], text));
            }
            final List<Line> lines = new ArrayList<>();
            final List<String> keys = GuideLayout.wrap(row.getFirst(), headRoom, sizes[0], text);
            final List<String> firsts = heads.size() > 1
                    ? GuideLayout.wrap(row.get(1), cellRoom, sizes[1], text) : List.of();
            final int opening = Math.max(keys.size(), firsts.size());
            for (int line = 0; line < opening; line++) {
                final List<Cell> placed = new ArrayList<>();
                if (line < keys.size()) {
                    placed.add(new Cell(PAD, rise(sizes[0]), keys.get(line), sizes[0], GuideStyle.HEADING));
                }
                if (line < firsts.size()) {
                    placed.add(new Cell(indent, rise(sizes[1]), firsts.get(line), sizes[1], GuideStyle.INK));
                }
                final boolean more = line == opening - 1 && heads.size() > 2;
                lines.add(new Line(LINE + (more ? STACK_GAP : 0), placed));
            }
            for (int column = 2; column < heads.size(); column++) {
                final List<String> labels = GuideLayout.wrap(heads.get(column), headRoom, TextSize.SMALL_BOLD, text);
                final List<String> words = GuideLayout.wrap(row.get(column), cellRoom, sizes[column], text);
                for (int line = 0; line < Math.max(labels.size(), words.size()); line++) {
                    final List<Cell> placed = new ArrayList<>();
                    if (line < labels.size()) {
                        placed.add(new Cell(PAD, 0, labels.get(line), TextSize.SMALL_BOLD, GuideStyle.FAINT));
                    }
                    if (line < words.size()) {
                        placed.add(new Cell(indent, rise(sizes[column]), words.get(line), sizes[column],
                                GuideStyle.INK));
                    }
                    lines.add(new Line(LINE, placed));
                }
            }
            rows.add(new Row(lines, STACK_BOTTOM, 2));
        }
        return new GuideTable(width, true, 0, List.of(), rows, cut);
    }

    /**
     * The words wider than the room that wrapping them there cuts between two letters: a path or a piece of code
     * broken where it breaks (after a slash, a dot, a bracket...) is not cut.
     */
    private static List<String> tooWide(final String words, final int room, final TextSize size,
                                        final IGuideText text) {
        final List<String> wide = new ArrayList<>();
        for (final String word : words.split(" ")) {
            if (!word.isEmpty() && text.width(word, size) > room && cutBetweenLetters(word, room, size, text)) {
                wide.add(word);
            }
        }
        return wide;
    }

    /** Whether breaking the word into lines of that room needs a cut where nothing breaks. */
    private static boolean cutBetweenLetters(final String word, final int room, final TextSize size,
                                             final IGuideText text) {
        int at = 0;
        final List<String> pieces = GuideLayout.pieces(word, room, size, text);
        for (int i = 0; i < pieces.size() - 1; i++) {
            at += pieces.get(i).length();
            if (!GuideLayout.breaksBefore(word, at)) {
                return true;
            }
        }
        return false;
    }

    /** How far a line of text at that size is raised above a line of small words, so the two share a baseline. */
    private static int rise(final TextSize size) {
        return size == TextSize.CODE ? -CODE_RISE : 0;
    }

    /** How wide the widest of the words is, the room a column needs to break none of them. */
    private static int widestWord(final String words, final TextSize size, final IGuideText text) {
        int widest = 0;
        for (final String word : words.split(" ")) {
            widest = Math.max(widest, text.width(word, size));
        }
        return widest;
    }

    /**
     * A piece of text of a table, placed from the table's left edge and the top of its line.
     *
     * @param x      how far in from the table's left edge
     * @param y      how far down from the top of its line, less when it is raised
     * @param text   the words
     * @param size   their size
     * @param colour their colour
     */
    private record Cell(int x, int y, String text, TextSize size, String colour) {

        GuidePiece.Text at(final int left, final int top) {
            return new GuidePiece.Text(left + this.x, top + this.y, this.text, this.size, this.colour, "");
        }
    }

    /** A line of a row: how far it is to the next, and what stands on it. */
    private record Line(int pitch, List<Cell> cells) {
    }

    /**
     * A row: its lines, the room under the last of them, and how far up from the row's end its rule is drawn.
     */
    private record Row(List<Line> lines, int bottom, int rule) {
    }
}
