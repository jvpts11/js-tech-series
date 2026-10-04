/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import java.util.ArrayList;
import java.util.List;

/**
 * The grid the terminals are drawn on: the cells of the terminal font, Misc Fixed in its three sizes, and which size a
 * glass draws at what scale.
 *
 * <p>The terminal font is a bitmap font. Drawn at a scale where one of its pixels does not come out as a whole number
 * of the screen's, some of its columns come out a pixel wider than the others and every letter looks smeared. So a
 * glass picks, for the screen it is on, a size of the font and a whole number of the screen's pixels to each of the
 * font's: the pair that draws the widest cell and still fits its columns. On a screen where a pixel of the GUI is two
 * of the screen's, that is the 9x15 at one pixel each, as large as the game's small text; where it is three, the 6x10
 * at two each; where it is four, the 9x15 at two each. Only on a screen too small for even the smallest size at one
 * pixel each is the text drawn smaller than that, since cut-off columns would be worse than soft letters.
 *
 * <p>Pure: the screens hand it the size of their glass and the game's GUI scale, so its answers are tested without
 * the game.
 */
public final class TermGrid {

    /** How wide the small size's cell is, in its own pixels: the size a terminal window draws in. */
    public static final int CELL = 6;
    /** How tall the small size's row is: the font's own height, so box lines and blocks meet from row to row. */
    public static final int ROW = 10;
    /** The sizes of the terminal font, smallest first, as {@code jscore:fixed_6x10}, {@code 9x15} and {@code 10x20}. */
    public static final List<Cell> SIZES = List.of(new Cell(6, 10), new Cell(9, 15), new Cell(10, 20));

    private TermGrid() {
    }

    /**
     * The size and scale a glass that many GUI pixels across draws that many columns at: of every size at every whole
     * number of screen pixels to each of its own, the widest cell whose columns still fit, and of two as wide the
     * larger size, whose letters are drawn finer.
     *
     * @param glassWidth how wide the glass is, in GUI pixels
     * @param guiScale   how many screen pixels a GUI pixel is
     */
    public static Fit fit(final int glassWidth, final double guiScale, final int columns) {
        final double room = glassWidth * guiScale;
        Fit best = null;
        int widest = 0;
        for (int size = 0; size < SIZES.size(); size++) {
            final int across = Math.max(1, columns * SIZES.get(size).width());
            final int whole = (int) Math.floor(room / across);
            final int drawn = whole * SIZES.get(size).width();
            if (whole >= 1 && drawn >= widest) {
                widest = drawn;
                best = new Fit(size, (float) (whole / guiScale));
            }
        }
        return best != null ? best : new Fit(0, glassWidth / (float) Math.max(1, columns * CELL));
    }

    /** How many cells a line takes: one for each character. */
    public static int cells(final String line) {
        return line.codePointCount(0, line.length());
    }

    /** The first characters of a line, as many as that many cells hold. */
    public static String first(final String line, final int cells) {
        if (cells <= 0) {
            return "";
        }
        if (cells(line) <= cells) {
            return line;
        }
        return line.substring(0, line.offsetByCodePoints(0, cells));
    }

    /**
     * A line cut to that many cells, ending in three dots where it had to be cut, for a name that has to stay in its
     * column.
     */
    public static String clip(final String line, final int cells) {
        if (cells(line) <= cells) {
            return line;
        }
        return first(line, Math.max(1, cells - 3)) + "...";
    }

    /**
     * Words wrapped to lines of that many cells, breaking at spaces, and inside a word only when it is longer than a
     * whole line.
     */
    public static List<String> wrap(final String words, final int cells) {
        final List<String> out = new ArrayList<>();
        final int room = Math.max(1, cells);
        final StringBuilder line = new StringBuilder();
        for (final String whole : words.split(" ", -1)) {
            String word = whole;
            while (cells(word) > room) {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                final String piece = first(word, room);
                out.add(piece);
                word = word.substring(piece.length());
            }
            final int needed = line.isEmpty() ? cells(word) : cells(line.toString()) + 1 + cells(word);
            if (needed > room) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty() || out.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /** A size of the terminal font: its cell, in its own pixels. */
    public record Cell(int width, int height) {
    }

    /**
     * Which size of the terminal font a glass draws in, and at what scale against its own pixels.
     *
     * @param size  the size's place in {@link #SIZES}
     * @param scale how large the font is drawn: a whole number of screen pixels to each of its own
     */
    public record Fit(int size, float scale) {

        /** The cell of the size this is. */
        public Cell cell() {
            return SIZES.get(size);
        }
    }
}
