/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.gui.component.Draw;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws what a terminal editor keeps round the text: a title row, the row that talks, the rows of keys, and
 * a second buffer under the file.
 *
 * <p>A terminal has two colours and one trick, which is swapping them, so everything here that has to stand
 * out is drawn in the text's colour with the ground's colour written on it. What is written on a bar like
 * that has no shadow under it: the bar is only as tall as its letters, and a shadow would hang out of it.
 */
final class TtyChrome {

    private static final int PAD = 3;

    /**
     * How tall the letters are, which is how tall anything painted behind them is. Rows may be further apart
     * than that, on a terminal that spaces them to land on whole pixels, and a bar as tall as the row would
     * then hang below its own text.
     */
    private static final int LETTERS = 9;

    private TtyChrome() {
    }

    /** The title row: what the editor is, the file in the middle, and whether it has been changed. */
    static void title(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                      final int rowHeight, final TtyLook look, final InkPalette palette) {
        g.fill(x, y - 1, x + width, y + LETTERS, palette.plain());
        g.drawString(font, look.titleLeft(), x + PAD, y, palette.ground(), false);
        final int rightW = font.width(look.titleRight());
        final int leftEnd = x + PAD + font.width(look.titleLeft()) + 6;
        final int rightStart = x + width - PAD - rightW;
        final String middle = font.plainSubstrByWidth(look.titleMiddle(), Math.max(0, rightStart - leftEnd - 6));
        // In the middle of the row when there is room, and after the name when there is not.
        final int centred = x + (width - font.width(middle)) / 2;
        g.drawString(font, middle, Math.max(leftEnd, centred), y, palette.ground(), false);
        g.drawString(font, look.titleRight(), rightStart, y, palette.ground(), false);
    }

    /** Something said in passing: in brackets, in the middle of the row, only as wide as what it says. */
    static void bracketed(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                          final int rowHeight, final String said, final InkPalette palette) {
        if (said.isEmpty()) {
            return;
        }
        final String shown = font.plainSubstrByWidth(said, width - 2 * PAD);
        final int at = x + (width - font.width(shown)) / 2;
        g.fill(at - 2, y - 1, at + font.width(shown) + 2, y + LETTERS, palette.plain());
        g.drawString(font, shown, at, y, palette.ground(), false);
    }

    /** A question being answered: a bar the whole width, read from the left, with a caret after the answer. */
    static void bar(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                    final int rowHeight, final String asked, final InkPalette palette) {
        g.fill(x, y - 1, x + width, y + LETTERS, palette.plain());
        final String shown = font.plainSubstrByWidth(asked, width - 2 * PAD - font.width("m"));
        g.drawString(font, shown, x + PAD, y, palette.ground(), false);
        final int caret = x + PAD + font.width(shown);
        g.fill(caret, y, caret + font.width("m"), y + LETTERS - 1, palette.ground());
    }

    /** The rows of keys: each key written the way a terminal makes things stand out, and what it does after it. */
    static void keys(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                     final int rowHeight, final List<List<TtyLook.Key>> rows, final InkPalette palette) {
        int ry = y;
        for (final List<TtyLook.Key> row : rows) {
            final int column = row.isEmpty() ? width : width / row.size();
            int cx = x + PAD;
            for (final TtyLook.Key key : row) {
                if (!key.chord().isEmpty()) {
                    final int chordW = font.width(key.chord());
                    // A pixel of the bar either side of the letters, without which they run into its edges.
                    g.fill(cx - 1, ry - 1, cx + chordW + 1, ry + LETTERS - 1, palette.plain());
                    g.drawString(font, key.chord(), cx, ry, palette.ground(), false);
                    Draw.text(g, font, font.plainSubstrByWidth(key.does(), Math.max(0, column - chordW - 8)),
                            cx + chordW + 4, ry, palette.plain(), palette.ground());
                }
                cx += column;
            }
            ry += rowHeight;
        }
    }

    /**
     * A second buffer under the file: a mode line of its own naming it, then its lines.
     *
     * <p>What is in it is text somebody else produced, so it is drawn plainly rather than coloured as source:
     * it is a compiler talking, not a program.
     */
    static void lower(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                      final int height, final int rowHeight, final String name, final List<String> lines,
                      final InkPalette palette) {
        g.fill(x, y, x + width, y + rowHeight, palette.gutter());
        Draw.text(g, font, font.plainSubstrByWidth("-UUU:%%--F1  " + name, width - 6), x + PAD, y,
                palette.plain(), palette.gutter());
        Draw.pushScissor(g, x, y + rowHeight, x + width, y + height);
        int ry = y + rowHeight + 1;
        for (final String line : lines) {
            if (ry + rowHeight > y + height) {
                break;
            }
            Draw.text(g, font, line, x + PAD, ry, palette.plain(), palette.ground());
            ry += rowHeight;
        }
        Draw.popScissor(g);
    }
}
