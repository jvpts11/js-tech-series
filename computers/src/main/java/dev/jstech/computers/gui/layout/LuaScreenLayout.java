/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

/**
 * Where a Lua program's screen is drawn in a terminal: at what size, where, and which of its rows.
 *
 * <p>The screen is ComputerCraft's, 51 cells by 19, each 6 pixels by 9 at full size. It is drawn at full
 * size, three quarters or half, and never stretched to anything in between, so the characters stay
 * whole. A terminal window takes the largest of those that fits; an editor's panel, which is short,
 * takes three quarters. When not every row fits, the rows around the cursor are the ones shown.
 */
public final class LuaScreenLayout {

    public static final int COLUMNS = 51;
    public static final int ROWS = 19;
    public static final int CELL_W = 6;
    public static final int CELL_H = 9;
    public static final int GRID_W = COLUMNS * CELL_W;
    public static final int GRID_H = ROWS * CELL_H;

    /** The dark margin round the screen. */
    public static final int PAD = 4;

    /** The sizes the screen is drawn at, largest first. */
    private static final float[] SCALES = {1f, 0.75f, 0.5f};

    /** The size an editor's panel draws the screen at. */
    public static final float PANEL_SCALE = 0.75f;

    private LuaScreenLayout() {
    }

    /**
     * The screen as placed: its size, its top left corner, and the run of rows showing.
     *
     * @param firstRow the first row showing, counted from 0
     * @param rows     how many rows show
     */
    public record Placement(float scale, int x, int y, int firstRow, int rows) {

        public float cellWidth() {
            return CELL_W * this.scale;
        }

        public float cellHeight() {
            return CELL_H * this.scale;
        }

        /** How wide the screen is drawn. */
        public int width() {
            return Math.round(GRID_W * this.scale);
        }

        /** How tall the rows showing are drawn. */
        public int height() {
            return Math.round(this.rows * CELL_H * this.scale);
        }

        /** Whether every row is showing. */
        public boolean whole() {
            return this.rows >= ROWS;
        }

        /**
         * The cell under a point, as ComputerCraft counts them (from 1, across then down), or null when
         * the point is not on the screen.
         */
        public int[] cellAt(final double mx, final double my) {
            if (mx < this.x || my < this.y || mx >= this.x + this.width() || my >= this.y + this.height()) {
                return null;
            }
            final int column = Math.min(COLUMNS, (int) ((mx - this.x) / this.cellWidth()) + 1);
            final int row = Math.min(ROWS, this.firstRow + (int) ((my - this.y) / this.cellHeight()) + 1);
            return new int[] {column, row};
        }
    }

    /** The room a window needs inside its frame to show the whole screen at full size. */
    public static int fullWidth() {
        return GRID_W + PAD * 2;
    }

    public static int fullHeight() {
        return GRID_H + PAD * 2;
    }

    /** The screen in a terminal window of that size: the largest size that shows it whole, centred. */
    public static Placement inWindow(final int x, final int y, final int width, final int height, final int cursorY) {
        float chosen = SCALES[SCALES.length - 1];
        for (final float scale : SCALES) {
            if (GRID_W * scale + PAD * 2 <= width && GRID_H * scale + PAD * 2 <= height) {
                chosen = scale;
                break;
            }
        }
        return place(x, y, width, height, chosen, cursorY);
    }

    /** The screen in an editor's panel: three quarters (half when the panel is too narrow for that). */
    public static Placement inPanel(final int x, final int y, final int width, final int height, final int cursorY) {
        final float scale = GRID_W * PANEL_SCALE + PAD * 2 <= width ? PANEL_SCALE : SCALES[SCALES.length - 1];
        return place(x, y, width, height, scale, cursorY);
    }

    private static Placement place(final int x, final int y, final int width, final int height, final float scale,
                                   final int cursorY) {
        final int rows = Math.max(1, Math.min(ROWS, (int) ((height - PAD * 2) / (CELL_H * scale))));
        final int gridW = Math.round(GRID_W * scale);
        final int gridH = Math.round(rows * CELL_H * scale);
        final int left = x + Math.max(PAD, (width - gridW) / 2);
        final int top = y + Math.max(PAD, (height - gridH) / 2);
        return new Placement(scale, left, top, firstRow(rows, cursorY), rows);
    }

    /** The first row showing when only so many fit: the cursor's row sits in the middle of them where it can. */
    public static int firstRow(final int rows, final int cursorY) {
        if (rows >= ROWS) {
            return 0;
        }
        final int cursorRow = Math.max(0, Math.min(ROWS - 1, cursorY - 1));
        return Math.max(0, Math.min(ROWS - rows, cursorRow - rows / 2));
    }
}
