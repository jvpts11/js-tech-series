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
 * The grid the terminals are drawn on: the cells the game's font is laid on, and the scale a glass draws them at.
 *
 * <p>The terminals write in the game's own font, one character to a cell six of its pixels wide, which is what nearly
 * every letter of it takes, and ten tall, which leaves a pixel between its lines. A glass draws it at the machine's
 * display scale, the same a desktop on it is drawn at (three quarters of the game's own text unless the machine is set
 * otherwise), or smaller where its columns would not fit at that.
 *
 * <p>It is a bitmap font: drawn at a scale where one of its pixels does not come out as a whole number of the
 * screen's, some of its columns come out a pixel wider than the others. So the glass draws at the whole number of
 * screen pixels to each of the font's nearest the machine's scale, where that still fits the columns, and the letters
 * are crisp; where it does not fit, it keeps the scale and lets the letters be a little uneven, since text far smaller
 * than the desktop's is harder to read than an uneven letter.
 *
 * <p>Pure: the screens hand it the size of their glass and the game's GUI scale, so its answers are tested without
 * the game.
 */
public final class TermGrid {

    /** How wide a cell is, in the font's own pixels: the size a terminal window draws in. */
    public static final int CELL = 6;
    /** How tall a row is: a line of the game's font and a pixel under it; box lines and blocks fill all of it. */
    public static final int ROW = 10;
    /** The cells the terminals draw in, smallest first: the game's font on cells six wide and ten tall. */
    public static final List<Cell> SIZES = List.of(new Cell(CELL, ROW));
    /**
     * The display scale a machine draws at when it has not been set otherwise, in percent of the game's own size: the
     * size a desktop is drawn at.
     */
    public static final int DEFAULT_SCALE = 75;

    private TermGrid() {
    }

    /** A machine's display scale as a factor: its setting in percent, the default where it has none. */
    public static float scaleOf(final int percent) {
        return (percent <= 0 ? DEFAULT_SCALE : percent) / 100.0F;
    }

    /**
     * The scale a glass that many GUI pixels across draws that many columns at: the whole number of screen pixels to
     * each of the font's nearest the machine's display scale, where that fits the columns; else the display scale, or
     * less where even that would not fit them.
     *
     * @param glassWidth how wide the glass is, in GUI pixels
     * @param guiScale   how many screen pixels a GUI pixel is
     * @param scale      the machine's display scale, as a factor ({@link #scaleOf})
     */
    public static Fit fit(final int glassWidth, final double guiScale, final int columns, final float scale) {
        final int across = Math.max(1, columns * CELL);
        final float size = Math.min(scale, glassWidth / (float) across);
        final long whole = Math.round(size * guiScale);
        final float crisp = (float) (whole / guiScale);
        return new Fit(0, whole >= 1 && crisp * across <= glassWidth + 1.0e-4F ? crisp : size);
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
     * @param scale how large the font is drawn against its own pixels, in GUI pixels to each of them
     */
    public record Fit(int size, float scale) {

        /** The cell of the size this is. */
        public Cell cell() {
            return SIZES.get(size);
        }
    }
}
