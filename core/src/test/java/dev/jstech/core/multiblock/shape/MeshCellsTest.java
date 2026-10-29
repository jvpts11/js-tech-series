/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MeshCellsTest {

    private static final double[] ORIGIN = {0, 0, 0};
    private static final long HERE = MeshCells.key(0, 0, 0);
    private static final long EAST = MeshCells.key(1, 0, 0);
    private static final long ABOVE = MeshCells.key(0, 1, 0);
    private static final double CLOSE = 1.0e-3;

    @Test
    void split_givesEachFaceToTheBlockItStandsIn() {
        final MeshCells.Split split = MeshCells.split(new float[] {0.2F, 0.5F, 0.2F, 0.8F, 0.5F, 0.2F, 0.2F, 0.5F,
            0.8F}, ORIGIN, Set.of(HERE), Set.of());

        assertTrue(split.fits(), split.toString());
        assertEquals(9, split.pieces().get(HERE).length);
        assertEquals(0.5, split.pieces().get(HERE)[1], CLOSE);
    }

    @Test
    void split_cutsAFaceWhereItCrossesFromOneBlockToTheNext() {
        final MeshCells.Split split = MeshCells.split(quadY(0, 2, 0.5, 0, 1, true), ORIGIN, Set.of(HERE, EAST),
                Set.of());

        assertTrue(split.fits(), split.toString());
        assertEquals(1.0, area(split.pieces().get(HERE)), CLOSE);
        assertEquals(1.0, area(split.pieces().get(EAST)), CLOSE);
        for (final float value : split.pieces().get(EAST)) {
            assertTrue(value >= -CLOSE && value <= 1 + CLOSE, "in the block's own space: " + value);
        }
    }

    @Test
    void split_givesAFloorToTheBlockItCloses() {
        final MeshCells.Split split = MeshCells.split(quadY(0, 1, 1, 0, 1, true), ORIGIN, Set.of(HERE, ABOVE),
                Set.of());

        assertEquals(1.0, area(split.pieces().get(HERE)), CLOSE);
        assertEquals(List.of(ABOVE), split.bare(), "the block above has none of it");
    }

    @Test
    void split_givesAFaceToTheBlockItFacesWhenTheOneItClosesIsNone() {
        final MeshCells.Split split = MeshCells.split(quadY(0, 1, 0, 0, 1, true), ORIGIN, Set.of(HERE), Set.of());

        assertTrue(split.fits(), split.toString());
        assertEquals(1.0, area(split.pieces().get(HERE)), CLOSE);
    }

    @Test
    void split_carriesAFaceWhereNoBlockIsByTheNearestBlock() {
        final MeshCells.Split split = MeshCells.split(quadY(1.2, 1.8, 0.5, 0.2, 0.8, true), ORIGIN, Set.of(HERE),
                Set.of());

        assertTrue(split.fits(), split.toString());
        assertTrue(split.pieces().get(HERE)[0] > 1, "it reaches out of the block that carries it");
    }

    @Test
    void split_reportsFacesMoreThanABlockOutsideTheStructure() {
        final MeshCells.Split split = MeshCells.split(quadY(2.2, 2.8, 0.5, 0.2, 0.8, true), ORIGIN, Set.of(HERE),
                Set.of());

        assertFalse(split.fits());
        assertEquals(Set.of(MeshCells.key(2, 0, 0)), split.outside().keySet());
        assertEquals(List.of(HERE), split.bare(), "and the block they belong nowhere near has none");
    }

    @Test
    void split_leavesOutTheFacesOfBlocksThatStandApart() {
        final MeshCells.Split split = MeshCells.split(quadY(1.2, 1.8, 0.5, 0.2, 0.8, true), ORIGIN, Set.of(HERE),
                Set.of(EAST));

        assertTrue(split.outside().isEmpty(), "the face stands for the block apart");
        assertEquals(List.of(HERE), split.bare(), "only the block with no face is wrong");
    }

    @Test
    void split_takesTheOriginOfTheModelsSpace() {
        final MeshCells.Split split = MeshCells.split(quadY(-0.5, 0.5, 0.5, -0.5, 0.5, true),
                new double[] {-0.5, 0, -0.5}, Set.of(HERE), Set.of());

        assertTrue(split.fits(), split.toString());
        assertEquals(1.0, area(split.pieces().get(HERE)), CLOSE);
    }

    @Test
    void key_keepsPlacesOnBothSidesOfTheOrigin() {
        final long key = MeshCells.key(-3, 7, -1_000);

        assertEquals(-3, MeshCells.x(key));
        assertEquals(7, MeshCells.y(key));
        assertEquals(-1_000, MeshCells.z(key));
    }

    /* A flat square at height {@code y}, from x0 to x1 and z0 to z1, facing up or down, as two triangles. */
    private static float[] quadY(final double x0, final double x1, final double y, final double z0, final double z1,
                                 final boolean up) {
        final float[] a = {(float) x0, (float) y, (float) z0};
        final float[] b = {(float) x1, (float) y, (float) z0};
        final float[] c = {(float) x1, (float) y, (float) z1};
        final float[] d = {(float) x0, (float) y, (float) z1};
        return up ? join(a, d, c, a, c, b) : join(a, b, c, a, c, d);
    }

    private static float[] join(final float[]... corners) {
        final float[] out = new float[corners.length * 3];
        for (int i = 0; i < corners.length; i++) {
            System.arraycopy(corners[i], 0, out, i * 3, 3);
        }
        return out;
    }

    private static double area(final float[] triangles) {
        double sum = 0;
        for (int t = 0; t + 9 <= triangles.length; t += 9) {
            final double ux = triangles[t + 3] - triangles[t];
            final double uy = triangles[t + 4] - triangles[t + 1];
            final double uz = triangles[t + 5] - triangles[t + 2];
            final double vx = triangles[t + 6] - triangles[t];
            final double vy = triangles[t + 7] - triangles[t + 1];
            final double vz = triangles[t + 8] - triangles[t + 2];
            final double x = uy * vz - uz * vy;
            final double y = uz * vx - ux * vz;
            final double z = ux * vy - uy * vx;
            sum += Math.sqrt(x * x + y * y + z * z) / 2;
        }
        return sum;
    }
}
