/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermGrid;
import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.gui.term.TermSelection;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.font.GridPainter;
import dev.jstech.core.font.GridSpan;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a terminal's rows on a grid, in the terminal font: every character in a cell of its own, all the cells the
 * same width.
 *
 * <p>The terminals draw in Misc Fixed, the font of the X terminals and the Unix consoles, in the size their screen
 * fits best ({@link TermFace}), through the Core's grid painter: the columns of a table line up, a bar made of one
 * character holds still as its line redraws, and the box lines and blocks are drawn to fill their cells so a frame
 * meets its own corners and a bar has no gaps. A character the font lacks is drawn in the game's font, in the middle
 * of its cell. A row is laid out once and kept for as long as it is on the glass, and the whole glass goes to the
 * card in one batch.
 */
public final class TermPainter {

    private final Map<TermFace, GridPainter<CliStyle>> grids = new HashMap<>();
    private TermFace face = TermFace.SMALL;

    /** How wide a cell of the small size is before any scaling: a terminal window's. */
    public static final int CELL = TermGrid.CELL;
    /** How tall a row of the small size is before any scaling: a terminal window's. */
    public static final int ROW = TermGrid.ROW;

    /** Draws in that size of the font from now on. */
    public void use(final TermFace size) {
        this.face = size;
    }

    /** The size of the font this draws in. */
    public TermFace face() {
        return face;
    }

    /**
     * Draws those rows downwards from a point, in the pose the caller has set up.
     *
     * @param pitch   how far apart the rows are, in the same units as the pose
     * @param colorOf the colour a style has on this glass, which a one-colour tube answers differently
     * @param ground  the colour of the glass the rows are on, which a shadow under them would be worked out against
     */
    public void draw(final GuiGraphics g, final Font font, final List<TermRow> rows, final int x, final int y,
                     final int pitch, final ToIntFunction<CliStyle> colorOf, final int ground) {
        grid().draw(g, font, rows, TermPainter::spans, x, y, pitch, colorOf, ground);
    }

    /** One row of the glass on its own, for a view that is handed its rows one at a time. */
    public void drawRow(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                        final int pitch, final ToIntFunction<CliStyle> colorOf, final int ground) {
        grid().draw(g, font, List.of(row), TermPainter::spans, x, y, pitch, colorOf, ground);
    }

    /** One row on its own, for the line being typed, which changes too often to be worth remembering. */
    public void drawOnce(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                         final int pitch, final ToIntFunction<CliStyle> colorOf, final int ground) {
        grid().drawOnce(g, font, spans(row), x, y, pitch, colorOf, ground);
    }

    /** How many cells of the small size fit across that many pixels at that scale. */
    public static int columnsIn(final int pixels, final float scale) {
        return Math.max(8, (int) (pixels / (CELL * scale)));
    }

    /** Which cell of the small size across a row a pointer that far from the left of the glass is in. */
    public static int columnAt(final double pixels, final float scale) {
        return columnAt(pixels, CELL, scale);
    }

    /** Which cell that wide across a row a pointer that far from the left of the glass is in. */
    public static int columnAt(final double pixels, final int cellWidth, final float scale) {
        return Math.max(0, (int) (pixels / (cellWidth * scale)));
    }

    /** Which row down the glass a pointer that far from the top of it is in. */
    public static int rowAt(final double pixels, final int pitch, final float scale) {
        return (int) Math.floor(pixels / (pitch * scale));
    }

    /**
     * Fills the cells a selection takes, under the letters, in cells of that width.
     *
     * <p>Drawn in the same pose and the same units the rows are, and told which row of the buffer the first of
     * them is, since a glass shows a window onto a buffer that is taller than it.
     *
     * @param firstRow which row of the buffer {@code rows} starts at
     */
    public static void highlight(final GuiGraphics g, final List<TermRow> rows, final int x, final int y,
                                 final int pitch, final int firstRow, final TermSelection selection,
                                 final int color, final int cellWidth) {
        if (selection.isEmpty()) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            final int row = firstRow + i;
            if (row < selection.startRow() || row > selection.endRow()) {
                continue;
            }
            final int cells = rows.get(i).length();
            final int from = row == selection.startRow() ? Math.min(selection.startColumn(), cells) : 0;
            final int to = row == selection.endRow() ? Math.min(selection.endColumn(), cells) : cells;
            if (from < to) {
                g.fill(x + from * cellWidth, y + i * pitch, x + to * cellWidth, y + i * pitch + pitch, color);
            }
        }
    }

    private GridPainter<CliStyle> grid() {
        return grids.computeIfAbsent(face, size -> new GridPainter<>(size.font()));
    }

    /** A row's runs as the spans the grid lays out, each in its own style. */
    private static List<GridSpan<CliStyle>> spans(final TermRow row) {
        return row.runs().stream().map(run -> new GridSpan<>(run.text(), run.style())).toList();
    }
}
