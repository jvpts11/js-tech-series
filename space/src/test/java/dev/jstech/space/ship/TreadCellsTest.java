/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TreadCellsTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    public void of_aBoxWithinOneBlockIsThatBlocksOwn() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.2, 0.3, 0.4, 0.6, 0.5, 0.9}));

        assertEquals(1, cells.cells().size());
        assertArrayEquals(new int[] {0, 0, 0}, cells.cells().getFirst());
        assertArrayEquals(new double[] {0.2, 0.3, 0.4, 0.6, 0.5, 0.9}, cells.within(0, 0, 0).getFirst(), CLOSE);
    }

    @Test
    public void of_aBoxAcrossTwoBlocksIsCutAtTheirFace() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.5, 0.0, 0.0, 1.5, 0.2, 0.2}));

        assertEquals(2, cells.cells().size());
        assertArrayEquals(new double[] {0.5, 0.0, 0.0, 1.0, 0.2, 0.2}, cells.within(0, 0, 0).getFirst(), CLOSE);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 0.5, 0.2, 0.2}, cells.within(1, 0, 0).getFirst(), CLOSE);
    }

    @Test
    public void of_aBoxUnderNoughtStandsInTheBlockBelow() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {-0.5, -1.25, 0.0, -0.2, -1.0, 0.5}));

        assertArrayEquals(new int[] {-1, -2, 0}, cells.cells().getFirst());
        assertArrayEquals(new double[] {0.5, 0.75, 0.0, 0.8, 1.0, 0.5}, cells.within(-1, -2, 0).getFirst(), CLOSE);
    }

    @Test
    public void of_aBoxEndingOnABlocksFaceTakesNothingOfTheNext() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0}));

        assertEquals(1, cells.cells().size());
        assertTrue(cells.within(1, 0, 0).isEmpty());
    }

    @Test
    public void of_twoBoxesInOneBlockAreBothIt() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.1, 0.1, 0.1, 0.2, 0.2, 0.2},
                new double[] {0.5, 0.5, 0.5, 0.6, 0.6, 0.6}));

        assertEquals(1, cells.cells().size());
        assertEquals(2, cells.within(0, 0, 0).size());
    }

    @Test
    public void within_aBlockNoBoxStandsInHasNone() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.1, 0.1, 0.1, 0.2, 0.2, 0.2}));

        assertTrue(cells.within(3, 3, 3).isEmpty());
    }

    @Test
    public void within_givesCopiesTheCallerMayChange() {
        final TreadCells cells = TreadCells.of(List.of(new double[] {0.1, 0.1, 0.1, 0.2, 0.2, 0.2}));

        cells.within(0, 0, 0).getFirst()[0] = 9;

        assertEquals(0.1, cells.within(0, 0, 0).getFirst()[0], CLOSE);
    }
}
