/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.term.TermFace;
import dev.jstech.computers.client.term.TermText;
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
 * a second buffer under the file, all in the terminal font, one character to a cell, in the size of the terminal the
 * editor has taken.
 *
 * <p>A terminal has two colours and one trick, which is swapping them, so everything here that has to stand
 * out is drawn in the text's colour with the ground's colour written on it. A bar like that is one cell tall,
 * the height of the letters on it, so it never hangs below its own text on a terminal whose rows are further
 * apart.
 */
final class TtyChrome {

    private static final int PAD = 3;

    private TtyChrome() {
    }

    /** The title row: what the editor is, the file in the middle, and whether it has been changed. */
    static void title(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                      final int width, final int rowHeight, final TtyLook look, final InkPalette palette) {
        g.fill(x, y, x + width, y + face.height(), palette.plain());
        final String left = look.titleLeft();
        TermText.draw(face, g, font, left, x + PAD, y, palette.ground(), palette.plain());
        final String right = GameText.resolve(look.titleRight());
        final int rightW = TermText.width(face, right);
        final int leftEnd = x + PAD + TermText.width(face, left) + face.width();
        final int rightStart = x + width - PAD - rightW;
        final String middle = TermText.first(face, GameText.resolve(look.titleMiddle()),
                Math.max(0, rightStart - leftEnd - face.width()));
        // In the middle of the row when there is room, and after the name when there is not.
        final int centred = x + (width - TermText.width(face, middle)) / 2;
        TermText.draw(face, g, font, middle, Math.max(leftEnd, centred), y, palette.ground(), palette.plain());
        TermText.draw(face, g, font, right, rightStart, y, palette.ground(), palette.plain());
    }

    /** Something said in passing: in brackets, in the middle of the row, only as wide as what it says. */
    static void bracketed(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                          final int width, final int rowHeight, final String said, final InkPalette palette) {
        if (said.isEmpty()) {
            return;
        }
        final String shown = TermText.first(face, said, width - 2 * PAD);
        final int at = x + (width - TermText.width(face, shown)) / 2;
        g.fill(at - 2, y, at + TermText.width(face, shown) + 2, y + face.height(), palette.plain());
        TermText.draw(face, g, font, shown, at, y, palette.ground(), palette.plain());
    }

    /** A question being answered: a bar the whole width, read from the left, with a caret after the answer. */
    static void bar(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                    final int width, final int rowHeight, final String asked, final InkPalette palette) {
        g.fill(x, y, x + width, y + face.height(), palette.plain());
        final String shown = TermText.first(face, asked, width - 2 * PAD - face.width());
        TermText.draw(face, g, font, shown, x + PAD, y, palette.ground(), palette.plain());
        final int caret = x + PAD + TermText.width(face, shown);
        g.fill(caret, y, caret + face.width(), y + face.height(), palette.ground());
    }

    /**
     * A row written plainly on the glass's own ground, with nothing behind it: the editors of an older age
     * that never kept a ruler in the corner say what they have to say this way, and the same row is where
     * one of them names where the caret stands, right under its keys.
     */
    static void message(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                        final int width, final String said, final InkPalette palette) {
        TermText.draw(face, g, font, TermText.first(face, said, width - 2 * PAD), x + PAD, y, palette.plain(),
                palette.ground());
    }

    /**
     * The rows of keys: each key written the way a terminal makes things stand out, and what it does after
     * it. A chord badged in a bar of its own is how most of these editors write one; {@code bare} is an
     * editor that instead prints its chords in bright ink on the glass, with no bar behind them.
     *
     * <p>Each key starts at its column, or a cell after the end of the key before it when that one ran long, the way
     * the editors print them: on eighty columns their own words just fit, a long one nudging the next along.
     */
    static void keys(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                     final int width, final int rowHeight, final List<List<TtyLook.Key>> rows,
                     final InkPalette palette, final boolean bare) {
        final int right = x + width - PAD;
        int ry = y;
        for (final List<TtyLook.Key> row : rows) {
            final int column = row.isEmpty() ? width : width / row.size();
            int start = x + PAD;
            int free = start;
            for (final TtyLook.Key key : row) {
                final int at = Math.max(start, free);
                start += column;
                if (at >= right) {
                    break;
                }
                if (!key.chord().isEmpty()) {
                    final int chordW = TermText.width(face, key.chord());
                    if (bare) {
                        TermText.draw(face, g, font, key.chord(), at, ry, palette.bright(), palette.ground());
                    } else {
                        g.fill(at, ry, at + chordW, ry + face.height(), palette.plain());
                        TermText.draw(face, g, font, key.chord(), at, ry, palette.ground(), palette.plain());
                    }
                    // A cell between the chord and what it does, as the editors print them.
                    final int from = at + chordW + face.width();
                    final String does = TermText.first(face, GameText.resolve(key.does()), Math.max(0, right - from));
                    TermText.draw(face, g, font, does, from, ry, palette.plain(), palette.ground());
                    free = from + TermText.width(face, does) + face.width();
                } else if (!key.does().isEmpty()) {
                    // A cell with no chord to badge, such as a tip written out whole rather than a key it names.
                    final String does = TermText.first(face, GameText.resolve(key.does()), Math.max(0, right - at));
                    TermText.draw(face, g, font, does, at, ry, palette.plain(), palette.ground());
                    free = at + TermText.width(face, does) + face.width();
                }
            }
            ry += rowHeight;
        }
    }

    /**
     * A box drawn over the text, sized to what it holds in cells: a filled rectangle with a plain outline round
     * it, the title in bright ink and one row for each item.
     *
     * <p>The box starts at the column and row {@link TtyMenuBox} works out, and never draws past what it is
     * given: an item too long for it is trimmed, and a menu with more rows than it has room for scrolls the
     * chosen one into view rather than spilling over whatever is drawn under it.
     */
    static void menu(final TermFace face, final GuiGraphics g, final Font font, final int areaX, final int areaY,
                     final int areaWidth, final int areaHeight, final int textRow, final TtyLook.Menu menu,
                     final InkPalette palette) {
        final int itemRow = face.height() + 2;
        final List<String> items = new ArrayList<>(menu.items().size());
        int innerW = TermText.width(face, GameText.resolve(menu.title()));
        for (int i = 0; i < menu.items().size(); i++) {
            final String item = (char) ('a' + i) + ") " + GameText.resolve(menu.items().get(i));
            items.add(item);
            innerW = Math.max(innerW, TermText.width(face, item));
        }
        final TtyMenuBox box = TtyMenuBox.of(areaWidth, areaHeight, face.width(), textRow, PAD, itemRow, innerW,
                items.size(), menu.selected());
        final int bx = areaX + box.x();
        final int by = areaY + box.y();
        final int innerWidth = Math.max(0, box.width() - 4 * PAD);
        g.fill(bx, by, bx + box.width(), by + box.height(), palette.ground());
        Draw.outline(g, bx, by, box.width(), box.height(), palette.plain());
        TermText.draw(face, g, font, TermText.first(face, GameText.resolve(menu.title()), innerWidth), bx + 2 * PAD,
                by + PAD, palette.bright(), palette.ground());
        Draw.pushScissor(g, bx, by, bx + box.width(), by + box.height());
        for (int i = box.topRow(); i < items.size() && i - box.topRow() < box.visibleRows(); i++) {
            final int iy = by + PAD + (i - box.topRow() + 2) * itemRow;
            final String shown = TermText.first(face, items.get(i), innerWidth);
            if (i == menu.selected()) {
                g.fill(bx + 1, iy, bx + box.width() - 1, iy + face.height(), palette.selection());
                TermText.draw(face, g, font, shown, bx + 2 * PAD, iy, palette.ground(), palette.selection());
            } else {
                TermText.draw(face, g, font, shown, bx + 2 * PAD, iy, palette.plain(), palette.ground());
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
    static void lower(final TermFace face, final GuiGraphics g, final Font font, final int x, final int y,
                      final int width, final int height, final int rowHeight, final String name,
                      final List<String> lines, final InkPalette palette) {
        g.fill(x, y, x + width, y + rowHeight, palette.gutter());
        TermText.draw(face, g, font, TermText.first(face, "-UUU:%%--F1  " + name, width - 6), x + PAD, y,
                palette.plain(), palette.gutter());
        Draw.pushScissor(g, x, y + rowHeight, x + width, y + height);
        int ry = y + rowHeight + 1;
        for (final String line : lines) {
            if (ry + rowHeight > y + height) {
                break;
            }
            TermText.draw(face, g, font, line, x + PAD, ry, palette.plain(), palette.ground());
            ry += rowHeight;
        }
        Draw.popScissor(g);
    }
}
