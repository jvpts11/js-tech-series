/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeshOutlineTest {

    private static final double CLOSE = 1.0e-3;

    @Test
    void lines_ofACubeAreItsTwelveEdges() {
        final float[] lines = MeshOutline.lines(cube());

        assertEquals(12, lines.length / MeshOutline.LINE);
        for (int at = 0; at < lines.length; at += MeshOutline.LINE) {
            assertEquals(1.0, length(lines, at), CLOSE, "each a whole edge, the faces' halves aside");
        }
    }

    @Test
    void lines_leaveOutEdgesBetweenFlatFacesEvenWhereACornerSitsHalfwayAlongOne() {
        final float[] square = {0, 0, 0.5F, 1, 0, 0.5F, 0, 1, 0.5F,
            1, 0, 0.5F, 1, 1, 0.5F, 0.5F, 0.5F, 0.5F,
            0.5F, 0.5F, 0.5F, 1, 1, 0.5F, 0, 1, 0.5F};
        final float[] lines = MeshOutline.lines(square);

        assertEquals(4, lines.length / MeshOutline.LINE, "the square's four sides alone");
        double total = 0;
        for (int at = 0; at < lines.length; at += MeshOutline.LINE) {
            total += length(lines, at);
        }
        assertEquals(4.0, total, CLOSE);
    }

    @Test
    void lines_leaveOutTheEdgesOfAGentleCurve() {
        final double tilt = Math.toRadians(10);
        final float rise = (float) Math.sin(tilt);
        final float run = (float) Math.cos(tilt);
        final float[] bent = {0, 0, 0, 1, 0, 0, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1 + run, rise, 0, 1, 0, 1,
            1 + run, rise, 0, 1 + run, rise, 1, 1, 0, 1};
        final float[] lines = MeshOutline.lines(bent);

        for (int at = 0; at < lines.length; at += MeshOutline.LINE) {
            final boolean alongTheBend = Math.abs(lines[at] - 1) < CLOSE && Math.abs(lines[at + 3] - 1) < CLOSE;
            assertEquals(false, alongTheBend, "the gentle bend is not an edge");
        }
        assertEquals(6, lines.length / MeshOutline.LINE, "the border of the bent sheet");
    }

    @Test
    void lines_drawASharpBend() {
        final float[] folded = {0, 0, 0, 1, 0, 0, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 1, 0, 1, 0, 1,
            1, 1, 0, 1, 1, 1, 1, 0, 1};
        final float[] lines = MeshOutline.lines(folded);

        boolean fold = false;
        for (int at = 0; at < lines.length; at += MeshOutline.LINE) {
            fold |= Math.abs(lines[at] - 1) < CLOSE && Math.abs(lines[at + 1]) < CLOSE
                    && Math.abs(lines[at + 3] - 1) < CLOSE && Math.abs(lines[at + 4]) < CLOSE;
        }
        assertEquals(true, fold, "the fold is drawn");
    }

    /* The six faces of the block, facing out, two triangles each, each split along its own diagonal. */
    private static float[] cube() {
        final List<float[]> corners = new ArrayList<>();
        for (int axis = 0; axis < 3; axis++) {
            for (final int at : new int[] {0, 1}) {
                final int u = (axis + 1) % 3;
                final int v = (axis + 2) % 3;
                final int[][] square = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
                final float[][] points = new float[4][3];
                for (int k = 0; k < 4; k++) {
                    points[k][axis] = at;
                    points[k][u] = square[k][0];
                    points[k][v] = square[k][1];
                }
                final int[] order = at == 1 ? new int[] {0, 1, 2, 0, 2, 3} : new int[] {0, 2, 1, 0, 3, 2};
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

    private static double length(final float[] lines, final int at) {
        final double dx = lines[at + 3] - lines[at];
        final double dy = lines[at + 4] - lines[at + 1];
        final double dz = lines[at + 5] - lines[at + 2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
