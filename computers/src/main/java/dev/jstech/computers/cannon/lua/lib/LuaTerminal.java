/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.Arrays;

/**
 * The screen a Lua program draws on: ComputerCraft's 51 columns by 19 rows, a text colour and a
 * ground colour in every cell, and a cursor the program places itself.
 *
 * <p>Colours are the sixteen of ComputerCraft's palette, kept here by their index (0 for white to 15
 * for black) and handed to programs as the bit each one is ({@code colours.red} is 16384). Every
 * change moves the revision on, which is how whoever shows the screen knows there is something new.
 */
public final class LuaTerminal {

    public static final int WIDTH = 51;
    public static final int HEIGHT = 19;
    /** The index of white and of black in the palette. */
    public static final int WHITE = 0;
    public static final int BLACK = 15;

    /** ComputerCraft's default palette, in index order. */
    public static final int[] DEFAULT_PALETTE = {
        0xF0F0F0, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xDEDE6C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C,
        0x999999, 0x4C99B2, 0xB266E5, 0x3366CC, 0x7F664C, 0x57A64E, 0xCC4C4C, 0x111111
    };

    private static final String HEX = "0123456789abcdef";

    private final char[][] text = new char[HEIGHT][WIDTH];
    private final byte[][] fg = new byte[HEIGHT][WIDTH];
    private final byte[][] bg = new byte[HEIGHT][WIDTH];
    private final int[] palette = DEFAULT_PALETTE.clone();
    private int cursorX = 1;
    private int cursorY = 1;
    private boolean blink = true;
    private int textColour = WHITE;
    private int groundColour = BLACK;
    private long revision;

    public LuaTerminal() {
        for (int y = 0; y < HEIGHT; y++) {
            this.blank(y);
        }
    }

    private void blank(final int row) {
        Arrays.fill(this.text[row], ' ');
        Arrays.fill(this.fg[row], (byte) this.textColour);
        Arrays.fill(this.bg[row], (byte) this.groundColour);
    }

    private void changed() {
        this.revision++;
    }

    /** How many changes the screen has had, for whoever copies it elsewhere. */
    public long revision() {
        return this.revision;
    }

    /** Writes at the cursor without wrapping, and moves the cursor past what was written. */
    public void write(final String written) {
        final int y = this.cursorY - 1;
        if (y >= 0 && y < HEIGHT) {
            for (int i = 0; i < written.length(); i++) {
                final int x = this.cursorX - 1 + i;
                if (x >= 0 && x < WIDTH) {
                    this.text[y][x] = printable(written.charAt(i));
                    this.fg[y][x] = (byte) this.textColour;
                    this.bg[y][x] = (byte) this.groundColour;
                }
            }
        }
        this.cursorX += written.length();
        this.changed();
    }

    /** Writes text with a colour of its own for every character, given as hexadecimal digits. */
    public void blit(final String written, final String textColours, final String groundColours) {
        final int y = this.cursorY - 1;
        if (y >= 0 && y < HEIGHT) {
            for (int i = 0; i < written.length(); i++) {
                final int x = this.cursorX - 1 + i;
                if (x >= 0 && x < WIDTH) {
                    this.text[y][x] = printable(written.charAt(i));
                    this.fg[y][x] = (byte) digit(textColours.charAt(i));
                    this.bg[y][x] = (byte) digit(groundColours.charAt(i));
                }
            }
        }
        this.cursorX += written.length();
        this.changed();
    }

    private static char printable(final char c) {
        return c < 32 || c == 127 ? '?' : c;
    }

    /** The colour a hexadecimal digit names, or -1 for a character that is not one. */
    public static int digit(final char c) {
        return HEX.indexOf(Character.toLowerCase(c));
    }

    public void clear() {
        for (int y = 0; y < HEIGHT; y++) {
            this.blank(y);
        }
        this.changed();
    }

    public void clearLine() {
        if (this.cursorY >= 1 && this.cursorY <= HEIGHT) {
            this.blank(this.cursorY - 1);
            this.changed();
        }
    }

    /** Moves everything up by that many rows (down for a negative count), the rows it uncovers blank. */
    public void scroll(final int rows) {
        if (rows == 0) {
            return;
        }
        final char[][] oldText = new char[HEIGHT][];
        final byte[][] oldFg = new byte[HEIGHT][];
        final byte[][] oldBg = new byte[HEIGHT][];
        for (int y = 0; y < HEIGHT; y++) {
            oldText[y] = this.text[y].clone();
            oldFg[y] = this.fg[y].clone();
            oldBg[y] = this.bg[y].clone();
        }
        for (int y = 0; y < HEIGHT; y++) {
            final int from = y + rows;
            if (from >= 0 && from < HEIGHT) {
                this.text[y] = oldText[from];
                this.fg[y] = oldFg[from];
                this.bg[y] = oldBg[from];
            } else {
                this.text[y] = new char[WIDTH];
                this.fg[y] = new byte[WIDTH];
                this.bg[y] = new byte[WIDTH];
                this.blank(y);
            }
        }
        this.changed();
    }

    /**
     * Writes text the way ComputerCraft's {@code write} does: words wrap at the edge, a newline starts
     * a new row, and reaching the bottom scrolls. Gives back how many new rows it started.
     */
    public int print(final String written) {
        int lines = 0;
        String rest = written;
        while (!rest.isEmpty()) {
            int spaces = 0;
            while (spaces < rest.length() && (rest.charAt(spaces) == ' ' || rest.charAt(spaces) == '\t')) {
                spaces++;
            }
            if (spaces > 0) {
                this.write(rest.substring(0, spaces));
                rest = rest.substring(spaces);
            }
            if (!rest.isEmpty() && rest.charAt(0) == '\n') {
                this.newLine();
                lines++;
                rest = rest.substring(1);
            }
            int word = 0;
            while (word < rest.length() && " \t\n".indexOf(rest.charAt(word)) < 0) {
                word++;
            }
            if (word > 0) {
                String piece = rest.substring(0, word);
                rest = rest.substring(word);
                if (piece.length() > WIDTH) {
                    while (!piece.isEmpty()) {
                        if (this.cursorX > WIDTH) {
                            this.newLine();
                            lines++;
                        }
                        this.write(piece);
                        final int shown = Math.max(1, WIDTH - (this.cursorX - piece.length()) + 1);
                        piece = shown >= piece.length() ? "" : piece.substring(shown);
                    }
                } else {
                    if (this.cursorX + piece.length() - 1 > WIDTH) {
                        this.newLine();
                        lines++;
                    }
                    this.write(piece);
                }
            }
        }
        return lines;
    }

    private void newLine() {
        if (this.cursorY + 1 <= HEIGHT) {
            this.cursorX = 1;
            this.cursorY++;
        } else {
            this.cursorX = 1;
            this.cursorY = HEIGHT;
            this.scroll(1);
        }
        this.changed();
    }

    public int cursorX() {
        return this.cursorX;
    }

    public int cursorY() {
        return this.cursorY;
    }

    public void setCursor(final int x, final int y) {
        this.cursorX = x;
        this.cursorY = y;
        this.changed();
    }

    public boolean blink() {
        return this.blink;
    }

    public void setBlink(final boolean value) {
        this.blink = value;
        this.changed();
    }

    public int textColour() {
        return this.textColour;
    }

    public void setTextColour(final int index) {
        this.textColour = index & 15;
    }

    public int groundColour() {
        return this.groundColour;
    }

    public void setGroundColour(final int index) {
        this.groundColour = index & 15;
    }

    /** The colour of that index, as red, green and blue in one number. */
    public int paletteColour(final int index) {
        return this.palette[index & 15];
    }

    public void setPaletteColour(final int index, final int rgb) {
        this.palette[index & 15] = rgb & 0xFFFFFF;
        this.changed();
    }

    /** One row's text. */
    public String row(final int y) {
        return new String(this.text[y]);
    }

    /** One row's text colours, one hexadecimal digit a cell. */
    public String rowText(final int y) {
        return hex(this.fg[y]);
    }

    /** One row's ground colours, one hexadecimal digit a cell. */
    public String rowGround(final int y) {
        return hex(this.bg[y]);
    }

    private static String hex(final byte[] colours) {
        final StringBuilder out = new StringBuilder(colours.length);
        for (final byte colour : colours) {
            out.append(HEX.charAt(colour & 15));
        }
        return out.toString();
    }

    /** Puts a row back as it was written down: its text, its text colours and its grounds. */
    public void setRow(final int y, final String rowText, final String textColours, final String groundColours) {
        for (int x = 0; x < WIDTH; x++) {
            this.text[y][x] = x < rowText.length() ? rowText.charAt(x) : ' ';
            this.fg[y][x] = (byte) Math.max(0, x < textColours.length() ? digit(textColours.charAt(x)) : WHITE);
            this.bg[y][x] = (byte) Math.max(0, x < groundColours.length() ? digit(groundColours.charAt(x)) : BLACK);
        }
        this.changed();
    }

    /** The whole palette, as red, green and blue per colour. */
    public int[] palette() {
        return this.palette.clone();
    }
}
