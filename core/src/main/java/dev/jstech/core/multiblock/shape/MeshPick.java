/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

/**
 * Where a line of sight first meets the faces of a block of a formed structure: tested against each triangle, from
 * either side, as the game draws the models' faces. Pure, so it is tested without the game.
 */
public final class MeshPick {

    /* A line nearly along a face meets it nowhere it could be aimed at. */
    private static final double PARALLEL = 1.0e-12;
    private static final int TRIANGLE = 9;

    private MeshPick() {
    }

    /**
     * How far along the line from {@code from} to {@code to} it first meets one of {@code triangles}, from 0 at
     * {@code from} to 1 at {@code to}; or -1 where it meets none. The way the face met turns is put in {@code normal},
     * toward {@code from}.
     *
     * @param triangles nine numbers to a triangle
     * @param from      where the line starts
     * @param to        where it ends
     * @param normal    three numbers, set to the way the face met turns
     */
    public static double along(final float[] triangles, final double[] from, final double[] to,
                               final double[] normal) {
        final double dx = to[0] - from[0];
        final double dy = to[1] - from[1];
        final double dz = to[2] - from[2];
        double best = -1;
        for (int t = 0; t + TRIANGLE <= triangles.length; t += TRIANGLE) {
            final double ax = triangles[t];
            final double ay = triangles[t + 1];
            final double az = triangles[t + 2];
            final double e1x = triangles[t + 3] - ax;
            final double e1y = triangles[t + 4] - ay;
            final double e1z = triangles[t + 5] - az;
            final double e2x = triangles[t + 6] - ax;
            final double e2y = triangles[t + 7] - ay;
            final double e2z = triangles[t + 8] - az;
            final double px = dy * e2z - dz * e2y;
            final double py = dz * e2x - dx * e2z;
            final double pz = dx * e2y - dy * e2x;
            final double det = e1x * px + e1y * py + e1z * pz;
            if (Math.abs(det) < PARALLEL) {
                continue;
            }
            final double inv = 1.0 / det;
            final double sx = from[0] - ax;
            final double sy = from[1] - ay;
            final double sz = from[2] - az;
            final double u = (sx * px + sy * py + sz * pz) * inv;
            if (u < 0 || u > 1) {
                continue;
            }
            final double qx = sy * e1z - sz * e1y;
            final double qy = sz * e1x - sx * e1z;
            final double qz = sx * e1y - sy * e1x;
            final double v = (dx * qx + dy * qy + dz * qz) * inv;
            if (v < 0 || u + v > 1) {
                continue;
            }
            final double at = (e2x * qx + e2y * qy + e2z * qz) * inv;
            if (at < 0 || at > 1 || best >= 0 && at >= best) {
                continue;
            }
            best = at;
            double nx = e1y * e2z - e1z * e2y;
            double ny = e1z * e2x - e1x * e2z;
            double nz = e1x * e2y - e1y * e2x;
            if (nx * dx + ny * dy + nz * dz > 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            normal[0] = nx / length;
            normal[1] = ny / length;
            normal[2] = nz / length;
        }
        return best;
    }
}
