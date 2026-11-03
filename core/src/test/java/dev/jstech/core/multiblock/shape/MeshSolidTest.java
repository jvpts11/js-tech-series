/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeshSolidTest {

    private static final int FINE = MeshVoxels.FINE;
    private static final double[] BLOCK = {0, 0, 0};

    @Test
    void within_aClosedCubeIsSolidThroughAndThrough() {
        final MeshSolid solid = MeshSolid.of(box(0.25, 0.75, false));

        final boolean[] within = solid.within(BLOCK, FINE);

        assertTrue(within[index(8, 8, 8)], "its middle");
        assertTrue(within[index(4, 4, 4)], "its lowest cell");
        assertFalse(within[index(3, 8, 8)], "beside it");
        assertFalse(within[index(1, 1, 1)], "outside it");
    }

    @Test
    void boxes_ofAClosedCubeWithItsSolidAreOneBox() {
        final float[] cube = box(0.25, 0.75, false);
        final MeshSolid solid = MeshSolid.of(cube);

        final int[] boxes = MeshVoxels.boxes(cube, grid -> solid.within(BLOCK, grid));

        assertArrayEquals(new int[] {4, 4, 4, 12, 12, 12}, boxes);
    }

    @Test
    void within_aLonePanelClosesNothing() {
        final float[] panel = {0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.9F, 0.9F, 0.5F, 0.9F,
            0.1F, 0.5F, 0.1F, 0.9F, 0.5F, 0.9F, 0.9F, 0.5F, 0.1F};
        final MeshSolid solid = MeshSolid.of(panel);

        final boolean[] within = solid.within(BLOCK, FINE);

        for (int y = 0; y < FINE; y++) {
            assertFalse(within[index(8, y, 8)], "nothing above or under it at " + y);
        }
    }

    @Test
    void within_aHollowBoxIsSolidInItsWallsAndEmptyInside() {
        final float[] hollow = join(box(0.0, 1.0, false), box(0.25, 0.75, true));
        final MeshSolid solid = MeshSolid.of(hollow);

        final boolean[] within = solid.within(BLOCK, FINE);

        assertTrue(within[index(1, 8, 8)], "in its wall");
        assertTrue(within[index(8, 14, 8)], "in its lid");
        assertFalse(within[index(8, 8, 8)], "in the room it closes");
    }

    @Test
    void within_aSolidReachingIntoTheNextBlockIsSolidThereToo() {
        final MeshSolid solid = MeshSolid.of(box(0.5, 1.5, false));

        final boolean[] here = solid.within(BLOCK, FINE);
        final boolean[] next = solid.within(new double[] {1, 0, 0}, FINE);

        assertTrue(here[index(12, 12, 12)], "its part in the first block");
        assertFalse(here[index(4, 12, 12)], "the first block's other half");
        assertTrue(next[index(4, 12, 12)], "its part in the block beside");
        assertFalse(next[index(12, 12, 12)], "past it in the block beside");
    }

    @Test
    void within_aWallOfTwoFacesWithAGapInOneOnBothSidesLetsNoLineFillTheRoom() {
        // A room with a window cut through the outside faces only, on both sides at once.
        final List<float[]> faces = new ArrayList<>();
        faces.add(box(0.0, 1.0, false));
        faces.add(box(0.125, 0.875, true));
        final float[] room = join(faces.toArray(float[][]::new));
        final float[] cut = withoutFacesAt(room, 0.0, 1.0);
        final MeshSolid solid = MeshSolid.of(cut);

        final boolean[] within = solid.within(BLOCK, FINE);

        assertFalse(within[index(8, 8, 8)], "the room");
    }

    /* The six faces of a cube from {@code lo} to {@code hi} on each axis, facing out, or in. */
    private static float[] box(final double lo, final double hi, final boolean in) {
        final List<float[]> corners = new ArrayList<>();
        for (int axis = 0; axis < 3; axis++) {
            for (final double at : new double[] {lo, hi}) {
                final int u = (axis + 1) % 3;
                final int v = (axis + 2) % 3;
                final double[][] square = {{lo, lo}, {hi, lo}, {hi, hi}, {lo, hi}};
                final float[][] points = new float[4][3];
                for (int k = 0; k < 4; k++) {
                    points[k][axis] = (float) at;
                    points[k][u] = (float) square[k][0];
                    points[k][v] = (float) square[k][1];
                }
                final boolean out = (at == hi) != in;
                final int[] order = out ? new int[] {0, 1, 2, 0, 2, 3} : new int[] {0, 2, 1, 0, 3, 2};
                for (final int k : order) {
                    corners.add(points[k]);
                }
            }
        }
        final float[] out = new float[corners.size() * 3];
        for (int i = 0; i < corners.size(); i++) {
            System.arraycopy(corners.get(i), 0, out, i * 3, 3);
        }
        return out;
    }

    /* The triangles of {@code faces} but those lying flat on x = {@code low} or x = {@code high}. */
    private static float[] withoutFacesAt(final float[] faces, final double low, final double high) {
        final List<float[]> kept = new ArrayList<>();
        for (int t = 0; t + 9 <= faces.length; t += 9) {
            final boolean flat = faces[t] == faces[t + 3] && faces[t] == faces[t + 6];
            if (flat && (faces[t] == (float) low || faces[t] == (float) high)) {
                continue;
            }
            final float[] triangle = new float[9];
            System.arraycopy(faces, t, triangle, 0, 9);
            kept.add(triangle);
        }
        return join(kept.toArray(float[][]::new));
    }

    private static float[] join(final float[]... parts) {
        int length = 0;
        for (final float[] part : parts) {
            length += part.length;
        }
        final float[] out = new float[length];
        int at = 0;
        for (final float[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    private static int index(final int x, final int y, final int z) {
        return (x * FINE + y) * FINE + z;
    }
}
