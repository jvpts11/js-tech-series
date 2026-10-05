/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the tiles of Frames 10's Start menu stand, in the cells of its grid: a small tile is one cell, a medium one two
 * by two, a wide one four by two, and the grid runs {@link #COLUMNS} cells across, four medium tiles.
 *
 * <p>Each tile takes the first place it fits, row by row, in the order the player arranged them. Small tiles go four
 * to the room of a medium one, as Frames 10 packed them: the first small tile takes such a room and the three after it
 * fill the rest of it, left to right and then the row under, before another room is opened.
 */
public final class TileGrid {

    /** How many cells the grid runs across. */
    public static final int COLUMNS = 8;

    private TileGrid() {
    }

    /**
     * Places tiles of these sizes, each {@code {across, down}} in cells, and gives back where each one stands, in the
     * same order.
     */
    public static List<Cell> place(final List<int[]> sizes) {
        final List<boolean[]> taken = new ArrayList<>();
        final List<Cell> out = new ArrayList<>(sizes.size());
        int roomCol = -1;
        int roomRow = -1;
        int roomUsed = 4;
        for (final int[] size : sizes) {
            final int across = Math.max(1, Math.min(COLUMNS, size[0]));
            final int down = Math.max(1, size[1]);
            if (across == 1 && down == 1) {
                if (roomUsed >= 4) {
                    final int[] at = firstFit(taken, 2, 2);
                    occupy(taken, at[0], at[1], 2, 2);
                    roomCol = at[0];
                    roomRow = at[1];
                    roomUsed = 0;
                }
                out.add(new Cell(roomCol + roomUsed % 2, roomRow + roomUsed / 2, 1, 1));
                roomUsed++;
                continue;
            }
            final int[] at = firstFit(taken, across, down);
            occupy(taken, at[0], at[1], across, down);
            out.add(new Cell(at[0], at[1], across, down));
        }
        return out;
    }

    /** How many rows of cells those places run down. */
    public static int rows(final List<Cell> cells) {
        int rows = 0;
        for (final Cell cell : cells) {
            rows = Math.max(rows, cell.row() + cell.down());
        }
        return rows;
    }

    /** The first place, row by row, where a tile of that size fits. */
    private static int[] firstFit(final List<boolean[]> taken, final int across, final int down) {
        for (int row = 0; ; row++) {
            for (int col = 0; col + across <= COLUMNS; col++) {
                if (free(taken, col, row, across, down)) {
                    return new int[] {col, row};
                }
            }
        }
    }

    private static boolean free(final List<boolean[]> taken, final int col, final int row, final int across,
                                final int down) {
        for (int r = row; r < row + down; r++) {
            if (r >= taken.size()) {
                continue;
            }
            for (int c = col; c < col + across; c++) {
                if (taken.get(r)[c]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void occupy(final List<boolean[]> taken, final int col, final int row, final int across,
                               final int down) {
        while (taken.size() < row + down) {
            taken.add(new boolean[COLUMNS]);
        }
        for (int r = row; r < row + down; r++) {
            for (int c = col; c < col + across; c++) {
                taken.get(r)[c] = true;
            }
        }
    }

    /** Where one tile stands: its first cell's column and row, and how many cells it takes across and down. */
    public record Cell(int col, int row, int across, int down) {
    }
}
