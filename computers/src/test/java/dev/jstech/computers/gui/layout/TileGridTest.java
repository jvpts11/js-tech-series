/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileGridTest {

    @Test
    void place_putsMediumTilesFourAcrossThenStartsANewRow() {
        final List<TileGrid.Cell> cells = TileGrid.place(List.of(medium(), medium(), medium(), medium(), medium()));
        for (int i = 0; i < 4; i++) {
            assertEquals(new TileGrid.Cell(i * 2, 0, 2, 2), cells.get(i));
        }
        assertEquals(new TileGrid.Cell(0, 2, 2, 2), cells.get(4));
    }

    @Test
    void place_packsFourSmallTilesIntoTheRoomOfOneMedium() {
        final List<TileGrid.Cell> cells = TileGrid.place(List.of(small(), small(), small(), small(), small()));
        assertEquals(new TileGrid.Cell(0, 0, 1, 1), cells.get(0));
        assertEquals(new TileGrid.Cell(1, 0, 1, 1), cells.get(1));
        assertEquals(new TileGrid.Cell(0, 1, 1, 1), cells.get(2));
        assertEquals(new TileGrid.Cell(1, 1, 1, 1), cells.get(3));
        assertEquals(new TileGrid.Cell(2, 0, 1, 1), cells.get(4), "the fifth opens the next room");
    }

    @Test
    void place_letsAWideTileTakeTheFirstGapItFits() {
        final List<TileGrid.Cell> cells = TileGrid.place(List.of(medium(), medium(), medium(), wide()));
        assertEquals(new TileGrid.Cell(0, 2, 4, 2), cells.get(3), "two cells are left on the first row, too few");
        final List<TileGrid.Cell> fits = TileGrid.place(List.of(medium(), wide()));
        assertEquals(new TileGrid.Cell(2, 0, 4, 2), fits.get(1));
    }

    @Test
    void place_neverLetsTwoTilesShareACell() {
        final List<int[]> sizes = new ArrayList<>();
        final int[][] cycle = {small(), wide(), medium(), small(), small(), medium(), wide(), small(), medium()};
        for (int i = 0; i < 24; i++) {
            sizes.add(cycle[i % cycle.length]);
        }
        final List<TileGrid.Cell> cells = TileGrid.place(sizes);
        final boolean[][] taken = new boolean[TileGrid.rows(cells)][TileGrid.COLUMNS];
        for (final TileGrid.Cell cell : cells) {
            assertTrue(cell.col() >= 0 && cell.col() + cell.across() <= TileGrid.COLUMNS, cell.toString());
            for (int r = cell.row(); r < cell.row() + cell.down(); r++) {
                for (int c = cell.col(); c < cell.col() + cell.across(); c++) {
                    assertFalse(taken[r][c], "two tiles at " + c + "," + r);
                    taken[r][c] = true;
                }
            }
        }
    }

    @Test
    void place_clampsATileWiderThanTheGrid() {
        final List<TileGrid.Cell> cells = TileGrid.place(List.of(new int[] {12, 2}));
        assertEquals(TileGrid.COLUMNS, cells.get(0).across());
    }

    @Test
    void rows_countsDownToTheLowestTile() {
        assertEquals(0, TileGrid.rows(List.of()));
        assertEquals(4, TileGrid.rows(TileGrid.place(List.of(medium(), medium(), medium(), wide()))));
    }

    private static int[] small() {
        return new int[] {1, 1};
    }

    private static int[] medium() {
        return new int[] {2, 2};
    }

    private static int[] wide() {
        return new int[] {4, 2};
    }
}
