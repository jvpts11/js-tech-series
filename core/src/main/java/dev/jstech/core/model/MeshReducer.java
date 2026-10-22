/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Brings a mesh down to fewer triangles, as a modeller's decimate does: the edge whose two corners can become one with
 * the least change to the shape, measured by the planes of the triangles round them as Garland and Heckbert measure
 * it, is folded first, one corner moved onto the other, again and again until few enough are left. A corner moves onto
 * another rather than between them, so it keeps its place on the picture and its normal; the edges of a mesh's holes
 * and of its pictures' seams stay where they are; and no fold turns a triangle over or joins what was apart. A mesh
 * that bends with a skin or moves toward morph targets is left as it is. Pure.
 */
public final class MeshReducer {

    /* How much more an edge of a hole or a seam weighs than the shape inside, so it is the last to go. */
    private static final double BORDER = 1000.0;
    /* How far a triangle's normal may turn in a fold, as the cosine of the angle. */
    private static final double TURN = 0.2;
    private static final int QUADRIC = 10;

    private MeshReducer() {
    }

    /** {@code mesh} with {@code triangles} triangles or fewer, or as close as folding gets it. */
    public static ModelMesh reduce(final ModelMesh mesh, final int triangles) {
        if (mesh.triangles().length / 3 <= triangles || mesh.skinned() || !mesh.morphs().isEmpty()) {
            return mesh;
        }
        return new Folding(mesh).fold(Math.max(1, triangles));
    }

    /** How many triangles a mesh has. */
    public static int triangles(final ModelMesh mesh) {
        return mesh.triangles().length / 3;
    }

    /* The work of reducing one mesh: its corners' planes, which triangles each corner is in, and the folds to try. */
    private static final class Folding {

        private final ModelMesh mesh;
        private final float[] points;
        private final int[] corners;
        private final boolean[] gone;
        private final boolean[] dead;
        private final int[] stamps;
        private final double[] planes;
        private final int[][] around;
        private final int[] aroundCount;
        private final PriorityQueue<Fold> folds = new PriorityQueue<>();
        private int left;

        private Folding(final ModelMesh mesh) {
            this.mesh = mesh;
            this.points = mesh.positions();
            this.corners = mesh.triangles().clone();
            final int count = this.points.length / 3;
            this.gone = new boolean[this.corners.length / 3];
            this.dead = new boolean[count];
            this.stamps = new int[count];
            this.planes = new double[count * QUADRIC];
            this.around = new int[count][];
            this.aroundCount = new int[count];
            this.left = this.corners.length / 3;
        }

        private ModelMesh fold(final int wanted) {
            gather();
            weighBorders();
            final Set<Long> seen = new HashSet<>();
            for (int t = 0; t < this.gone.length; t++) {
                for (int k = 0; k < 3; k++) {
                    final int a = this.corners[t * 3 + k];
                    final int b = this.corners[t * 3 + (k + 1) % 3];
                    if (seen.add(key(a, b))) {
                        offer(a, b);
                    }
                }
            }
            while (this.left > wanted && !this.folds.isEmpty()) {
                final Fold next = this.folds.poll();
                if (this.dead[next.from] || this.dead[next.to] || this.stamps[next.from] != next.fromStamp
                        || this.stamps[next.to] != next.toStamp) {
                    continue;
                }
                if (allowed(next.from, next.to)) {
                    collapse(next.from, next.to);
                }
            }
            return kept();
        }

        /* Which triangles each corner is in, and the planes of the triangles round each corner, by their area. */
        private void gather() {
            for (int i = 0; i < this.around.length; i++) {
                this.around[i] = new int[4];
            }
            for (int t = 0; t < this.gone.length; t++) {
                final int a = this.corners[t * 3];
                final int b = this.corners[t * 3 + 1];
                final int c = this.corners[t * 3 + 2];
                if (a == b || b == c || a == c) {
                    // A triangle with two corners alike has no area and no plane; it goes.
                    this.gone[t] = true;
                    this.left--;
                    continue;
                }
                for (final int corner : new int[] {a, b, c}) {
                    join(corner, t);
                }
                final double[] normal = normal(a, b, c);
                final double length = Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
                if (length == 0) {
                    continue;
                }
                final double nx = normal[0] / length;
                final double ny = normal[1] / length;
                final double nz = normal[2] / length;
                final double d = -(nx * this.points[a * 3] + ny * this.points[a * 3 + 1] + nz * this.points[a * 3 + 2]);
                for (final int corner : new int[] {a, b, c}) {
                    add(corner, nx, ny, nz, d, length);
                }
            }
        }

        /* The edges only one triangle has (a hole's, a seam's) weigh much more, by a plane standing up along them. */
        private void weighBorders() {
            final Map<Long, Integer> uses = new HashMap<>();
            for (int t = 0; t < this.gone.length; t++) {
                if (this.gone[t]) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    uses.merge(key(this.corners[t * 3 + k], this.corners[t * 3 + (k + 1) % 3]), 1, Integer::sum);
                }
            }
            for (int t = 0; t < this.gone.length; t++) {
                if (this.gone[t]) {
                    continue;
                }
                final int a = this.corners[t * 3];
                final double[] face = normal(a, this.corners[t * 3 + 1], this.corners[t * 3 + 2]);
                for (int k = 0; k < 3; k++) {
                    final int from = this.corners[t * 3 + k];
                    final int to = this.corners[t * 3 + (k + 1) % 3];
                    if (uses.get(key(from, to)) != 1) {
                        continue;
                    }
                    final double ex = this.points[to * 3] - this.points[from * 3];
                    final double ey = this.points[to * 3 + 1] - this.points[from * 3 + 1];
                    final double ez = this.points[to * 3 + 2] - this.points[from * 3 + 2];
                    double nx = ey * face[2] - ez * face[1];
                    double ny = ez * face[0] - ex * face[2];
                    double nz = ex * face[1] - ey * face[0];
                    final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (length == 0) {
                        continue;
                    }
                    nx /= length;
                    ny /= length;
                    nz /= length;
                    final double d = -(nx * this.points[from * 3] + ny * this.points[from * 3 + 1]
                            + nz * this.points[from * 3 + 2]);
                    final double weight = BORDER * (ex * ex + ey * ey + ez * ez);
                    add(from, nx, ny, nz, d, weight);
                    add(to, nx, ny, nz, d, weight);
                }
            }
        }

        /* Offers the fold of the edge between {@code a} and {@code b}, whichever way changes the shape less. */
        private void offer(final int a, final int b) {
            final double toB = cost(a, b, b);
            final double toA = cost(a, b, a);
            if (toB <= toA) {
                this.folds.add(new Fold(toB, a, b, this.stamps[a], this.stamps[b]));
            } else {
                this.folds.add(new Fold(toA, b, a, this.stamps[b], this.stamps[a]));
            }
        }

        /* How far from the planes of both corners the corner {@code at} lies, as the sum of their squares. */
        private double cost(final int a, final int b, final int at) {
            final double x = this.points[at * 3];
            final double y = this.points[at * 3 + 1];
            final double z = this.points[at * 3 + 2];
            final int qa = a * QUADRIC;
            final int qb = b * QUADRIC;
            final double[] q = this.planes;
            return (q[qa] + q[qb]) * x * x + 2 * (q[qa + 1] + q[qb + 1]) * x * y + 2 * (q[qa + 2] + q[qb + 2]) * x * z
                    + 2 * (q[qa + 3] + q[qb + 3]) * x + (q[qa + 4] + q[qb + 4]) * y * y
                    + 2 * (q[qa + 5] + q[qb + 5]) * y * z + 2 * (q[qa + 6] + q[qb + 6]) * y
                    + (q[qa + 7] + q[qb + 7]) * z * z + 2 * (q[qa + 8] + q[qb + 8]) * z + q[qa + 9] + q[qb + 9];
        }

        /*
         * Whether {@code from} may move onto {@code to}: the corners both are joined to are only the third corners of
         * the triangles they share, so nothing apart is joined; and no triangle left turns over.
         */
        private boolean allowed(final int from, final int to) {
            final Set<Integer> shared = new HashSet<>();
            final Set<Integer> fromNeighbours = new HashSet<>();
            for (int i = 0; i < this.aroundCount[from]; i++) {
                final int t = this.around[from][i];
                if (this.gone[t]) {
                    continue;
                }
                boolean hasTo = false;
                for (int k = 0; k < 3; k++) {
                    final int corner = this.corners[t * 3 + k];
                    hasTo |= corner == to;
                    if (corner != from) {
                        fromNeighbours.add(corner);
                    }
                }
                if (hasTo) {
                    for (int k = 0; k < 3; k++) {
                        final int corner = this.corners[t * 3 + k];
                        if (corner != from && corner != to) {
                            shared.add(corner);
                        }
                    }
                } else if (turnsOver(t, from, to)) {
                    return false;
                }
            }
            for (int i = 0; i < this.aroundCount[to]; i++) {
                final int t = this.around[to][i];
                if (this.gone[t]) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    final int corner = this.corners[t * 3 + k];
                    if (corner != to && corner != from && fromNeighbours.contains(corner) && !shared.contains(corner)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /* Whether triangle {@code t} turns over, or comes to nothing, as {@code from} moves onto {@code to}. */
        private boolean turnsOver(final int t, final int from, final int to) {
            final int a = this.corners[t * 3];
            final int b = this.corners[t * 3 + 1];
            final int c = this.corners[t * 3 + 2];
            final double[] before = normal(a, b, c);
            final double[] after = normal(a == from ? to : a, b == from ? to : b, c == from ? to : c);
            final double lengths = Math.sqrt((before[0] * before[0] + before[1] * before[1] + before[2] * before[2])
                    * (after[0] * after[0] + after[1] * after[1] + after[2] * after[2]));
            if (lengths == 0) {
                return true;
            }
            return (before[0] * after[0] + before[1] * after[1] + before[2] * after[2]) / lengths < TURN;
        }

        /* Moves {@code from} onto {@code to}: the triangles both are in go, the others take {@code to}. */
        private void collapse(final int from, final int to) {
            for (int i = 0; i < this.aroundCount[from]; i++) {
                final int t = this.around[from][i];
                if (this.gone[t]) {
                    continue;
                }
                boolean hasTo = false;
                for (int k = 0; k < 3; k++) {
                    hasTo |= this.corners[t * 3 + k] == to;
                }
                if (hasTo) {
                    this.gone[t] = true;
                    this.left--;
                } else {
                    for (int k = 0; k < 3; k++) {
                        if (this.corners[t * 3 + k] == from) {
                            this.corners[t * 3 + k] = to;
                        }
                    }
                    join(to, t);
                }
            }
            this.dead[from] = true;
            this.stamps[from]++;
            this.stamps[to]++;
            for (int k = 0; k < QUADRIC; k++) {
                this.planes[to * QUADRIC + k] += this.planes[from * QUADRIC + k];
            }
            final Set<Integer> neighbours = new HashSet<>();
            for (int i = 0; i < this.aroundCount[to]; i++) {
                final int t = this.around[to][i];
                if (this.gone[t]) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    final int corner = this.corners[t * 3 + k];
                    if (corner != to) {
                        neighbours.add(corner);
                    }
                }
            }
            for (final int neighbour : neighbours) {
                offer(to, neighbour);
            }
        }

        /* The mesh of what is left, its corners counted again from 0. */
        private ModelMesh kept() {
            final int[] renumbered = new int[this.dead.length];
            Arrays.fill(renumbered, -1);
            int corners = 0;
            int triangles = 0;
            for (int t = 0; t < this.gone.length; t++) {
                if (this.gone[t]) {
                    continue;
                }
                triangles++;
                for (int k = 0; k < 3; k++) {
                    final int corner = this.corners[t * 3 + k];
                    if (renumbered[corner] < 0) {
                        renumbered[corner] = corners++;
                    }
                }
            }
            final float[] normals = this.mesh.normals();
            final float[] uvs = this.mesh.uvs();
            final boolean withNormals = normals.length == this.points.length;
            final boolean withUvs = uvs.length == this.points.length / 3 * 2;
            final float[] positions = new float[corners * 3];
            final float[] keptNormals = new float[withNormals ? corners * 3 : 0];
            final float[] keptUvs = new float[withUvs ? corners * 2 : 0];
            for (int old = 0; old < renumbered.length; old++) {
                final int now = renumbered[old];
                if (now < 0) {
                    continue;
                }
                System.arraycopy(this.points, old * 3, positions, now * 3, 3);
                if (withNormals) {
                    System.arraycopy(normals, old * 3, keptNormals, now * 3, 3);
                }
                if (withUvs) {
                    System.arraycopy(uvs, old * 2, keptUvs, now * 2, 2);
                }
            }
            final int[] order = new int[triangles * 3];
            int at = 0;
            for (int t = 0; t < this.gone.length; t++) {
                if (!this.gone[t]) {
                    for (int k = 0; k < 3; k++) {
                        order[at++] = renumbered[this.corners[t * 3 + k]];
                    }
                }
            }
            return new ModelMesh(positions, keptNormals, keptUvs, order, new int[0], new float[0],
                    this.mesh.morphs(), this.mesh.texture());
        }

        private void join(final int corner, final int triangle) {
            if (this.aroundCount[corner] == this.around[corner].length) {
                this.around[corner] = Arrays.copyOf(this.around[corner], this.around[corner].length * 2);
            }
            this.around[corner][this.aroundCount[corner]++] = triangle;
        }

        private void add(final int corner, final double nx, final double ny, final double nz, final double d,
                         final double weight) {
            final int q = corner * QUADRIC;
            this.planes[q] += weight * nx * nx;
            this.planes[q + 1] += weight * nx * ny;
            this.planes[q + 2] += weight * nx * nz;
            this.planes[q + 3] += weight * nx * d;
            this.planes[q + 4] += weight * ny * ny;
            this.planes[q + 5] += weight * ny * nz;
            this.planes[q + 6] += weight * ny * d;
            this.planes[q + 7] += weight * nz * nz;
            this.planes[q + 8] += weight * nz * d;
            this.planes[q + 9] += weight * d * d;
        }

        /* The normal of the triangle {@code a, b, c}, as long as twice its area. */
        private double[] normal(final int a, final int b, final int c) {
            final double ux = this.points[b * 3] - this.points[a * 3];
            final double uy = this.points[b * 3 + 1] - this.points[a * 3 + 1];
            final double uz = this.points[b * 3 + 2] - this.points[a * 3 + 2];
            final double vx = this.points[c * 3] - this.points[a * 3];
            final double vy = this.points[c * 3 + 1] - this.points[a * 3 + 1];
            final double vz = this.points[c * 3 + 2] - this.points[a * 3 + 2];
            return new double[] {uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
        }

        private static long key(final int a, final int b) {
            return a < b ? (long) a << Integer.SIZE | b : (long) b << Integer.SIZE | a;
        }
    }

    /* A fold to try: moving one corner onto another, what it costs, and the stamps both had when it was offered. */
    private record Fold(double cost, int from, int to, int fromStamp, int toStamp) implements Comparable<Fold> {

        @Override
        public int compareTo(final Fold other) {
            return Double.compare(this.cost, other.cost);
        }
    }
}
