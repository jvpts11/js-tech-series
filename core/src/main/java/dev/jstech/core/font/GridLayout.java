/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays a row of a monospace grid out into the fewest pieces that put every character in its own cell.
 *
 * <p>Drawing one character at a time would be one draw at a time, a great many for a screen of eighty columns. But a
 * font moves its pen by how wide each glyph is, so a string only lands on the grid while every character in it moves
 * the pen exactly one cell. So a run of such characters in one style and one font is one piece, and anything else
 * ends the run and is a piece of its own: a character of the grid's own font that moves the pen more or less than a
 * cell is drawn at its cell's left edge, where the font's picture already holds its place in the cell; a character
 * only the game's font has is drawn in the middle of its cell; a box line or a block is drawn by the grid to fill its
 * cell ({@link CellGlyphs}); a space or a control character draws nothing and only ends the run.
 */
public final class GridLayout {

    private GridLayout() {
    }

    /** The pieces that draw that row, in order across it. */
    public static <S> List<IGridPiece<S>> layout(final List<GridSpan<S>> row, final IGridMetrics metrics) {
        final List<IGridPiece<S>> out = new ArrayList<>();
        final int cellWidth = metrics.cellWidth();
        final StringBuilder run = new StringBuilder();
        int cell = 0;
        int runAt = 0;
        boolean runCellFont = false;
        for (final GridSpan<S> span : row) {
            final S style = span.style();
            final int[] codePoints = span.text().codePoints().toArray();
            for (final int codePoint : codePoints) {
                final int x = cell * cellWidth;
                cell++;
                if (codePoint == ' ' || Character.isISOControl(codePoint)) {
                    flush(out, run, runAt, style, runCellFont);
                    continue;
                }
                if (CellGlyphs.isWholeCell(codePoint)) {
                    flush(out, run, runAt, style, runCellFont);
                    out.add(new IGridPiece.Whole<>(codePoint, x, style));
                    continue;
                }
                final boolean cellFont = metrics.inCellFont(codePoint);
                final int advance = metrics.advance(codePoint, cellFont);
                if (advance == cellWidth) {
                    if (!run.isEmpty() && runCellFont != cellFont) {
                        flush(out, run, runAt, style, runCellFont);
                    }
                    if (run.isEmpty()) {
                        runAt = x;
                        runCellFont = cellFont;
                    }
                    run.appendCodePoint(codePoint);
                } else {
                    flush(out, run, runAt, style, runCellFont);
                    final int at = cellFont ? x : x + Math.max(0, (cellWidth - advance) / 2);
                    out.add(new IGridPiece.Text<>(new String(Character.toChars(codePoint)), at, style, cellFont));
                }
            }
            flush(out, run, runAt, style, runCellFont);
        }
        return List.copyOf(out);
    }

    private static <S> void flush(final List<IGridPiece<S>> out, final StringBuilder run, final int at, final S style,
                                  final boolean cellFont) {
        if (!run.isEmpty()) {
            out.add(new IGridPiece.Text<>(run.toString(), at, style, cellFont));
            run.setLength(0);
        }
    }
}
