/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
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
 * <p>One character at a time would be one draw at a time, which is a great many for a glass of sixty-four
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
     */
    public void draw(final GuiGraphics g, final Font font, final List<TermRow> rows, final int x, final int y,
                     final int pitch, final ToIntFunction<CliStyle> colorOf) {
        final Matrix4f pose = g.pose().last().pose();
        int at = y;
        for (final TermRow row : rows) {
            for (final Piece piece : this.worked.computeIfAbsent(row, r -> piecesOf(r, font))) {
                font.drawInBatch(piece.text(), x + piece.x(), at, colorOf.applyAsInt(piece.style()), false, pose,
                        g.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            at += pitch;
        }
        g.flush();
    }

    /** One row of the glass on its own, for a view that is handed its rows one at a time. */
    public void drawRow(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                        final ToIntFunction<CliStyle> colorOf) {
        final Matrix4f pose = g.pose().last().pose();
        for (final Piece piece : this.worked.computeIfAbsent(row, r -> piecesOf(r, font))) {
            font.drawInBatch(piece.text(), x + piece.x(), y, colorOf.applyAsInt(piece.style()), false, pose,
                    g.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        g.flush();
    }

    /** One row on its own, for the line being typed, which changes too often to be worth remembering. */
    public void drawOnce(final GuiGraphics g, final Font font, final TermRow row, final int x, final int y,
                         final ToIntFunction<CliStyle> colorOf) {
        final Matrix4f pose = g.pose().last().pose();
        for (final Piece piece : piecesOf(row, font)) {
            font.drawInBatch(piece.text(), x + piece.x(), y, colorOf.applyAsInt(piece.style()), false, pose,
                    g.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        g.flush();
    }

    /** How many cells fit across that many pixels at that scale. */
    public static int columnsIn(final int pixels, final float scale) {
        return Math.max(8, (int) (pixels / (CELL * scale)));
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
