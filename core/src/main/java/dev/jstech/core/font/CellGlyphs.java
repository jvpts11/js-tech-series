/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.util.ArrayList;
import java.util.List;

/**
 * The characters that fill their whole cell, the box lines (U+2500 to U+257F) and the blocks (U+2580 to U+259F), as
 * the rectangles that draw them in a cell of any size.
 *
 * <p>A frame or a bar made of these only reads as one when each piece meets the next: a line leaves its cell at the
 * same height the next one enters, and a full block touches the blocks on every side. A font's own glyphs for them
 * are drawn for its own cell and leave gaps when rows are spaced any other way, and the game's font draws them no
 * wider than its letters. So, as terminal programs do, the grid draws them itself from the cell's size: lines run
 * through the middle of the cell (a single line on the column left of centre and the row below it, as the classic
 * terminal fonts put them; a heavy line two pixels thick; a double line two single lines a pixel apart), and blocks
 * are cut from the cell in eighths, halves and quarters. The three shades are the whole cell in a quarter, a half and
 * three quarters of the colour.
 */
public final class CellGlyphs {

    /** The first box line. */
    private static final int BOX = 0x2500;
    /** The first block. */
    private static final int BLOCK = 0x2580;
    /** The last block. */
    private static final int LAST = 0x259F;

    private static final int NONE = 0;
    private static final int LIGHT = 1;
    private static final int HEAVY = 2;
    private static final int DOUBLE = 3;

    /*
     * Each box line by its four arms, up, right, down and left: 0 none, 1 light, 2 heavy, 3 double. A dash marks a
     * dashed line (2, 3 or 4 dashes), an arc a rounded corner, and x one of the three diagonals.
     */
    private static final String[] ARMS = {
        "0101", "0202", "1010", "2020", "0101-3", "0202-3", "1010-3", "2020-3",
        "0101-4", "0202-4", "1010-4", "2020-4", "0110", "0210", "0120", "0220",
        "0011", "0012", "0021", "0022", "1100", "1200", "2100", "2200",
        "1001", "1002", "2001", "2002", "1110", "1210", "2110", "1120",
        "2120", "2210", "1220", "2220", "1011", "1012", "2011", "1021",
        "2021", "2012", "1022", "2022", "0111", "0112", "0211", "0212",
        "0121", "0122", "0221", "0222", "1101", "1102", "1201", "1202",
        "2101", "2102", "2201", "2202", "1111", "1112", "1211", "1212",
        "2111", "1121", "2121", "2112", "2211", "1122", "1221", "2212",
        "1222", "2122", "2221", "2222", "0101-2", "0202-2", "1010-2", "2020-2",
        "0303", "3030", "0310", "0130", "0330", "0013", "0031", "0033",
        "1300", "3100", "3300", "1003", "3001", "3003", "1310", "3130",
        "3330", "1013", "3031", "3033", "0313", "0131", "0333", "1303",
        "3101", "3303", "1313", "3131", "3333", "0110a", "0011a", "1001a",
        "1100a", "x/", "x\\", "xx", "0001", "1000", "0100", "0010",
        "0002", "2000", "0200", "0020", "0201", "1020", "0102", "2010",
    };

    private CellGlyphs() {
    }

    /** Whether the grid draws that character itself, filling its cell, rather than with a font. */
    public static boolean isWholeCell(final int codePoint) {
        return codePoint >= BOX && codePoint <= LAST;
    }

    /**
     * The rectangles that draw that character in a cell that many pixels wide and tall; none for a character that is
     * not one of these, or for a cell too small to hold it.
     */
    public static List<CellRect> of(final int codePoint, final int width, final int height) {
        final List<CellRect> out = new ArrayList<>();
        if (!isWholeCell(codePoint) || width < 3 || height < 3) {
            return out;
        }
        if (codePoint >= BLOCK) {
            block(codePoint, width, height, out);
        } else {
            box(ARMS[codePoint - BOX], width, height, out);
        }
        return List.copyOf(out);
    }

    private static void block(final int codePoint, final int w, final int h, final List<CellRect> out) {
        final int halfW = w / 2;
        final int halfH = h / 2;
        switch (codePoint) {
            case 0x2580 -> add(out, 0, 0, w, halfH);
            case 0x2588 -> add(out, 0, 0, w, h);
            case 0x2590 -> add(out, halfW, 0, w - halfW, h);
            case 0x2591 -> out.add(new CellRect(0, 0, w, h, 64));
            case 0x2592 -> out.add(new CellRect(0, 0, w, h, 128));
            case 0x2593 -> out.add(new CellRect(0, 0, w, h, 191));
            case 0x2594 -> add(out, 0, 0, w, eighths(h, 1));
            case 0x2595 -> add(out, w - eighths(w, 1), 0, eighths(w, 1), h);
            default -> {
                if (codePoint < 0x2588) {
                    final int tall = eighths(h, codePoint - 0x2580);
                    add(out, 0, h - tall, w, tall);
                } else if (codePoint < 0x2590) {
                    add(out, 0, 0, eighths(w, 0x2590 - codePoint), h);
                } else {
                    quadrants(codePoint, w, h, halfW, halfH, out);
                }
            }
        }
    }

    /** U+2596 to U+259F, each a set of the cell's four quarters. */
    private static void quadrants(final int codePoint, final int w, final int h, final int halfW, final int halfH,
                                  final List<CellRect> out) {
        // Upper left, upper right, lower left, lower right, as bits 8, 4, 2 and 1.
        final int[] sets = {0b0010, 0b0001, 0b1000, 0b1011, 0b1001, 0b1110, 0b1101, 0b0100, 0b0110, 0b0111};
        final int set = sets[codePoint - 0x2596];
        if ((set & 0b1000) != 0) {
            add(out, 0, 0, halfW, halfH);
        }
        if ((set & 0b0100) != 0) {
            add(out, halfW, 0, w - halfW, halfH);
        }
        if ((set & 0b0010) != 0) {
            add(out, 0, halfH, halfW, h - halfH);
        }
        if ((set & 0b0001) != 0) {
            add(out, halfW, halfH, w - halfW, h - halfH);
        }
    }

    private static void box(final String arms, final int w, final int h, final List<CellRect> out) {
        final int cx = (w - 1) / 2;
        final int cy = h / 2;
        if (arms.charAt(0) == 'x') {
            diagonals(arms.charAt(1), w, h, out);
            return;
        }
        final int up = arms.charAt(0) - '0';
        final int right = arms.charAt(1) - '0';
        final int down = arms.charAt(2) - '0';
        final int left = arms.charAt(3) - '0';
        if (arms.length() > 4 && arms.charAt(4) == '-') {
            dashes(right != NONE, Math.max(right, up), arms.charAt(5) - '0', w, h, cx, cy, out);
        } else if (arms.length() > 4) {
            arc(up, right, down, left, w, h, cx, cy, out);
        } else {
            lines(up, right, down, left, w, h, cx, cy, out);
        }
    }

    private static void lines(final int up, final int right, final int down, final int left, final int w,
                              final int h, final int cx, final int cy, final List<CellRect> out) {
        final boolean doubleAcross = left == DOUBLE || right == DOUBLE;
        final boolean doubleDown = up == DOUBLE || down == DOUBLE;
        /*
         * A half line on its own leaves the centre to the other half of its axis, as the terminal fonts draw them:
         * the left and lower halves take it, so the halves put side by side make the whole line once.
         */
        final boolean alone = (up == NONE ? 0 : 1) + (right == NONE ? 0 : 1) + (down == NONE ? 0 : 1)
                + (left == NONE ? 0 : 1) == 1;
        if (right == DOUBLE) {
            final int upper = up == DOUBLE ? cx + 1 : single(up) ? cx : down == DOUBLE ? cx - 1 : cx;
            final int lower = down == DOUBLE ? cx + 1 : single(down) ? cx : up == DOUBLE ? cx - 1 : cx;
            add(out, upper, cy - 1, w - upper, 1);
            add(out, lower, cy + 1, w - lower, 1);
        } else if (single(right)) {
            final int from = alone ? cx + 1 : single(left) ? cx : up == DOUBLE && down == DOUBLE ? cx + 1
                    : doubleDown ? cx - 1 : cx;
            across(right, from, w, cy, out);
        }
        if (left == DOUBLE) {
            final int upper = up == DOUBLE ? cx : single(up) ? cx + 1 : down == DOUBLE ? cx + 2 : cx + 1;
            final int lower = down == DOUBLE ? cx : single(down) ? cx + 1 : up == DOUBLE ? cx + 2 : cx + 1;
            add(out, 0, cy - 1, upper, 1);
            add(out, 0, cy + 1, lower, 1);
        } else if (single(left)) {
            final int to = single(right) ? cx + 1 : up == DOUBLE && down == DOUBLE ? cx : doubleDown ? cx + 2 : cx + 1;
            across(left, 0, to, cy, out);
        }
        if (up == DOUBLE) {
            final int leftEnd = left == DOUBLE ? cy : single(left) ? cy + 1 : right == DOUBLE ? cy + 2 : cy + 1;
            final int rightEnd = right == DOUBLE ? cy : single(right) ? cy + 1 : left == DOUBLE ? cy + 2 : cy + 1;
            add(out, cx - 1, 0, 1, leftEnd);
            add(out, cx + 1, 0, 1, rightEnd);
        } else if (single(up)) {
            final int to = alone ? cy : single(down) ? cy + 1 : left == DOUBLE && right == DOUBLE ? cy
                    : doubleAcross ? cy + 2 : cy + 1;
            downward(up, 0, to, cx, out);
        }
        if (down == DOUBLE) {
            final int leftStart = left == DOUBLE ? cy + 1 : single(left) ? cy : right == DOUBLE ? cy - 1 : cy;
            final int rightStart = right == DOUBLE ? cy + 1 : single(right) ? cy : left == DOUBLE ? cy - 1 : cy;
            add(out, cx - 1, leftStart, 1, h - leftStart);
            add(out, cx + 1, rightStart, 1, h - rightStart);
        } else if (single(down)) {
            final int from = single(up) ? cy : left == DOUBLE && right == DOUBLE ? cy + 1 : doubleAcross ? cy - 1
                    : cy;
            downward(down, from, h, cx, out);
        }
    }

    /** A rounded corner: the two light arms stop a pixel short of the centre, which rounds the turn. */
    private static void arc(final int up, final int right, final int down, final int left, final int w, final int h,
                            final int cx, final int cy, final List<CellRect> out) {
        if (right != NONE) {
            add(out, cx + 1, cy, w - cx - 1, 1);
        }
        if (left != NONE) {
            add(out, 0, cy, cx, 1);
        }
        if (up != NONE) {
            add(out, cx, 0, 1, cy);
        }
        if (down != NONE) {
            add(out, cx, cy + 1, 1, h - cy - 1);
        }
    }

    /** A line broken into that many dashes along its length, each followed by its gap. */
    private static void dashes(final boolean across, final int weight, final int count, final int w, final int h,
                               final int cx, final int cy, final List<CellRect> out) {
        final int length = across ? w : h;
        final int gap = Math.max(1, length / count / 2);
        for (int i = 0; i < count; i++) {
            final int from = i * length / count;
            final int to = Math.max(from + 1, (i + 1) * length / count - gap);
            if (across) {
                across(weight, from, to, cy, out);
            } else {
                downward(weight, from, to, cx, out);
            }
        }
    }

    /** One pixel in every row, from corner to corner. */
    private static void diagonals(final char which, final int w, final int h, final List<CellRect> out) {
        for (int y = 0; y < h; y++) {
            final int x = y * w / h;
            if (which != '/') {
                add(out, x, y, 1, 1);
            }
            if (which != '\\') {
                add(out, w - 1 - x, y, 1, 1);
            }
        }
    }

    /** A horizontal arm from {@code from} up to {@code to}: one row thick when light, and the row above when heavy. */
    private static void across(final int weight, final int from, final int to, final int cy, final List<CellRect> out) {
        if (weight == HEAVY) {
            add(out, from, cy - 1, to - from, 2);
        } else {
            add(out, from, cy, to - from, 1);
        }
    }

    /** A vertical arm: one column thick when light, the column right of it too when heavy. */
    private static void downward(final int weight, final int from, final int to, final int cx,
                                 final List<CellRect> out) {
        add(out, cx, from, weight == HEAVY ? 2 : 1, to - from);
    }

    private static boolean single(final int weight) {
        return weight == LIGHT || weight == HEAVY;
    }

    /** That many eighths of a length, to the nearest pixel. */
    private static int eighths(final int length, final int eighths) {
        return (length * eighths + 4) / 8;
    }

    private static void add(final List<CellRect> out, final int x, final int y, final int width, final int height) {
        if (width > 0 && height > 0) {
            out.add(new CellRect(x, y, width, height, CellRect.SOLID));
        }
    }
}
