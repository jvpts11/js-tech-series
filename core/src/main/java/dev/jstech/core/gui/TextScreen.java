/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import java.util.Arrays;

/**
 * A text-mode screen: a grid of cells, each a character with a colour of its own and a colour behind it, the way a
 * colour adapter of the first personal computers held its picture. A program that draws its whole screen this way
 * (a menu shell, a file manager of that age) writes into one of these, and the screen paints it a cell at a time.
 *
 * <p>Pure: no rendering here, so a test can read back what a program drew. Colours are ARGB, and {@link #cga(int)}
 * gives the adapter's sixteen in their own order. Writing past an edge is cut at the edge rather than refused, as the
 * adapter's memory simply ended there.
 */
public final class TextScreen {

    private final int columns;
    private final int rows;
    private final char[] glyphs;
    private final int[] inks;
    private final int[] grounds;

    /** The adapter's sixteen colours by index: black, the six dark ones, light grey, dark grey, the bright ones. */
    public static final int BLACK = 0;
    public static final int BLUE = 1;
    public static final int GREEN = 2;
    public static final int CYAN = 3;
    public static final int RED = 4;
    public static final int MAGENTA = 5;
    public static final int BROWN = 6;
    public static final int GREY = 7;
    public static final int DARK_GREY = 8;
    public static final int LIGHT_BLUE = 9;
    public static final int LIGHT_GREEN = 10;
    public static final int LIGHT_CYAN = 11;
    public static final int LIGHT_RED = 12;
    public static final int LIGHT_MAGENTA = 13;
    public static final int YELLOW = 14;
    public static final int WHITE = 15;

    /* The line-drawing characters of a box: corners, then the across and down strokes, single and double. */
    private static final String SINGLE = "┌┐└┘─│";
    private static final String DOUBLE = "╔╗╚╝═║";
    private static final int OPAQUE = 0xFF;

    private static final int[] SIXTEEN = sixteen();

    /** A screen of that many columns and rows, every cell a space of {@code ink} on {@code ground}. */
    public TextScreen(final int columns, final int rows, final int ink, final int ground) {
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        final int cells = this.columns * this.rows;
        this.glyphs = new char[cells];
        this.inks = new int[cells];
        this.grounds = new int[cells];
        Arrays.fill(this.glyphs, ' ');
        Arrays.fill(this.inks, ink);
        Arrays.fill(this.grounds, ground);
    }

    /** The adapter's colour of that index, 0 to 15, as opaque ARGB. */
    public static int cga(final int index) {
        return SIXTEEN[Math.floorMod(index, SIXTEEN.length)];
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** Writes {@code text} from the cell at ({@code column}, {@code row}) along the row, in that ink on that ground. */
    public TextScreen put(final int column, final int row, final String text, final int ink, final int ground) {
        if (row < 0 || row >= rows || text == null) {
            return this;
        }
        for (int i = 0; i < text.length(); i++) {
            final int c = column + i;
            if (c >= 0 && c < columns) {
                final int at = row * columns + c;
                glyphs[at] = text.charAt(i);
                inks[at] = ink;
                grounds[at] = ground;
            }
        }
        return this;
    }

    /** Writes {@code text} in that ink, keeping whatever ground each cell already has. */
    public TextScreen put(final int column, final int row, final String text, final int ink) {
        if (row < 0 || row >= rows || text == null) {
            return this;
        }
        for (int i = 0; i < text.length(); i++) {
            final int c = column + i;
            if (c >= 0 && c < columns) {
                final int at = row * columns + c;
                glyphs[at] = text.charAt(i);
                inks[at] = ink;
            }
        }
        return this;
    }

    /** Fills a block of cells with spaces of that ink on that ground. */
    public TextScreen fill(final int column, final int row, final int width, final int height, final int ink,
                           final int ground) {
        for (int r = Math.max(0, row); r < Math.min(rows, row + height); r++) {
            for (int c = Math.max(0, column); c < Math.min(columns, column + width); c++) {
                final int at = r * columns + c;
                glyphs[at] = ' ';
                inks[at] = ink;
                grounds[at] = ground;
            }
        }
        return this;
    }

    /** Draws a box of line characters round a block, its inside cleared to that ground, single or double ruled. */
    public TextScreen box(final int column, final int row, final int width, final int height, final int ink,
                          final int ground, final boolean doubled) {
        if (width < 2 || height < 2) {
            return this;
        }
        final String set = doubled ? DOUBLE : SINGLE;
        fill(column, row, width, height, ink, ground);
        final String across = String.valueOf(set.charAt(4)).repeat(width - 2);
        put(column, row, set.charAt(0) + across + set.charAt(1), ink, ground);
        put(column, row + height - 1, set.charAt(2) + across + set.charAt(3), ink, ground);
        for (int r = row + 1; r < row + height - 1; r++) {
            put(column, r, String.valueOf(set.charAt(5)), ink, ground);
            put(column + width - 1, r, String.valueOf(set.charAt(5)), ink, ground);
        }
        return this;
    }

    /**
     * The shadow a dialog of that age threw: two columns down its right side and a row under it, one cell further
     * along, the characters there still showing but dimmed to that ink on that ground.
     */
    public TextScreen shadow(final int column, final int row, final int width, final int height, final int ink,
                             final int ground) {
        for (int r = row + 1; r <= row + height; r++) {
            dim(column + width, r, ink, ground);
            dim(column + width + 1, r, ink, ground);
        }
        for (int c = column + 2; c < column + width; c++) {
            dim(c, row + height, ink, ground);
        }
        return this;
    }

    /** The character in that cell, or a space off the screen. */
    public char glyph(final int column, final int row) {
        return inside(column, row) ? glyphs[row * columns + column] : ' ';
    }

    /** The ink of that cell, or zero off the screen. */
    public int ink(final int column, final int row) {
        return inside(column, row) ? inks[row * columns + column] : 0;
    }

    /** The ground of that cell, or zero off the screen. */
    public int ground(final int column, final int row) {
        return inside(column, row) ? grounds[row * columns + column] : 0;
    }

    /** What a row says, colours aside, trailing spaces kept. */
    public String text(final int row) {
        if (row < 0 || row >= rows) {
            return "";
        }
        return new String(glyphs, row * columns, columns);
    }

    /** Every row's words, one row after another, for a test or a screen that reads the picture back. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            out.append(text(r).stripTrailing()).append('\n');
        }
        return out.toString();
    }

    private void dim(final int column, final int row, final int ink, final int ground) {
        if (inside(column, row)) {
            final int at = row * columns + column;
            inks[at] = ink;
            grounds[at] = ground;
        }
    }

    private boolean inside(final int column, final int row) {
        return column >= 0 && column < columns && row >= 0 && row < rows;
    }

    private static int[] sixteen() {
        final int[] rgb = Tube.sixteenColours();
        final int[] argb = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            argb[i] = OPAQUE << 24 | rgb[i];
        }
        return argb;
    }
}
