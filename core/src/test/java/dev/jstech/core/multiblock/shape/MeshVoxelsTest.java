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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeshVoxelsTest {

    private static final int FINE = MeshVoxels.FINE;

    @Test
    void boxes_ofAFloorAreOneSlabInsideTheBlock() {
        final int[] boxes = MeshVoxels.boxes(square(0, 1, 0, false));

        assertArrayEquals(new int[] {0, 0, 0, 16, 1, 16}, boxes);
    }

    @Test
    void boxes_ofACeilingAreOneSlabUnderTheBlocksTop() {
        final int[] boxes = MeshVoxels.boxes(square(0, 1, 1, true));

        assertArrayEquals(new int[] {0, 15, 0, 16, 16, 16}, boxes);
    }

    @Test
    void boxes_ofACubeAreItsShellAndNotItsInside() {
        final boolean[][][] full = grid(MeshVoxels.boxes(cube(0.25, 0.75)));

        assertTrue(full[4][4][4], "its corner");
        assertTrue(full[8][4][8], "its floor");
        assertTrue(full[8][8][4], "its side");
        assertFalse(full[8][8][8], "its inside");
        assertFalse(full[1][1][1], "outside it");
    }

    @Test
    void boxes_ofASlopeStepUpAsStairsDo() {
        final float[] slope = {0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1};
        final boolean[][][] full = grid(MeshVoxels.boxes(slope));

        assertTrue(full[8][8][8], "on the slope");
        assertTrue(full[3][2][2], "low on the slope");
        assertFalse(full[8][2][14], "under the slope's high end");
        assertFalse(full[8][14][2], "over its low end");
    }

    @Test
    void boxes_ofAFacePastTheBlockArePressedOntoItsSide() {
        final float[] past = {1.2F, 0.2F, 0.2F, 1.8F, 0.8F, 0.2F, 1.2F, 0.8F, 0.8F};
        final boolean[][][] full = grid(MeshVoxels.boxes(past));

        assertTrue(full[15][8][8], "the side it is pressed onto");
        assertFalse(full[14][8][8], "nothing within");
    }

    @Test
    void boxes_ofMoreThanTheMostAreWorkedOutInEighths() {
        final float[] ball = ball(0.45, 24);
        final int[] fine = MeshVoxels.boxes(ball, FINE);
        final int[] boxes = MeshVoxels.boxes(ball);

        assertTrue(fine.length / MeshVoxels.BOX > MeshVoxels.MOST_BOXES, "a ball is more than the most in sixteenths");
        for (final int value : boxes) {
            assertEquals(0, value % (FINE / MeshVoxels.COARSE), "in eighths: " + value);
            assertTrue(value >= 0 && value <= FINE, "still in sixteenths, within the block: " + value);
        }
        assertTrue(boxes.length < fine.length);
        final boolean[][][] full = grid(boxes);
        assertTrue(full[8][8][1] && !full[8][8][8], "the ball's shell, not its inside, as in sixteenths");
    }

    /* A square across the whole block at height {@code y}, facing up or down. */
    private static float[] square(final double x0, final double x1, final double y, final boolean up) {
        final float[] a = {(float) x0, (float) y, (float) x0};
        final float[] b = {(float) x1, (float) y, (float) x0};
        final float[] c = {(float) x1, (float) y, (float) x1};
        final float[] d = {(float) x0, (float) y, (float) x1};
        return up ? join(a, d, c, a, c, b) : join(a, b, c, a, c, d);
    }

    /* The six faces of a cube from {@code lo} to {@code hi} on each axis, facing out. */
    private static float[] cube(final double lo, final double hi) {
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
                final boolean out = at == hi;
                final int[] order = out ? new int[] {0, 1, 2, 0, 2, 3} : new int[] {0, 2, 1, 0, 3, 2};
                for (final int k : order) {
                    corners.add(points[k]);
                }
            }
        }
        return join(corners.toArray(float[][]::new));
    }

    /* A ball of {@code radius} round the block's middle, {@code steps} faces round and as many from pole to pole. */
    private static float[] ball(final double radius, final int steps) {
        final List<float[]> corners = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            for (int j = 0; j < steps; j++) {
                final float[] a = onBall(radius, i, j, steps);
                final float[] b = onBall(radius, i + 1, j, steps);
                final float[] c = onBall(radius, i + 1, j + 1, steps);
                final float[] d = onBall(radius, i, j + 1, steps);
                corners.addAll(List.of(a, b, c, a, c, d));
            }
        }
        return join(corners.toArray(float[][]::new));
    }

    private static float[] onBall(final double radius, final int i, final int j, final int steps) {
        final double polar = Math.PI * j / steps;
        final double round = 2 * Math.PI * i / steps;
        return new float[] {(float) (0.5 + radius * Math.sin(polar) * Math.cos(round)),
            (float) (0.5 + radius * Math.cos(polar)), (float) (0.5 + radius * Math.sin(polar) * Math.sin(round))};
    }

    private static float[] join(final float[]... corners) {
        final float[] out = new float[corners.length * 3];
        for (int i = 0; i < corners.length; i++) {
            System.arraycopy(corners[i], 0, out, i * 3, 3);
        }
        return out;
    }

    /* Which sixteenths of the block the boxes fill, by x, y and z. */
    private static boolean[][][] grid(final int[] boxes) {
        final boolean[][][] full = new boolean[FINE][FINE][FINE];
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            for (int x = boxes[b]; x < boxes[b + 3]; x++) {
                for (int y = boxes[b + 1]; y < boxes[b + 4]; y++) {
                    for (int z = boxes[b + 2]; z < boxes[b + 5]; z++) {
                        full[x][y][z] = true;
                    }
                }
            }
        }
        return full;
    }
}
