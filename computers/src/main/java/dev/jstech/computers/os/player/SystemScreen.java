/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The monitor's text screen as an operating system a player wrote draws on it with {@code Screen}
 * (computers/docs/design/player-systems.md): a grid of characters, each with the colour it was written in and the
 * colour behind it, and a cursor where the next one goes.
 *
 * <p>Pure, and carrying no Minecraft dependency: the machine keeps one per running system and sends its rows to whoever
 * watches.
 */
public final class SystemScreen {

    private final char[][] cells = new char[ROWS][COLUMNS];
    private final int[][] fore = new int[ROWS][COLUMNS];
    private final int[][] back = new int[ROWS][COLUMNS];
    private int column;
    private int row;
    private int currentFore = WHITE;
    private int currentBack = BLACK;

    /** The columns and rows of the monitor's text mode. */
    public static final int COLUMNS = 80;
    public static final int ROWS = 25;

    /** The colours a system names, as the old text screens' sixteen: "lightgreen", or one written "#RRGGBB". */
    public static final Map<String, Integer> NAMED = Map.ofEntries(
            Map.entry("black", 0x000000), Map.entry("blue", 0x0000AA), Map.entry("green", 0x00AA00),
            Map.entry("cyan", 0x00AAAA), Map.entry("red", 0xAA0000), Map.entry("magenta", 0xAA00AA),
            Map.entry("brown", 0xAA5500), Map.entry("lightgray", 0xAAAAAA), Map.entry("darkgray", 0x555555),
            Map.entry("lightblue", 0x5555FF), Map.entry("lightgreen", 0x55FF55), Map.entry("lightcyan", 0x55FFFF),
            Map.entry("lightred", 0xFF5555), Map.entry("lightmagenta", 0xFF55FF), Map.entry("yellow", 0xFFFF55),
            Map.entry("white", 0xFFFFFF));

    private static final int WHITE = 0xAAAAAA;
    private static final int BLACK = 0x000000;

    public SystemScreen() {
        this.clear();
    }

    /** The colour a system names, as 0xRRGGBB, or -1 for a name or a colour that is neither. */
    public static int colour(final String named) {
        final String written = named == null ? "" : named.trim().toLowerCase(Locale.ROOT);
        if (written.matches("#[0-9a-f]{6}")) {
            return Integer.parseInt(written.substring(1), 16);
        }
        return NAMED.getOrDefault(written, -1);
    }

    /** Writes text where the cursor is, in the colours chosen, going on to the next row at the edge. */
    public void write(final String text) {
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\n') {
                this.newLine();
                continue;
            }
            if (this.column >= COLUMNS) {
                this.newLine();
            }
            this.cells[this.row][this.column] = c;
            this.fore[this.row][this.column] = this.currentFore;
            this.back[this.row][this.column] = this.currentBack;
            this.column++;
        }
    }

    /** Writes text and goes to the start of the next row. */
    public void writeLine(final String text) {
        this.write(text);
        this.newLine();
    }

    /** Empties the screen in the colour behind the text, and puts the cursor in the top left corner. */
    public void clear() {
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(this.cells[r], ' ');
            Arrays.fill(this.fore[r], this.currentFore);
            Arrays.fill(this.back[r], this.currentBack);
        }
        this.column = 0;
        this.row = 0;
    }

    /** Puts the cursor at that column and row, held to the screen. */
    public void cursor(final int x, final int y) {
        this.column = Math.max(0, Math.min(COLUMNS - 1, x));
        this.row = Math.max(0, Math.min(ROWS - 1, y));
    }

    /** Chooses the colours what is written next is in; false, choosing nothing, when either is not a colour. */
    public boolean colours(final String foreground, final String background) {
        final int f = colour(foreground);
        final int b = colour(background);
        if (f < 0 || b < 0) {
            return false;
        }
        this.currentFore = f;
        this.currentBack = b;
        return true;
    }

    /** Each row as its text. */
    public List<String> rows() {
        final List<String> out = new ArrayList<>(ROWS);
        for (final char[] cellsOfRow : this.cells) {
            out.add(new String(cellsOfRow));
        }
        return out;
    }

    /** The colour of each character, row after row, as 0xRRGGBB. */
    public int[] foregrounds() {
        return flat(this.fore);
    }

    /** The colour behind each character, row after row, as 0xRRGGBB. */
    public int[] backgrounds() {
        return flat(this.back);
    }

    /** Where the cursor is: its column and its row. */
    public int[] cursorAt() {
        return new int[]{this.column, this.row};
    }

    /** Puts a screen back as it was saved, every row and both colours of every character. */
    public void restore(final List<String> rows, final int[] foregrounds, final int[] backgrounds,
                        final int[] cursor) {
        for (int r = 0; r < ROWS; r++) {
            final String text = r < rows.size() ? rows.get(r) : "";
            for (int c = 0; c < COLUMNS; c++) {
                final int at = r * COLUMNS + c;
                this.cells[r][c] = c < text.length() ? text.charAt(c) : ' ';
                this.fore[r][c] = at < foregrounds.length ? foregrounds[at] : this.currentFore;
                this.back[r][c] = at < backgrounds.length ? backgrounds[at] : this.currentBack;
            }
        }
        if (cursor.length == 2) {
            this.cursor(cursor[0], cursor[1]);
        }
    }

    /* The cursor to the start of the next row; past the last, every row moves up one. */
    private void newLine() {
        this.column = 0;
        if (this.row < ROWS - 1) {
            this.row++;
            return;
        }
        for (int r = 1; r < ROWS; r++) {
            this.cells[r - 1] = this.cells[r];
            this.fore[r - 1] = this.fore[r];
            this.back[r - 1] = this.back[r];
        }
        this.cells[ROWS - 1] = new char[COLUMNS];
        this.fore[ROWS - 1] = new int[COLUMNS];
        this.back[ROWS - 1] = new int[COLUMNS];
        Arrays.fill(this.cells[ROWS - 1], ' ');
        Arrays.fill(this.fore[ROWS - 1], this.currentFore);
        Arrays.fill(this.back[ROWS - 1], this.currentBack);
    }

    private static int[] flat(final int[][] grid) {
        final int[] out = new int[ROWS * COLUMNS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(grid[r], 0, out, r * COLUMNS, COLUMNS);
        }
        return out;
    }
}
