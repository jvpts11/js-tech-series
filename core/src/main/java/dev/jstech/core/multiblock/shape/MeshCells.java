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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A formed model's faces shared out among the blocks of its structure: each face cut where it crosses from one block
 * to the next, each piece given to the block it stands in, in that block's own space, from 0 to 1 along each axis.
 * Pure, so it is tested without the game.
 *
 * <p>A face lying on the wall between two blocks belongs to the block whose solid it closes, the one it faces away
 * from, as a floor closes the block under it; or, when that block is none of the structure's, to the block it faces,
 * as a tank's inner dome on its foot does. A piece in a place the structure has no block in is carried by the nearest
 * block that is one, a block away at most, reaching out of it; one farther than that is out of the structure, a
 * mistake in the model. A block of the structure that no face reaches is one too.
 */
public final class MeshCells {

    /** How far a face is moved against the way it faces to tell which block it closes, in blocks. */
    static final double NUDGE = 1.0e-4;
    /* A piece smaller than this, in square blocks, is the trace of a face that only touches a block. */
    private static final double LEAST_AREA = 1.0e-9;
    private static final int CORNERS = 3;
    private static final int TRIANGLE = 9;
    private static final int BITS = 21;
    private static final long MASK = (1L << BITS) - 1;
    private static final int AXES = 3;

    private MeshCells() {
    }

    /**
     * Shares {@code triangles} out among the blocks of a structure.
     *
     * @param triangles nine numbers to a triangle, its corners in the model's space, in blocks
     * @param origin    where the lowest corner of the block {@code (0, 0, 0)} is in the model's space
     * @param shaped    the blocks that take a shape from their pieces, by {@link #key}; each needs a piece
     * @param others    the structure's other blocks, whose pieces stand for themselves and are left out
     * @return each shaped block's pieces in its own space, nine numbers to a triangle, and where it does not fit
     */
    public static Split split(final float[] triangles, final double[] origin, final Set<Long> shaped,
                              final Set<Long> others) {
        final Map<Long, FloatBuffer> pieces = new HashMap<>();
        final Map<Long, Integer> lost = new TreeMap<>();
        final double[][] corner = new double[CORNERS][AXES];
        for (int t = 0; t + TRIANGLE <= triangles.length; t += TRIANGLE) {
            for (int c = 0; c < CORNERS; c++) {
                for (int axis = 0; axis < AXES; axis++) {
                    corner[c][axis] = triangles[t + c * AXES + axis] - origin[axis];
                }
            }
            final double[] normal = normal(corner[0], corner[1], corner[2]);
            if (normal == null) {
                continue;
            }
            share(corner, normal, shaped, others, pieces, lost);
        }
        final List<Long> bare = new ArrayList<>();
        final Map<Long, float[]> out = new TreeMap<>();
        for (final long cell : shaped) {
            final FloatBuffer found = pieces.get(cell);
            if (found == null || found.size == 0) {
                bare.add(cell);
            } else {
                out.put(cell, found.toArray());
            }
        }
        bare.sort(Long::compare);
        return new Split(out, lost, bare);
    }

    /** The key of the block at ({@code x}, {@code y}, {@code z}), each within a million of the origin. */
    public static long key(final int x, final int y, final int z) {
        return (x & MASK) << (BITS * 2) | (y & MASK) << BITS | z & MASK;
    }

    /** The x of the block of {@code key}. */
    public static int x(final long key) {
        return signed(key >>> (BITS * 2) & MASK);
    }

    /** The y of the block of {@code key}. */
    public static int y(final long key) {
        return signed(key >>> BITS & MASK);
    }

    /** The z of the block of {@code key}. */
    public static int z(final long key) {
        return signed(key & MASK);
    }

    /** The block of {@code key} as people read it. */
    public static String name(final long key) {
        return "(" + x(key) + ", " + y(key) + ", " + z(key) + ")";
    }

    /* The way the triangle faces, of length 1; or none for one of no area. */
    static double[] normal(final double[] a, final double[] b, final double[] c) {
        final double[] n = cross(sub(b, a), sub(c, a));
        final double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (length < LEAST_AREA) {
            return null;
        }
        return new double[] {n[0] / length, n[1] / length, n[2] / length};
    }

    /*
     * One triangle cut among the blocks it crosses, moved a hair against its normal to tell the block it closes, each
     * piece given to its block, to the one it faces, or to the nearest one of the structure.
     */
    private static void share(final double[][] corner, final double[] normal, final Set<Long> shaped,
                              final Set<Long> others, final Map<Long, FloatBuffer> pieces,
                              final Map<Long, Integer> lost) {
        final List<double[]> polygon = new ArrayList<>(CORNERS);
        for (final double[] c : corner) {
            polygon.add(new double[] {c[0] - normal[0] * NUDGE, c[1] - normal[1] * NUDGE,
                c[2] - normal[2] * NUDGE});
        }
        final int[] low = new int[AXES];
        final int[] high = new int[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (final double[] p : polygon) {
                min = Math.min(min, p[axis]);
                max = Math.max(max, p[axis]);
            }
            low[axis] = (int) Math.floor(min);
            high[axis] = (int) Math.floor(max);
        }
        for (int x = low[0]; x <= high[0]; x++) {
            for (int y = low[1]; y <= high[1]; y++) {
                for (int z = low[2]; z <= high[2]; z++) {
                    final List<double[]> piece = low[0] == high[0] && low[1] == high[1] && low[2] == high[2]
                            ? polygon : clip(polygon, x, y, z);
                    if (piece.size() < CORNERS || area(piece) < LEAST_AREA) {
                        continue;
                    }
                    give(piece, normal, key(x, y, z), shaped, others, pieces, lost);
                }
            }
        }
    }

    /* A piece in the block {@code cell}, moved back onto the face it came from, given to the block that takes it. */
    private static void give(final List<double[]> piece, final double[] normal, final long cell,
                             final Set<Long> shaped, final Set<Long> others, final Map<Long, FloatBuffer> pieces,
                             final Map<Long, Integer> lost) {
        final List<double[]> back = new ArrayList<>(piece.size());
        for (final double[] p : piece) {
            back.add(new double[] {p[0] + normal[0] * NUDGE, p[1] + normal[1] * NUDGE, p[2] + normal[2] * NUDGE});
        }
        long owner = cell;
        if (!shaped.contains(cell)) {
            if (others.contains(cell)) {
                return;
            }
            final double[] middle = middle(back);
            final long faced = key((int) Math.floor(middle[0] + normal[0] * NUDGE),
                    (int) Math.floor(middle[1] + normal[1] * NUDGE), (int) Math.floor(middle[2] + normal[2] * NUDGE));
            if (shaped.contains(faced)) {
                owner = faced;
            } else if (others.contains(faced)) {
                return;
            } else {
                owner = nearest(middle, cell, shaped);
                if (owner == cell) {
                    lost.merge(cell, 1, Integer::sum);
                    return;
                }
            }
        }
        final FloatBuffer into = pieces.computeIfAbsent(owner, any -> new FloatBuffer());
        final double ox = x(owner);
        final double oy = y(owner);
        final double oz = z(owner);
        final double[] first = back.getFirst();
        for (int i = 1; i + 1 < back.size(); i++) {
            for (final double[] p : List.of(first, back.get(i), back.get(i + 1))) {
                into.add((float) (p[0] - ox), (float) (p[1] - oy), (float) (p[2] - oz));
            }
        }
    }

    /* The shaped block next to {@code cell}, sides first, nearest to {@code at}; or {@code cell}, for none. */
    private static long nearest(final double[] at, final long cell, final Set<Long> shaped) {
        long best = cell;
        double bestDistance = Double.MAX_VALUE;
        final int cx = x(cell);
        final int cy = y(cell);
        final int cz = z(cell);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final long next = key(cx + dx, cy + dy, cz + dz);
                    if (!shaped.contains(next)) {
                        continue;
                    }
                    final double distance = outside(at[0], cx + dx) + outside(at[1], cy + dy)
                            + outside(at[2], cz + dz) + (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) * NUDGE;
                    if (distance < bestDistance || distance == bestDistance && next < best) {
                        best = next;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    /* How far {@code value} lies outside the span of the block {@code at} along one axis, squared. */
    private static double outside(final double value, final int at) {
        final double out = value < at ? at - value : value > at + 1 ? value - at - 1 : 0;
        return out * out;
    }

    /* {@code polygon} cut to the block whose lowest corner is ({@code x}, {@code y}, {@code z}). */
    private static List<double[]> clip(final List<double[]> polygon, final int x, final int y, final int z) {
        final int[] at = {x, y, z};
        List<double[]> out = polygon;
        for (int axis = 0; axis < AXES && out.size() >= CORNERS; axis++) {
            out = cut(out, axis, at[axis], true);
            out = cut(out, axis, at[axis] + 1, false);
        }
        return out;
    }

    /* The part of {@code polygon} on one side of the plane where {@code axis} is {@code at}: above it, or below. */
    private static List<double[]> cut(final List<double[]> polygon, final int axis, final double at,
                                      final boolean above) {
        final List<double[]> out = new ArrayList<>(polygon.size() + 1);
        for (int i = 0; i < polygon.size(); i++) {
            final double[] a = polygon.get(i);
            final double[] b = polygon.get((i + 1) % polygon.size());
            final boolean aIn = above ? a[axis] >= at : a[axis] <= at;
            final boolean bIn = above ? b[axis] >= at : b[axis] <= at;
            if (aIn) {
                out.add(a);
            }
            if (aIn != bIn) {
                final double f = (at - a[axis]) / (b[axis] - a[axis]);
                final double[] p = new double[AXES];
                for (int k = 0; k < AXES; k++) {
                    p[k] = a[k] + (b[k] - a[k]) * f;
                }
                p[axis] = at;
                out.add(p);
            }
        }
        return out;
    }

    private static double area(final List<double[]> polygon) {
        final double[] first = polygon.getFirst();
        double sum = 0;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            final double[] n = cross(sub(polygon.get(i), first), sub(polygon.get(i + 1), first));
            sum += Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) / 2;
        }
        return sum;
    }

    private static double[] middle(final List<double[]> polygon) {
        final double[] out = new double[AXES];
        for (final double[] p : polygon) {
            for (int axis = 0; axis < AXES; axis++) {
                out[axis] += p[axis] / polygon.size();
            }
        }
        return out;
    }

    private static double[] sub(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static int signed(final long value) {
        return (int) (value << (Long.SIZE - BITS) >> (Long.SIZE - BITS));
    }

    /**
     * A formed model's faces shared out among its blocks.
     *
     * @param pieces  each shaped block's pieces, by its key, in its own space, nine numbers to a triangle
     * @param outside the places more than a block from every block of the structure that faces lie in, each with how
     *                many pieces of faces lie there
     * @param bare    the shaped blocks no face reaches
     */
    public record Split(Map<Long, float[]> pieces, Map<Long, Integer> outside, List<Long> bare) {

        public Split {
            pieces = Map.copyOf(pieces);
            outside = Collections.unmodifiableMap(new TreeMap<>(outside));
            bare = List.copyOf(bare);
        }

        /** Whether the model fits its structure: every shaped block has a face, and no face lies outside. */
        public boolean fits() {
            return this.outside.isEmpty() && this.bare.isEmpty();
        }
    }

    /* Numbers gathered as they come. */
    private static final class FloatBuffer {

        private float[] values = new float[TRIANGLE * 4];
        private int size;

        void add(final float x, final float y, final float z) {
            if (this.size + AXES > this.values.length) {
                this.values = Arrays.copyOf(this.values, this.values.length * 2);
            }
            this.values[this.size++] = x;
            this.values[this.size++] = y;
            this.values[this.size++] = z;
        }

        float[] toArray() {
            return Arrays.copyOf(this.values, this.size);
        }
    }
}
