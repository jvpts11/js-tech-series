/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.multiblock.Orientation;
import org.junit.jupiter.api.Test;

class CellSpaceTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void boxes_turnWithTheirStructure() {
        final int[] westSlab = {0, 0, 0, 4, 16, 16};

        assertArrayEquals(westSlab, CellSpace.boxes(westSlab, Orientation.NORTH));
        assertArrayEquals(new int[] {0, 0, 0, 16, 16, 4}, CellSpace.boxes(westSlab, Orientation.EAST),
                "turned a quarter clockwise, its west is north");
        assertArrayEquals(new int[] {12, 0, 0, 16, 16, 16}, CellSpace.boxes(westSlab, Orientation.SOUTH));
        assertArrayEquals(new int[] {0, 0, 12, 16, 16, 16}, CellSpace.boxes(westSlab, Orientation.WEST));
    }

    @Test
    void boxes_standAsAPointOfThemDoes() {
        final int[] box = {2, 3, 5, 7, 11, 13};
        for (final Orientation turn : Orientation.lyingAndMirrors()) {
            final int[] turned = CellSpace.boxes(box, turn);
            final double[] corner = new double[3];
            CellSpace.toWorld(new double[] {2.5 / 16, 3.5 / 16, 5.5 / 16}, turn, corner);
            for (int axis = 0; axis < 3; axis++) {
                final double at = corner[axis] * 16;
                assertTrue(at > turned[axis] && at < turned[axis + 3], "the box holds its own corner, turned "
                        + turn);
            }
        }
    }

    @Test
    void toModel_undoesToWorld() {
        final double[] point = {0.1, 0.35, 0.8};
        for (final Orientation turn : Orientation.lyingAndMirrors()) {
            final double[] world = new double[3];
            final double[] back = new double[3];
            CellSpace.toWorld(point, turn, world);
            CellSpace.toModel(world, turn, back);
            assertArrayEquals(point, back, CLOSE, "turned " + turn);
        }
    }

    @Test
    void cellOf_undoesWhereAStructureStandsAPlace() {
        for (final Orientation turn : Orientation.lyingAndMirrors()) {
            final int[] cell = CellSpace.cellOf(3, -1, 2, turn);
            assertArrayEquals(new int[] {3, -1, 2}, turn.apply(cell[0], cell[1], cell[2]), "turned " + turn);
        }
    }
}
