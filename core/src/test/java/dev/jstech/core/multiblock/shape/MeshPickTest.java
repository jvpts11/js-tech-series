/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeshPickTest {

    private static final double CLOSE = 1.0e-6;
    /* Two walls across the block, at a quarter and three quarters along z, each facing south. */
    private static final float[] WALLS = {0, 0, 0.25F, 1, 0, 0.25F, 1, 1, 0.25F, 0, 0, 0.25F, 1, 1, 0.25F, 0, 1, 0.25F,
        0, 0, 0.75F, 1, 0, 0.75F, 1, 1, 0.75F, 0, 0, 0.75F, 1, 1, 0.75F, 0, 1, 0.75F};

    @Test
    void along_meetsTheNearestFaceFirst() {
        final double[] normal = new double[3];
        final double along = MeshPick.along(WALLS, new double[] {0.5, 0.5, 0}, new double[] {0.5, 0.5, 1}, normal);

        assertEquals(0.25, along, CLOSE);
        assertArrayEquals(new double[] {0, 0, -1}, normal, CLOSE, "turned toward where the line starts");
    }

    @Test
    void along_meetsAFaceFromBehindTurnedTowardTheLine() {
        final double[] normal = new double[3];
        final double along = MeshPick.along(WALLS, new double[] {0.5, 0.5, 1}, new double[] {0.5, 0.5, 0}, normal);

        assertEquals(0.25, along, CLOSE);
        assertArrayEquals(new double[] {0, 0, 1}, normal, CLOSE);
    }

    @Test
    void along_missesWhereNoFaceIs() {
        final double along = MeshPick.along(WALLS, new double[] {0.5, 1.5, 0}, new double[] {0.5, 1.5, 1},
                new double[3]);

        assertEquals(-1, along);
    }

    @Test
    void along_missesAFaceBeyondTheLinesEnd() {
        final double along = MeshPick.along(WALLS, new double[] {0.5, 0.5, 0}, new double[] {0.5, 0.5, 0.2},
                new double[3]);

        assertEquals(-1, along);
    }
}
