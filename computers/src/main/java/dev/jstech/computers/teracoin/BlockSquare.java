/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

/**
 * An address drawn as a square of blocks, to show across a room (computers/docs/design/teracoin.md): twenty-one by
 * twenty-one, a three-ringed mark in three corners so the square is read the right way up, and the rest drawn from
 * the address, so the same address always makes the same square and another address another. A pattern of the mod's
 * own; nothing reads it but a person. Pure.
 */
public final class BlockSquare {

    /** The blocks along a side. */
    public static final int SIDE = 21;
    /* The corner marks' side, and the clear row round each. */
    private static final int MARK = 7;

    private BlockSquare() {
    }

    /** The square of {@code address}, row by row: true for a dark block. */
    public static boolean[][] of(final String address) {
        final boolean[][] square = new boolean[SIDE][SIDE];
        long state = seed(address);
        for (int row = 0; row < SIDE; row++) {
            for (int column = 0; column < SIDE; column++) {
                state = next(state);
                square[row][column] = (state >>> 33 & 1L) == 1L;
            }
        }
        mark(square, 0, 0);
        mark(square, 0, SIDE - MARK);
        mark(square, SIDE - MARK, 0);
        return square;
    }

    /* A corner mark: a dark ring, a light ring, a dark middle of three, and a light row outside it. */
    private static void mark(final boolean[][] square, final int top, final int left) {
        for (int row = -1; row <= MARK; row++) {
            for (int column = -1; column <= MARK; column++) {
                final int r = top + row;
                final int c = left + column;
                if (r < 0 || c < 0 || r >= SIDE || c >= SIDE) {
                    continue;
                }
                final int ring = Math.min(Math.min(row, column), Math.min(MARK - 1 - row, MARK - 1 - column));
                square[r][c] = ring == 0 || ring >= 2;
            }
        }
    }

    private static long seed(final String address) {
        long hash = 1_125_899_906_842_597L;
        for (int i = 0; i < address.length(); i++) {
            hash = 31L * hash + address.charAt(i);
        }
        return hash;
    }

    private static long next(final long state) {
        return state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
    }
}
