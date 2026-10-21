/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.TextSize;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a manual's controls as the screen asks for them, each in the look its state gives: the buttons on the cover's
 * top edge with their marks, the page-turn arrows on its sides, and the chapter tabs at its edge. It holds nothing; the
 * screen says what each control is doing.
 */
final class ManualControls {

    /** How far a tab under the pointer goes towards white. */
    private static final double TAB_LIT = 0.3;
    /** The tab's bevel: its lit edge towards white, its shaded edge towards black. */
    private static final double TAB_LIGHT = 0.3;
    private static final double TAB_SHADE = 0.4;
    /** The magnifier of the search button, its ring and its handle, pixel by pixel. */
    private static final int[][] LENS = {{1, 0}, {2, 0}, {0, 1}, {3, 1}, {0, 2}, {3, 2}, {1, 3}, {2, 3}, {4, 4},
            {5, 5}};

    private ManualControls() {
    }

    /** A button on the cover's top edge: its face, its bevel and its mark, in the look of its state. */
    static void button(final GuiGraphics g, final ManualScreenLayout.Rect rect, final Kind kind,
                       final ManualButtonLook look) {
        bevel(g, rect.x(), rect.y(), rect.width(), rect.height(), look.fill(), look.light(), look.dark());
        if (look.framed()) {
            Draw.outline(g, rect.x(), rect.y(), rect.width(), rect.height(), look.mark());
        }
        final int x = rect.x();
        final int y = rect.y() + look.drop();
        final int mark = look.mark();
        switch (kind) {
            case CONTENTS -> {
                for (final int line : new int[] {2, 4, 6}) {
                    g.fill(x + 3, y + line, x + 11, y + line + 1, mark);
                }
            }
            case SEARCH -> {
                for (final int[] dot : LENS) {
                    g.fill(x + 4 + dot[0], y + 1 + dot[1], x + 5 + dot[0], y + 2 + dot[1], mark);
                }
            }
            case BOOKMARKS -> {
                // A ribbon hanging down, cut in a V at its end.
                g.fill(x + 5, y + 1, x + 9, y + 6, mark);
                g.fill(x + 5, y + 6, x + 6, y + 8, mark);
                g.fill(x + 8, y + 6, x + 9, y + 8, mark);
            }
            case TEAR -> {
                // A sheet, its left edge torn in teeth where it left the rings.
                g.fill(x + 6, y + 1, x + 10, y + 8, mark);
                for (int tooth = 0; tooth < 4; tooth++) {
                    g.fill(x + 5 - tooth % 2, y + 1 + 2 * tooth, x + 6, y + 2 + 2 * tooth, mark);
                }
            }
            case BACK, FORWARD -> {
                // A solid triangle, its point to the left for Back and to the right for Forward, and a short tail.
                final boolean back = kind == Kind.BACK;
                for (int i = 0; i < 4; i++) {
                    final int column = back ? x + 3 + i : x + 10 - i;
                    g.fill(column, y + 4 - i, column + 1, y + 5 + i, mark);
                }
                final int tail = back ? x + 7 : x + 5;
                g.fill(tail, y + 4, tail + 3, y + 5, mark);
            }
        }
    }

    /** A page-turn arrow on the cover's side: a little bevelled plate with a triangle pointing the way it turns. */
    static void arrow(final GuiGraphics g, final ManualScreenLayout.Rect rect, final boolean back,
                      final ManualButtonLook look) {
        bevel(g, rect.x(), rect.y(), rect.width(), rect.height(), look.fill(), look.light(), look.dark());
        if (look.framed()) {
            Draw.outline(g, rect.x(), rect.y(), rect.width(), rect.height(), look.mark());
        }
        final int y = rect.y() + look.drop();
        for (int i = 0; i < 4; i++) {
            final int column = back ? rect.x() + 4 - i : rect.x() + 2 + i;
            g.fill(column, y + 3 + i, column + 1, y + 10 - i, look.mark());
        }
    }

    /**
     * A chapter's tab at the binder's edge, its number in the middle: the shown chapter's reaches two pixels over the
     * page and one further out, its number bold; the one under the pointer a pixel over the page, lighter.
     */
    static void tab(final GuiGraphics g, final Font font, final GuideStyle style, final ManualScreenLayout.Rect rect,
                    final int number, final boolean on, final boolean pointed, final int colour, final int ink) {
        final int x = rect.x() - (on ? 2 : pointed ? 1 : 0);
        final int right = rect.x() + rect.width() + (on ? 1 : 0);
        final int fill = pointed && !on ? ManualButtonLook.lighten(colour, TAB_LIT) : colour;
        bevel(g, x, rect.y(), right - x, rect.height(), fill, ManualButtonLook.lighten(fill, TAB_LIGHT),
                ManualButtonLook.darken(fill, TAB_SHADE));
        final String written = String.valueOf(number);
        final TextSize size = on ? TextSize.SMALL_BOLD : TextSize.SMALL;
        final int width = GuideFonts.width(font, written, size, style);
        GuideFonts.draw(g, font, written, size, style, x + (right - x - width + 1) / 2,
                rect.y() + (rect.height() - 5) / 2, ink);
    }

    /** A bevelled plate: its face, lit along the top and left, shaded along the bottom and right. */
    static void bevel(final GuiGraphics g, final int x, final int y, final int width, final int height,
                      final int fill, final int light, final int dark) {
        g.fill(x, y, x + width, y + height, fill);
        g.fill(x, y, x + width, y + 1, light);
        g.fill(x, y, x + 1, y + height, light);
        g.fill(x, y + height - 1, x + width, y + height, dark);
        g.fill(x + width - 1, y, x + width, y + height, dark);
    }

    /** The buttons on the cover's top edge. */
    enum Kind {
        CONTENTS, BACK, FORWARD, SEARCH, BOOKMARKS, TEAR
    }
}
