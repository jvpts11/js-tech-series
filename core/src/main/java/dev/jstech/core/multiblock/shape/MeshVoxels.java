/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.ArrayList;
import java.util.List;

/**
 * What a block of a formed structure collides with, worked out from its faces: the sixteenths of the block a face
 * passes through, joined into as few boxes as they make. A block whose boxes would be more than {@link #MOST_BOXES}
 * is worked out in eighths instead, that block alone, so a curved wall costs no more than a few dozen boxes wherever it
 * stands. A face is moved a hair into the solid it closes first, so one lying on the block's wall fills the sixteenth
 * inside it; one reaching out of the block, as a piece carried for a place the structure has no block in, is pressed
 * onto the block's nearest side. Pure, so it is tested without the game.
 */
public final class MeshVoxels {

    /** The finest the boxes go: a sixteenth of a block, a pixel of the game's own textures. */
    public static final int FINE = 16;
    /** The coarser grid a block of too many boxes is worked out in. */
    public static final int COARSE = 8;
    /** The most boxes a block worked out in sixteenths may have. */
    public static final int MOST_BOXES = 32;
    /** How many numbers each box has: its lowest corner, then its highest, in sixteenths of a block. */
    public static final int BOX = 6;
    /* How far a face is moved into the solid it closes, in blocks: under a sixteenth, over the rounding of a float. */
    private static final double INTO = 1.0e-3;
    /* How much a sixteenth is shrunk before a face is tested against it, so a face only touching it misses. */
    private static final double SHRINK = 1.0e-6;
    private static final int TRIANGLE = 9;
    private static final int AXES = 3;

    private MeshVoxels() {
    }

    /**
     * The boxes of a block with {@code triangles} in it.
     *
     * @param triangles nine numbers to a triangle, in the block's own space, from 0 to 1 along each axis
     * @return {@link #BOX} numbers to a box, in sixteenths of a block
     */
    public static int[] boxes(final float[] triangles) {
        final int[] fine = boxes(triangles, FINE);
        if (fine.length / BOX <= MOST_BOXES) {
            return fine;
        }
        final int[] coarse = boxes(triangles, COARSE);
        final int scale = FINE / COARSE;
        for (int i = 0; i < coarse.length; i++) {
            coarse[i] *= scale;
        }
        return coarse;
    }

    /** The boxes of a block with {@code triangles} in it, worked out in a grid of {@code grid} to a side. */
    static int[] boxes(final float[] triangles, final int grid) {
        return join(fill(triangles, grid), grid);
    }

    /* Each cell of the grid a face passes through. */
    private static boolean[] fill(final float[] triangles, final int grid) {
        final boolean[] full = new boolean[grid * grid * grid];
        final double[][] corner = new double[3][AXES];
        final double cell = 1.0 / grid;
        final double half = cell / 2 - SHRINK;
        for (int t = 0; t + TRIANGLE <= triangles.length; t += TRIANGLE) {
            for (int c = 0; c < 3; c++) {
                for (int axis = 0; axis < AXES; axis++) {
                    corner[c][axis] = triangles[t + c * AXES + axis];
                }
            }
            final double[] normal = MeshCells.normal(corner[0], corner[1], corner[2]);
            if (normal == null) {
                continue;
            }
            // Pressed a hair within the block's sides, so one pressed onto a side fills the sixteenths along it.
            for (final double[] c : corner) {
                for (int axis = 0; axis < AXES; axis++) {
                    c[axis] = Math.min(1 - INTO, Math.max(INTO, c[axis] - normal[axis] * INTO));
                }
            }
            final double[] pressed = MeshCells.normal(corner[0], corner[1], corner[2]);
            final int[] low = new int[AXES];
            final int[] high = new int[AXES];
            for (int axis = 0; axis < AXES; axis++) {
                final double min = Math.min(corner[0][axis], Math.min(corner[1][axis], corner[2][axis]));
                final double max = Math.max(corner[0][axis], Math.max(corner[1][axis], corner[2][axis]));
                low[axis] = Math.clamp((int) Math.floor(min * grid), 0, grid - 1);
                high[axis] = Math.clamp((int) Math.floor(max * grid), 0, grid - 1);
            }
            for (int x = low[0]; x <= high[0]; x++) {
                for (int y = low[1]; y <= high[1]; y++) {
                    for (int z = low[2]; z <= high[2]; z++) {
                        final int at = (x * grid + y) * grid + z;
                        if (full[at]) {
                            continue;
                        }
                        // A face pressed flat onto the block's side is a line or a point: its sixteenths are those
                        // of its box, which is that flat.
                        full[at] = pressed == null || overlaps(corner, ((x + 0.5) * cell), ((y + 0.5) * cell),
                                ((z + 0.5) * cell), half);
                    }
                }
            }
        }
        return full;
    }

    /* The cells of the grid that are full, joined greedily: along x, then z, then y. */
    private static int[] join(final boolean[] full, final int grid) {
        final boolean[] taken = new boolean[full.length];
        final List<int[]> boxes = new ArrayList<>();
        final int step = FINE / grid;
        for (int y = 0; y < grid; y++) {
            for (int z = 0; z < grid; z++) {
                for (int x = 0; x < grid; x++) {
                    if (!free(full, taken, grid, x, y, z)) {
                        continue;
                    }
                    int x1 = x + 1;
                    while (x1 < grid && free(full, taken, grid, x1, y, z)) {
                        x1++;
                    }
                    int z1 = z + 1;
                    while (z1 < grid && row(full, taken, grid, x, x1, y, z1)) {
                        z1++;
                    }
                    int y1 = y + 1;
                    while (y1 < grid && layer(full, taken, grid, x, x1, y1, z, z1)) {
                        y1++;
                    }
                    for (int yy = y; yy < y1; yy++) {
                        for (int zz = z; zz < z1; zz++) {
                            for (int xx = x; xx < x1; xx++) {
                                taken[(xx * grid + yy) * grid + zz] = true;
                            }
                        }
                    }
                    boxes.add(new int[] {x * step, y * step, z * step, x1 * step, y1 * step, z1 * step});
                }
            }
        }
        final int[] out = new int[boxes.size() * BOX];
        for (int i = 0; i < boxes.size(); i++) {
            System.arraycopy(boxes.get(i), 0, out, i * BOX, BOX);
        }
        return out;
    }

    private static boolean free(final boolean[] full, final boolean[] taken, final int grid, final int x,
                                final int y, final int z) {
        final int at = (x * grid + y) * grid + z;
        return full[at] && !taken[at];
    }

    private static boolean row(final boolean[] full, final boolean[] taken, final int grid, final int x0,
                               final int x1, final int y, final int z) {
        for (int x = x0; x < x1; x++) {
            if (!free(full, taken, grid, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean layer(final boolean[] full, final boolean[] taken, final int grid, final int x0,
                                 final int x1, final int y, final int z0, final int z1) {
        for (int z = z0; z < z1; z++) {
            if (!row(full, taken, grid, x0, x1, y, z)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Whether the triangle {@code corner} meets the cube of half-side {@code half} round ({@code cx}, {@code cy},
     * {@code cz}): the separating axes of a triangle and a box, the box's three, the triangle's normal and the nine
     * crossings of their edges.
     */
    private static boolean overlaps(final double[][] corner, final double cx, final double cy, final double cz,
                                    final double half) {
        final double[] v0 = {corner[0][0] - cx, corner[0][1] - cy, corner[0][2] - cz};
        final double[] v1 = {corner[1][0] - cx, corner[1][1] - cy, corner[1][2] - cz};
        final double[] v2 = {corner[2][0] - cx, corner[2][1] - cy, corner[2][2] - cz};
        for (int axis = 0; axis < AXES; axis++) {
            final double min = Math.min(v0[axis], Math.min(v1[axis], v2[axis]));
            final double max = Math.max(v0[axis], Math.max(v1[axis], v2[axis]));
            if (min > half || max < -half) {
                return false;
            }
        }
        final double[][] edges = {sub(v1, v0), sub(v2, v1), sub(v0, v2)};
        final double[] normal = cross(edges[0], edges[1]);
        if (!apart(normal, v0, v1, v2, half)) {
            for (final double[] edge : edges) {
                for (int axis = 0; axis < AXES; axis++) {
                    final double[] unit = new double[AXES];
                    unit[axis] = 1;
                    if (apart(cross(edge, unit), v0, v1, v2, half)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /* Whether the axis {@code axis} keeps the triangle and the box apart. */
    private static boolean apart(final double[] axis, final double[] v0, final double[] v1, final double[] v2,
                                 final double half) {
        final double p0 = dot(axis, v0);
        final double p1 = dot(axis, v1);
        final double p2 = dot(axis, v2);
        final double reach = half * (Math.abs(axis[0]) + Math.abs(axis[1]) + Math.abs(axis[2]));
        return Math.min(p0, Math.min(p1, p2)) > reach || Math.max(p0, Math.max(p1, p2)) < -reach;
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] sub(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
