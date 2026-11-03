/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Which points of a model lie within its solids, so a block collides with all of what its faces close and not only
 * with the sixteenths its faces pass through: a hull's wall is solid through its thickness, a seat or a console solid
 * through and through. A point is within when every line through it along the three axes, followed from either end,
 * has gone into the model's faces more often than out of them, a face's way out being its front, as the game draws it.
 * Asking all six ways keeps a model that is not closed everywhere, a lone panel or a window's edge, from filling the
 * room beside it: there, at least one way says it is out, and the block keeps only its faces' sixteenths. Pure, so it
 * is tested without the game.
 */
public final class MeshSolid {

    /* The model's faces, nine numbers to a triangle, in its own space. */
    private final float[] triangles;

    private static final int TRIANGLE = 9;
    private static final int AXES = 3;
    /*
     * How far each line is set off a grid's middles, in blocks, a different way on each axis: far under a sixteenth,
     * so it reads the same sixteenth, and off the edges the models' faces share, which a line along them would cross
     * twice or not at all.
     */
    private static final double[] OFF = {1.37e-5, 2.91e-5, 4.53e-5};
    /* A face this close to lying along a line does not cross it. */
    private static final double ALONG = 1.0e-9;

    private MeshSolid(final float[] triangles) {
        this.triangles = triangles;
    }

    /**
     * The solids of the model of {@code triangles}.
     *
     * @param triangles nine numbers to a triangle, in the model's own space, facing out of what they close
     */
    public static MeshSolid of(final float[] triangles) {
        return new MeshSolid(triangles.clone());
    }

    /**
     * Which cells of a grid of {@code grid} to a side, in the block whose lowest corner is at {@code corner} in the
     * model's space, have their middles within the model's solids.
     *
     * @return by {@code (x * grid + y) * grid + z}, as {@link MeshVoxels} reads its grid
     */
    public boolean[] within(final double[] corner, final int grid) {
        final boolean[] within = new boolean[grid * grid * grid];
        Arrays.fill(within, true);
        for (int axis = 0; axis < AXES; axis++) {
            sweep(corner, grid, axis, within);
        }
        return within;
    }

    /*
     * Keeps within only the cells the lines along {@code axis} find within from both ends: each line through a column
     * of the grid, with the faces whose bounds it passes through.
     */
    private void sweep(final double[] corner, final int grid, final int axis, final boolean[] within) {
        final int u = (axis + 1) % AXES;
        final int v = (axis + 2) % AXES;
        final List<List<Integer>> columns = columns(corner, grid, u, v);
        final double cell = 1.0 / grid;
        final List<double[]> crossings = new ArrayList<>();
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
                final double atU = corner[u] + (i + 0.5) * cell + OFF[u];
                final double atV = corner[v] + (j + 0.5) * cell + OFF[v];
                crossings.clear();
                for (final int t : columns.get(i * grid + j)) {
                    final double[] crossing = cross(t, axis, u, v, atU, atV);
                    if (crossing != null) {
                        crossings.add(crossing);
                    }
                }
                for (int k = 0; k < grid; k++) {
                    final int[] at = new int[AXES];
                    at[axis] = k;
                    at[u] = i;
                    at[v] = j;
                    final int index = (at[0] * grid + at[1]) * grid + at[2];
                    if (within[index]) {
                        within[index] = enclosed(crossings, corner[axis] + (k + 0.5) * cell + OFF[axis]);
                    }
                }
            }
        }
    }

    /* The faces whose bounds across {@code u} and {@code v} take in each column of the block's grid. */
    private List<List<Integer>> columns(final double[] corner, final int grid, final int u, final int v) {
        final List<List<Integer>> out = new ArrayList<>(grid * grid);
        for (int c = 0; c < grid * grid; c++) {
            out.add(new ArrayList<>());
        }
        for (int t = 0; t + TRIANGLE <= this.triangles.length; t += TRIANGLE) {
            final int[] fromU = span(t, u, corner[u], grid);
            final int[] fromV = span(t, v, corner[v], grid);
            if (fromU == null || fromV == null) {
                continue;
            }
            for (int i = fromU[0]; i <= fromU[1]; i++) {
                for (int j = fromV[0]; j <= fromV[1]; j++) {
                    out.get(i * grid + j).add(t);
                }
            }
        }
        return out;
    }

    /* The columns of the grid from {@code low} the face at {@code t} reaches across {@code axis}; none for none. */
    private int[] span(final int t, final int axis, final double low, final int grid) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int c = 0; c < 3; c++) {
            final double at = this.triangles[t + c * AXES + axis];
            min = Math.min(min, at);
            max = Math.max(max, at);
        }
        final int first = (int) Math.ceil((min - low) * grid - 0.5);
        final int last = (int) Math.floor((max - low) * grid - 0.5);
        if (last < 0 || first > grid - 1 || first > last + 1) {
            return null;
        }
        return new int[] {Math.max(0, first - 1), Math.min(grid - 1, last + 1)};
    }

    /*
     * Where the line along {@code axis} through ({@code atU}, {@code atV}) crosses the face at {@code t}, and whether
     * it goes in there going up the axis: {where, +1 in or -1 out}; none when it misses or runs along the face.
     */
    private double[] cross(final int t, final int axis, final int u, final int v, final double atU,
                           final double atV) {
        final float[] f = this.triangles;
        final double au = f[t + u] - atU;
        final double av = f[t + v] - atV;
        final double bu = f[t + AXES + u] - atU;
        final double bv = f[t + AXES + v] - atV;
        final double cu = f[t + 2 * AXES + u] - atU;
        final double cv = f[t + 2 * AXES + v] - atV;
        // The face seen down the axis: the line passes through it when the point is on one side of all three edges.
        final double ab = au * bv - av * bu;
        final double bc = bu * cv - bv * cu;
        final double ca = cu * av - cv * au;
        final double area = ab + bc + ca;
        if (Math.abs(area) < ALONG || ab * area < 0 || bc * area < 0 || ca * area < 0) {
            return null;
        }
        final double where = (bc * f[t + axis] + ca * f[t + AXES + axis] + ab * f[t + 2 * AXES + axis]) / area;
        // The face's front along the axis is the sign of its normal there, which is that of its area seen down it.
        return new double[] {where, area < 0 ? 1 : -1};
    }

    /* Whether a point at {@code at} along the line has gone in more than out from both of the line's ends. */
    private static boolean enclosed(final List<double[]> crossings, final double at) {
        int fromBelow = 0;
        int fromAbove = 0;
        for (final double[] crossing : crossings) {
            if (crossing[0] < at) {
                fromBelow += (int) crossing[1];
            } else {
                fromAbove -= (int) crossing[1];
            }
        }
        return fromBelow > 0 && fromAbove > 0;
    }
}
