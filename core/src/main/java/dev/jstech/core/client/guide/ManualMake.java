/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How an open book is made, drawn round and over its pages: the paper they are, the binding in the middle, the marks
 * of its chapters at the edge, and its ribbon. A ring binder's rings and plastic tabs are the screen's own; these are
 * every other book's.
 */
final class ManualMake {

    /** A sheet of graph paper's squares, and where the line down its outer margin stands from the page's edge. */
    private static final int SQUARE = 5;
    private static final int SQUARE_START = 2;
    private static final int MARGIN_LINE = 7;
    /** How deep the paper's shade runs into the spine: glued, sewn into signatures, or on a wire coil. */
    private static final int GLUED_SHADE = 5;
    private static final int SEWN_SHADE = 9;
    private static final int WIRE_SHADE = 4;
    /** A wire coil: how far apart its loops are, from the top of the pages to their foot. */
    private static final int COIL_PITCH = 6;
    private static final int COIL_TOP = 6;
    /** How dark a coil's punched holes are against the paper. */
    private static final double HOLE = 0.55;
    /** A ribbon: how wide, how far into the right page, and how far down it hangs. */
    private static final int RIBBON_WIDTH = 4;
    private static final int RIBBON_IN = 2;
    private static final int RIBBON_LENGTH = 150;
    /** How far a ribbon's tail hangs out under a book open elsewhere. */
    private static final int RIBBON_TAIL = 6;
    /** How far in from the board's edge its covering shows, inside the bevel. */
    private static final int BOARD_INSET = 2;
    /** A cloth's weave: how far apart its darker rows are, and how much darker. */
    private static final int WEAVE = 2;
    private static final double WEAVE_SHADE = 0.1;
    /** Kraft card's flecks: how dark, and what they are scattered by. */
    private static final double[] FLECK_SHADES = {0.1, 0.18, 0.25};
    private static final long FLECK_SEED = 3L;
    /** How many flecks to a thousand pixels round the open pages, few, so the margins stay quiet beside the text. */
    private static final int BOARD_FLECKS = 7;

    private ManualMake() {
    }

    /** The covering of the open book's boards round its pages: a cloth's weave or kraft card's flecks, or nothing. */
    static void board(final GuiGraphics g, final ManualScreenLayout.Geometry geometry, final GuideStyle style,
                      final ToIntFunction<String> colour) {
        final int width = geometry.coverRight() - 2 * BOARD_INSET;
        final int tint = colour.applyAsInt(GuideStyle.COVER);
        switch (style.make().board()) {
            case CLOTH -> cloth(g, BOARD_INSET, BOARD_INSET - 1, width, geometry.height() - BOARD_INSET, tint);
            case KRAFT -> kraft(g, BOARD_INSET, BOARD_INSET, width, geometry.height() - 2 * BOARD_INSET, tint,
                    BOARD_FLECKS);
            case PLAIN -> {
            }
        }
    }

    /** A cloth's weave over a rectangle: a row a little darker than the cloth every other row. */
    static void cloth(final GuiGraphics g, final int x, final int y, final int width, final int height,
                      final int tint) {
        final int weave = ManualButtonLook.darken(tint, WEAVE_SHADE);
        for (int row = y + 1; row < y + height - 1; row += WEAVE) {
            g.fill(x, row, x + width, row + 1, weave);
        }
    }

    /** Kraft card's flecks over a rectangle, so many to a thousand pixels, each a shade darker than the card. */
    static void kraft(final GuiGraphics g, final int x, final int y, final int width, final int height,
                      final int tint, final int perThousand) {
        // Scattered the same way every time the book is shown, so the card does not shimmer.
        final Random flecks = new Random(FLECK_SEED);
        final int count = width * height * perThousand / 1000;
        for (int i = 0; i < count; i++) {
            final int fx = x + flecks.nextInt(Math.max(1, width));
            final int fy = y + flecks.nextInt(Math.max(1, height));
            g.fill(fx, fy, fx + 1, fy + 1, ManualButtonLook.darken(tint,
                    FLECK_SHADES[flecks.nextInt(FLECK_SHADES.length)]));
        }
    }

    /** The paper of a page: plain, or graph paper squared in the grid's colour, a line down its outer margin. */
    static void paper(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuideStyle style,
                      final ToIntFunction<String> colour, final boolean leftPage) {
        if (style.make().paper() != GuideStyle.Paper.GRAPH) {
            return;
        }
        final int grid = colour.applyAsInt(GuideStyle.GRID);
        for (int x = page.x() + SQUARE_START; x < page.x() + page.width(); x += SQUARE) {
            g.fill(x, page.y(), x + 1, page.y() + page.height(), grid);
        }
        for (int y = page.y() + SQUARE_START; y < page.y() + page.height(); y += SQUARE) {
            g.fill(page.x(), y, page.x() + page.width(), y + 1, grid);
        }
        final int margin = leftPage ? page.x() + MARGIN_LINE : page.x() + page.width() - MARGIN_LINE - 1;
        g.fill(margin, page.y(), margin + 1, page.y() + page.height(), colour.applyAsInt(GuideStyle.MARGIN));
    }

    /**
     * The middle of an open book that has no rings: the paper shading into the spine, softly where it is glued and
     * deeply where it is sewn, and a wire coil through punched holes where it is bound on one.
     */
    static void binding(final GuiGraphics g, final ManualScreenLayout.Geometry geometry, final GuideStyle style,
                        final ToIntFunction<String> colour) {
        final GuideStyle.Binding binding = style.make().binding();
        if (binding == GuideStyle.Binding.RINGS || geometry.right() == null) {
            return;
        }
        final ManualScreenLayout.Rect left = geometry.left();
        final ManualScreenLayout.Rect right = geometry.right();
        final int leftEdge = left.x() + left.width();
        final int paper = colour.applyAsInt(GuideStyle.PAPER);
        final int gutter = colour.applyAsInt(GuideStyle.GUTTER);
        final int depth = switch (binding) {
            case SEWN -> SEWN_SHADE;
            case WIRE -> WIRE_SHADE;
            default -> GLUED_SHADE;
        };
        for (int i = 0; i < depth; i++) {
            final int shade = ManualButtonLook.mix(paper, gutter, (depth - i) / (double) depth);
            g.fill(leftEdge - 1 - i, left.y(), leftEdge - i, left.y() + left.height(), shade);
            g.fill(right.x() + i, right.y(), right.x() + i + 1, right.y() + right.height(), shade);
        }
        if (binding != GuideStyle.Binding.WIRE) {
            return;
        }
        final int wire = colour.applyAsInt(GuideStyle.RING);
        final int hole = ManualButtonLook.darken(paper, HOLE);
        for (int y = left.y() + COIL_TOP; y < left.y() + left.height() - COIL_TOP; y += COIL_PITCH) {
            g.fill(leftEdge - 6, y, leftEdge - 3, y + 3, hole);
            g.fill(right.x() + 3, y, right.x() + 6, y + 3, hole);
            g.fill(leftEdge - 5, y + 1, right.x() + 4, y + 3, wire);
            g.fill(leftEdge - 5, y + 1, right.x() + 4, y + 2, ManualButtonLook.lit(wire));
            g.fill(leftEdge - 5, y + 3, right.x() + 4, y + 4, ManualButtonLook.shaded(wire));
        }
    }

    /**
     * A chapter's mark at the edge in its colour: a block of a thumb index printed on the page, or a paper flag stuck
     * on the edge. Its number is written in whichever of the paper and the ink reads better on it.
     */
    static void mark(final GuiGraphics g, final Font font, final GuideStyle style, final ManualScreenLayout.Rect rect,
                     final int number, final boolean open, final boolean pointed, final int markColour,
                     final ToIntFunction<String> colour) {
        final int ink = colour.applyAsInt(GuideStyle.INK);
        final int fill = pointed && !open ? ManualButtonLook.lit(markColour) : markColour;
        g.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
        if (style.make().marks() == GuideStyle.Marks.FLAGS) {
            g.fill(rect.x(), rect.y() + rect.height() - 1, rect.x() + rect.width(), rect.y() + rect.height(),
                    ManualButtonLook.shaded(fill));
        }
        final String written = String.valueOf(number);
        final TextSize size = open ? TextSize.SMALL_BOLD : TextSize.SMALL;
        final int width = GuideFonts.width(font, written, size, style);
        final int x = style.make().marks() == GuideStyle.Marks.FLAGS
                ? rect.x() + (rect.width() - width) / 2 + 2 : rect.x() + (rect.width() - width + 1) / 2;
        GuideFonts.draw(g, font, written, size, style, x, rect.y() + (rect.height() - 5) / 2,
                ManualButtonLook.markFor(fill, ink, colour.applyAsInt(GuideStyle.PAPER)));
    }

    /**
     * A hardcover's ribbon: down the right page's spine edge, clear of the text, on the spread it keeps; on any other
     * its tail hanging out under the book, so the reader sees there is a place kept.
     */
    static void ribbon(final GuiGraphics g, final ManualScreenLayout.Geometry geometry, final GuideStyle style,
                       final ToIntFunction<String> colour, final boolean kept) {
        if (!style.make().ribbon() || geometry.right() == null) {
            return;
        }
        final int ribbon = colour.applyAsInt(GuideStyle.RIBBON);
        final int x = geometry.right().x() + RIBBON_IN;
        if (!kept) {
            g.fill(x, geometry.height(), x + RIBBON_WIDTH, geometry.height() + RIBBON_TAIL, ribbon);
            return;
        }
        g.fill(x, 0, x + RIBBON_WIDTH, RIBBON_LENGTH, ribbon);
        g.fill(x, 0, x + 1, RIBBON_LENGTH, ManualButtonLook.lit(ribbon));
        g.fill(x + RIBBON_WIDTH - 1, 0, x + RIBBON_WIDTH, RIBBON_LENGTH, ManualButtonLook.shaded(ribbon));
        // Its cut end, notched in the middle.
        g.fill(x, RIBBON_LENGTH, x + 1, RIBBON_LENGTH + 3, ribbon);
        g.fill(x + RIBBON_WIDTH - 1, RIBBON_LENGTH, x + RIBBON_WIDTH, RIBBON_LENGTH + 3, ribbon);
        g.fill(x + 1, RIBBON_LENGTH, x + RIBBON_WIDTH - 1, RIBBON_LENGTH + 1, ribbon);
    }
}
