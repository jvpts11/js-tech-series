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
import dev.jstech.computers.os.edit.TtyMenuBox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
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
        final String right = GameText.resolve(look.titleRight());
        final int rightW = font.width(right);
        final int leftEnd = x + PAD + font.width(look.titleLeft()) + 6;
        final int rightStart = x + width - PAD - rightW;
        final String middle = font.plainSubstrByWidth(GameText.resolve(look.titleMiddle()),
                Math.max(0, rightStart - leftEnd - 6));
        // In the middle of the row when there is room, and after the name when there is not.
        final int centred = x + (width - font.width(middle)) / 2;
        g.drawString(font, middle, Math.max(leftEnd, centred), y, palette.ground(), false);
        g.drawString(font, right, rightStart, y, palette.ground(), false);
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

    /**
     * A row written plainly on the glass's own ground, with nothing behind it: the editors of an older age
     * that never kept a ruler in the corner say what they have to say this way, and the same row is where
     * one of them names where the caret stands, right under its keys.
     */
    static void message(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                        final String said, final InkPalette palette) {
        Draw.text(g, font, font.plainSubstrByWidth(said, width - 2 * PAD), x + PAD, y, palette.plain(),
                palette.ground());
    }

    /**
     * The rows of keys: each key written the way a terminal makes things stand out, and what it does after
     * it. A chord badged in a bar of its own is how most of these editors write one; {@code bare} is an
     * editor that instead prints its chords in bright ink on the glass, with no bar behind them.
     */
    static void keys(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                     final int rowHeight, final List<List<TtyLook.Key>> rows, final InkPalette palette,
                     final boolean bare) {
        int ry = y;
        for (final List<TtyLook.Key> row : rows) {
            final int column = row.isEmpty() ? width : width / row.size();
            int cx = x + PAD;
            for (final TtyLook.Key key : row) {
                if (!key.chord().isEmpty()) {
                    final int chordW = font.width(key.chord());
                    if (bare) {
                        Draw.text(g, font, key.chord(), cx, ry, palette.bright(), palette.ground());
                    } else {
                        // A pixel of the bar either side of the letters, without which they run into its edges.
                        g.fill(cx - 1, ry - 1, cx + chordW + 1, ry + LETTERS - 1, palette.plain());
                        g.drawString(font, key.chord(), cx, ry, palette.ground(), false);
                    }
                    Draw.text(g, font,
                            font.plainSubstrByWidth(GameText.resolve(key.does()), Math.max(0, column - chordW - 8)),
                            cx + chordW + 4, ry, palette.plain(), palette.ground());
                } else if (!key.does().isEmpty()) {
                    // A cell with no chord to badge, such as a tip written out whole rather than a key it names.
                    Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(key.does()), Math.max(0, column)),
                            cx, ry, palette.plain(), palette.ground());
                }
                cx += column;
            }
            ry += rowHeight;
        }
    }

    /**
     * A box drawn over the text, sized to what it holds rather than counted in characters: a terminal's
     * font is not fixed-width, so a border of {@code -} and {@code |} would not meet its own corners. A
     * filled rectangle and a plain outline do the same job without needing to.
     *
     * <p>The box starts at the column and row {@link TtyMenuBox} works out, and never draws past what it is
     * given: an item too long for it is trimmed, and a menu with more rows than it has room for scrolls the
     * chosen one into view rather than spilling over whatever is drawn under it.
     */
    static void menu(final GuiGraphics g, final Font font, final int areaX, final int areaY, final int areaWidth,
                     final int areaHeight, final int textRow, final TtyLook.Menu menu, final InkPalette palette) {
        final int itemRow = LETTERS + 2;
        final List<String> items = new ArrayList<>(menu.items().size());
        int innerW = font.width(GameText.resolve(menu.title()));
        for (int i = 0; i < menu.items().size(); i++) {
            final String item = (char) ('a' + i) + ") " + GameText.resolve(menu.items().get(i));
            items.add(item);
            innerW = Math.max(innerW, font.width(item));
        }
        final int cell = Math.max(1, font.width("m"));
        final TtyMenuBox box = TtyMenuBox.of(areaWidth, areaHeight, cell, textRow, PAD, itemRow, innerW,
                items.size(), menu.selected());
        final int bx = areaX + box.x();
        final int by = areaY + box.y();
        final int innerWidth = Math.max(0, box.width() - 4 * PAD);
        g.fill(bx, by, bx + box.width(), by + box.height(), palette.ground());
        Draw.outline(g, bx, by, box.width(), box.height(), palette.plain());
        Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(menu.title()), innerWidth), bx + 2 * PAD,
                by + PAD, palette.bright(), palette.ground());
        Draw.pushScissor(g, bx, by, bx + box.width(), by + box.height());
        for (int i = box.topRow(); i < items.size() && i - box.topRow() < box.visibleRows(); i++) {
            final int iy = by + PAD + (i - box.topRow() + 2) * itemRow;
            final String shown = font.plainSubstrByWidth(items.get(i), innerWidth);
            if (i == menu.selected()) {
                g.fill(bx + 1, iy - 1, bx + box.width() - 1, iy + LETTERS, palette.selection());
                g.drawString(font, shown, bx + 2 * PAD, iy, palette.ground(), false);
            } else {
                Draw.text(g, font, shown, bx + 2 * PAD, iy, palette.plain(), palette.ground());
            }
        }
        Draw.popScissor(g);
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
