/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.gui.term.TermSelection;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.gui.TextShadow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

/**
 * Draws a terminal's rows on a grid: every character in a cell of its own, all the cells the same width.
 *
 * <p>The game's font is not a terminal's. Its letters are as wide as they need to be, so a column of figures
 * does not line up under another, a bar made of one character is a different length from the same bar made of
 * another, and a line that redraws itself jitters as its digits change. A terminal puts each character in a
 * cell, and once it does all of that goes away: tables line up, bars hold still, and the right-hand edge of a
 * status column is an edge.
 *
 * <p>One character at a time would be one draw at a time, which is a great many for a glass of eighty
 * columns. So a row is worked out once, into the fewest strings that land on the grid when drawn the ordinary
 * way: a run of characters that each fill their cell is one string, and only a narrow one, which has to be
 * nudged to the middle of its cell, starts another. The whole glass then goes to the card in one batch.
 */
public final class TermPainter {

    /** What each row came to the last time it was worked out, for as long as the row is on the glass. */
    private final Map<TermRow, List<Piece>> worked = new WeakHashMap<>();

    /** How wide a cell is before any scaling, which is what nearly every character in the font takes. */
    public static final int CELL = 6;

    /**
     * Draws those rows downwards from a point, in the pose the caller has set up.
     *
     * @param pitch   how far apart the rows are, in the same units as the pose
     * @param colorOf the colour a style has on this glass, which a one-colour tube answers differently
     * @param ground  the colour of the glass the rows are on, which is what their shadow is worked out against
     */
    public void draw(final GuiGraphics g, final Font font, final List<TermRow> rows, final int x, final int y,
                     final int pitch, final ToIntFunction<CliStyle> colorOf, final int ground) {
        final Matrix4f pose = g.pose().last().pose();
        int at = y;
        for (final TermRow row : rows) {
            pieces(g, font, this.worked.computeIfAbsent(row, r -> piecesOf(r, font)), x, at, colorOf, ground, pose);
            at += pitch;
        }
        g.flush();
    }

    /** One row of the glass on its own, for a view that is handed its rows one at a time. */
    public void drawRow(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                        final ToIntFunction<CliStyle> colorOf, final int ground) {
        pieces(g, font, this.worked.computeIfAbsent(row, r -> piecesOf(r, font)), x, y, colorOf, ground,
                g.pose().last().pose());
        g.flush();
    }

    /** One row on its own, for the line being typed, which changes too often to be worth remembering. */
    public void drawOnce(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                         final ToIntFunction<CliStyle> colorOf, final int ground) {
        pieces(g, font, piecesOf(row, font), x, y, colorOf, ground, g.pose().last().pose());
        g.flush();
    }

    /**
     * One row into the batch: every piece's shadow, and then every piece over them.
     *
     * <p>The shadow is a second copy a unit down and to the right, in a colour worked out from the letter and
     * the glass rather than the dark copy of the letter the game would use, which is only a shadow on a dark
     * ground. It goes in ahead of the letters so that no letter is ever under its neighbour's.
     */
    private static void pieces(final GuiGraphics g, final Font font, final List<Piece> row, final int x, final int y,
                               final ToIntFunction<CliStyle> colorOf, final int ground, final Matrix4f pose) {
        for (final Piece piece : row) {
            font.drawInBatch(piece.text(), x + piece.x() + 1, y + 1,
                    TextShadow.of(colorOf.applyAsInt(piece.style()), ground), false, pose, g.bufferSource(),
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        for (final Piece piece : row) {
            font.drawInBatch(piece.text(), x + piece.x(), y, colorOf.applyAsInt(piece.style()), false, pose,
                    g.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
    }

    /** How many cells fit across that many pixels at that scale. */
    public static int columnsIn(final int pixels, final float scale) {
        return Math.max(8, (int) (pixels / (CELL * scale)));
    }

    /** Which cell across a row a pointer that far from the left of the glass is in. */
    public static int columnAt(final double pixels, final float scale) {
        return Math.max(0, (int) (pixels / (CELL * scale)));
    }

    /** Which row down the glass a pointer that far from the top of it is in. */
    public static int rowAt(final double pixels, final int pitch, final float scale) {
        return (int) Math.floor(pixels / (pitch * scale));
    }

    /**
     * Fills the cells a selection takes, under the letters.
     *
     * <p>Drawn in the same pose and the same units the rows are, and told which row of the buffer the first of
     * them is, since a glass shows a window onto a buffer that is taller than it.
     *
     * @param firstRow which row of the buffer {@code rows} starts at
     */
    public static void highlight(final GuiGraphics g, final List<TermRow> rows, final int x, final int y,
                                 final int pitch, final int firstRow, final TermSelection selection,
                                 final int color) {
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
                g.fill(x + from * CELL, y + i * pitch, x + to * CELL, y + i * pitch + pitch, color);
            }
        }
    }

    /**
     * The fewest strings that put every character of a row in its cell.
     *
     * <p>A character that fills its cell lets the run it is in carry on, since the font will put the next one
     * exactly where the grid wants it. Anything narrower or wider ends the run and is placed by hand, in the
     * middle of its cell.
     */
    private static List<Piece> piecesOf(final TermRow row, final Font font) {
        final List<Piece> out = new ArrayList<>();
        final StringBuilder run = new StringBuilder();
        int cell = 0;
        int runAt = 0;
        for (final CliSpan span : row.runs()) {
            final String text = span.text();
            for (int i = 0; i < text.length(); i++) {
                final char ch = text.charAt(i);
                final int wide = ch == ' ' ? CELL : font.width(String.valueOf(ch));
                if (ch == ' ') {
                    /* Nothing to draw, and the run before it ends here so the gap is a whole cell. */
                    flush(out, run, runAt, span.style());
                } else if (wide == CELL) {
                    if (run.isEmpty()) {
                        runAt = cell * CELL;
                    }
                    run.append(ch);
                } else {
                    flush(out, run, runAt, span.style());
                    out.add(new Piece(String.valueOf(ch), cell * CELL + Math.max(0, (CELL - wide) / 2),
                            span.style()));
                }
                cell++;
            }
            flush(out, run, runAt, span.style());
        }
        return List.copyOf(out);
    }

    private static void flush(final List<Piece> out, final StringBuilder run, final int at, final CliStyle style) {
        if (!run.isEmpty()) {
            out.add(new Piece(run.toString(), at, style));
            run.setLength(0);
        }
    }

    /** A string and where across the row it starts. */
    private record Piece(String text, int x, CliStyle style) {
    }
}
