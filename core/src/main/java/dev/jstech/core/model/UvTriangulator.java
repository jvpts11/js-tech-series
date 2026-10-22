/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lays triangles over a region of the plane bounded by rings, as a face of a solid is laid out on its surface's
 * parameters: a Delaunay triangulation of the rings' corners and of points inside, built a point at a time as Bowyer
 * and Watson do, the triangle a point falls in found by walking from the last one made and only the triangles round it
 * opened; each ring's edges kept by splitting an edge the triangulation misses at its middle until it has it; and only
 * the triangles inside kept, by the even-odd rule, so holes and islands are told apart whichever way their rings run.
 * Pure.
 */
final class UvTriangulator {

    private final PointSet points;
    private final IntList sources = new IntList();
    private final double epsilon;
    /* Each triangle's corners, counter-clockwise; across its edge k, from corner k to k + 1, its neighbour. */
    private int[] corners = new int[FIRST * 3];
    private int[] across = new int[FIRST * 3];
    private double[] circles = new double[FIRST * 3];
    private boolean[] dead = new boolean[FIRST];
    private int triangles;
    private int last;

    /* How much wider than the region the first triangle is, and how many times a missing edge is split. */
    private static final double SUPER = 64.0;
    private static final int MOST_ROUNDS = 10;
    private static final int MOST_POINTS = 20_000;
    private static final double CELLS = 4.0;
    private static final int FIRST = 256;
    private static final int NONE = -1;
    /* How many steps a walk takes at most before the triangle is looked for among all: a few more than it should. */
    private static final int WALK = 64;
    private static final double WALK_SPREAD = 8.0;

    private UvTriangulator(final double epsilon) {
        this.epsilon = epsilon;
        this.points = new PointSet(Math.max(epsilon * CELLS, 1e-12));
    }

    /**
     * The triangles of the region {@code rings} bound, each ring its corners as x, y pairs, closed from its last to
     * its first; with {@code inside} besides, points within, as x, y pairs, any of which outside is dropped.
     *
     * @param epsilon how close two points are to be one
     */
    static Result triangulate(final List<double[]> rings, final double[] inside, final double epsilon) {
        return new UvTriangulator(epsilon).run(rings, inside);
    }

    /** Whether a point is inside the region: within an odd number of its rings. */
    static boolean inRegion(final List<double[]> rings, final double x, final double y) {
        boolean inside = false;
        for (final double[] ring : rings) {
            final int count = ring.length / 2;
            for (int i = 0, j = count - 1; i < count; j = i++) {
                final double xi = ring[i * 2];
                final double yi = ring[i * 2 + 1];
                final double xj = ring[j * 2];
                final double yj = ring[j * 2 + 1];
                if (yi > y != yj > y && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private Result run(final List<double[]> rings, final double[] inside) {
        double lowX = Double.MAX_VALUE;
        double lowY = Double.MAX_VALUE;
        double highX = -Double.MAX_VALUE;
        double highY = -Double.MAX_VALUE;
        for (final double[] ring : rings) {
            for (int i = 0; i + 1 < ring.length; i += 2) {
                lowX = Math.min(lowX, ring[i]);
                highX = Math.max(highX, ring[i]);
                lowY = Math.min(lowY, ring[i + 1]);
                highY = Math.max(highY, ring[i + 1]);
            }
        }
        if (lowX > highX) {
            return new Result(new double[0], new int[0], new int[0]);
        }
        final double size = Math.max(highX - lowX, highY - lowY) + this.epsilon;
        final double middleX = (lowX + highX) / 2;
        final double middleY = (lowY + highY) / 2;
        // The first triangle holds every point; its corners are 0, 1 and 2, left out at the end.
        this.points.add(middleX - SUPER * size, middleY - SUPER * size);
        this.points.add(middleX + SUPER * size, middleY - SUPER * size);
        this.points.add(middleX, middleY + SUPER * size);
        for (int i = 0; i < 3; i++) {
            this.sources.add(NONE);
        }
        make(0, 1, 2, NONE, NONE, NONE);
        final List<int[]> edges = new ArrayList<>();
        int source = 0;
        for (final double[] ring : rings) {
            final int count = ring.length / 2;
            if (count < 3) {
                source += count;
                continue;
            }
            int first = NONE;
            int previous = NONE;
            for (int i = 0; i < count; i++) {
                final int index = insert(ring[i * 2], ring[i * 2 + 1], source++);
                if (first < 0) {
                    first = index;
                } else if (index != previous) {
                    edges.add(new int[] {previous, index});
                }
                previous = index;
            }
            if (previous != first) {
                edges.add(new int[] {previous, first});
            }
        }
        for (int i = 0; i + 1 < inside.length; i += 2) {
            if (inRegion(rings, inside[i], inside[i + 1])) {
                insert(inside[i], inside[i + 1], NONE);
            }
        }
        conform(edges);
        return result(rings);
    }

    /* Splits every ring edge the triangulation misses at its middle, until it has them all or has split enough. */
    private void conform(final List<int[]> edges) {
        List<int[]> wanted = edges;
        for (int round = 0; round < MOST_ROUNDS && this.points.size() < MOST_POINTS; round++) {
            final Set<Long> have = new HashSet<>();
            for (int t = 0; t < this.triangles; t++) {
                if (!this.dead[t]) {
                    for (int k = 0; k < 3; k++) {
                        have.add(key(this.corners[t * 3 + k], this.corners[t * 3 + (k + 1) % 3]));
                    }
                }
            }
            final List<int[]> next = new ArrayList<>();
            boolean missing = false;
            for (final int[] edge : wanted) {
                if (have.contains(key(edge[0], edge[1]))) {
                    next.add(edge);
                    continue;
                }
                missing = true;
                final int middle = insert((this.points.x(edge[0]) + this.points.x(edge[1])) / 2,
                        (this.points.y(edge[0]) + this.points.y(edge[1])) / 2, NONE);
                next.add(new int[] {edge[0], middle});
                next.add(new int[] {middle, edge[1]});
            }
            wanted = next;
            if (!missing) {
                return;
            }
        }
    }

    /*
     * Puts a point in the triangulation: the triangle it falls in found by walking, every triangle round it whose
     * circle holds it opened, and the hole filled with triangles to it. Its index, or that of one already there.
     *
     * @param source the ring corner it is, counted over every ring, or -1 for one made here
     */
    private int insert(final double x, final double y, final int source) {
        final int known = this.points.find(x, y, this.epsilon);
        if (known >= 0) {
            return known;
        }
        final int index = this.points.size();
        this.points.add(x, y);
        this.sources.add(source);
        final int start = locate(x, y);
        // The cavity: the triangles whose circles hold the point, joined to the one it falls in.
        final List<Integer> cavity = new ArrayList<>();
        final ArrayDeque<Integer> open = new ArrayDeque<>();
        final Set<Integer> seen = new HashSet<>();
        open.add(start);
        seen.add(start);
        while (!open.isEmpty()) {
            final int t = open.poll();
            if (t != start && !holds(t, x, y)) {
                continue;
            }
            cavity.add(t);
            for (int k = 0; k < 3; k++) {
                final int neighbour = this.across[t * 3 + k];
                if (neighbour != NONE && seen.add(neighbour)) {
                    open.add(neighbour);
                }
            }
        }
        final Set<Integer> inCavity = new HashSet<>(cavity);
        // Its rim: each edge of the cavity with a triangle outside, or none, beyond it.
        final List<int[]> rim = new ArrayList<>();
        for (final int t : cavity) {
            for (int k = 0; k < 3; k++) {
                final int neighbour = this.across[t * 3 + k];
                if (neighbour == NONE || !inCavity.contains(neighbour)) {
                    rim.add(new int[] {this.corners[t * 3 + k], this.corners[t * 3 + (k + 1) % 3], neighbour});
                }
            }
            this.dead[t] = true;
        }
        final Map<Integer, Integer> startingAt = new HashMap<>();
        final Map<Integer, Integer> endingAt = new HashMap<>();
        for (final int[] edge : rim) {
            final int made = make(edge[0], edge[1], index, edge[2], NONE, NONE);
            if (edge[2] != NONE) {
                point(edge[2], edge[1], edge[0], made);
            }
            startingAt.put(edge[0], made);
            endingAt.put(edge[1], made);
        }
        // Each new triangle meets the next round the point along their shared edges to it.
        for (final int[] edge : rim) {
            final int made = startingAt.get(edge[0]);
            final Integer after = startingAt.get(edge[1]);
            final Integer before = endingAt.get(edge[0]);
            this.across[made * 3 + 1] = after == null ? NONE : after;
            this.across[made * 3 + 2] = before == null ? NONE : before;
        }
        return index;
    }

    /*
     * The triangle a point falls in, walked to from the last made, a step across an edge it lies beyond, each step
     * trying the edges from another one so the walk cannot go round in a ring; past a few steps more than a walk
     * should take, looked for among them all.
     */
    private int locate(final double x, final double y) {
        int t = this.last;
        if (t == NONE || this.dead[t]) {
            t = firstAlive();
        }
        final int most = WALK + (int) (WALK_SPREAD * Math.sqrt(this.triangles));
        for (int steps = 0; steps < most; steps++) {
            boolean moved = false;
            final int from = (steps + t) % 3;
            for (int j = 0; j < 3; j++) {
                final int k = (from + j) % 3;
                final int a = this.corners[t * 3 + k];
                final int b = this.corners[t * 3 + (k + 1) % 3];
                if (turn(a, b, x, y) < 0 && this.across[t * 3 + k] != NONE) {
                    t = this.across[t * 3 + k];
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                return t;
            }
        }
        return search(x, y, t);
    }

    /* The triangle a point falls in, looked for among them all; {@code otherwise} for none, as at an edge. */
    private int search(final double x, final double y, final int otherwise) {
        for (int t = 0; t < this.triangles; t++) {
            if (this.dead[t]) {
                continue;
            }
            boolean inside = true;
            for (int k = 0; k < 3 && inside; k++) {
                inside = turn(this.corners[t * 3 + k], this.corners[t * 3 + (k + 1) % 3], x, y) >= 0;
            }
            if (inside) {
                return t;
            }
        }
        return otherwise;
    }

    private int firstAlive() {
        for (int t = this.triangles - 1; t >= 0; t--) {
            if (!this.dead[t]) {
                return t;
            }
        }
        throw new IllegalStateException("no triangle is left");
    }

    /* Which side of the line from {@code a} to {@code b} a point is on: above 0 left, below 0 right. */
    private double turn(final int a, final int b, final double x, final double y) {
        final double ax = this.points.x(a);
        final double ay = this.points.y(a);
        return (this.points.x(b) - ax) * (y - ay) - (this.points.y(b) - ay) * (x - ax);
    }

    private boolean holds(final int t, final double x, final double y) {
        final double dx = x - this.circles[t * 3];
        final double dy = y - this.circles[t * 3 + 1];
        return dx * dx + dy * dy < this.circles[t * 3 + 2] * (1 + 1e-9);
    }

    /* Makes a triangle of corners counter-clockwise, its neighbours across its three edges; its number. */
    private int make(final int a, final int b, final int c, final int acrossAB, final int acrossBC,
                     final int acrossCA) {
        if (this.triangles == this.dead.length) {
            final int grown = this.dead.length * 2;
            this.corners = Arrays.copyOf(this.corners, grown * 3);
            this.across = Arrays.copyOf(this.across, grown * 3);
            this.circles = Arrays.copyOf(this.circles, grown * 3);
            this.dead = Arrays.copyOf(this.dead, grown);
        }
        final int t = this.triangles++;
        this.corners[t * 3] = a;
        this.corners[t * 3 + 1] = b;
        this.corners[t * 3 + 2] = c;
        this.across[t * 3] = acrossAB;
        this.across[t * 3 + 1] = acrossBC;
        this.across[t * 3 + 2] = acrossCA;
        final double ax = this.points.x(a);
        final double ay = this.points.y(a);
        final double bx = this.points.x(b);
        final double by = this.points.y(b);
        final double cx = this.points.x(c);
        final double cy = this.points.y(c);
        final double d = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(d) < 1e-300) {
            this.circles[t * 3] = ax;
            this.circles[t * 3 + 1] = ay;
            this.circles[t * 3 + 2] = -1;
        } else {
            final double a2 = ax * ax + ay * ay;
            final double b2 = bx * bx + by * by;
            final double c2 = cx * cx + cy * cy;
            final double ux = (a2 * (by - cy) + b2 * (cy - ay) + c2 * (ay - by)) / d;
            final double uy = (a2 * (cx - bx) + b2 * (ax - cx) + c2 * (bx - ax)) / d;
            this.circles[t * 3] = ux;
            this.circles[t * 3 + 1] = uy;
            this.circles[t * 3 + 2] = (ax - ux) * (ax - ux) + (ay - uy) * (ay - uy);
        }
        this.dead[t] = false;
        this.last = t;
        return t;
    }

    /* Points triangle {@code t}'s edge from {@code a} to {@code b} at its new neighbour. */
    private void point(final int t, final int a, final int b, final int neighbour) {
        for (int k = 0; k < 3; k++) {
            if (this.corners[t * 3 + k] == a && this.corners[t * 3 + (k + 1) % 3] == b) {
                this.across[t * 3 + k] = neighbour;
                return;
            }
        }
    }

    /* The triangles inside the region, the first triangle's corners left out. */
    private Result result(final List<double[]> rings) {
        final IntList kept = new IntList();
        for (int t = 0; t < this.triangles; t++) {
            final int a = this.corners[t * 3];
            final int b = this.corners[t * 3 + 1];
            final int c = this.corners[t * 3 + 2];
            if (this.dead[t] || a < 3 || b < 3 || c < 3) {
                continue;
            }
            final double x = (this.points.x(a) + this.points.x(b) + this.points.x(c)) / 3;
            final double y = (this.points.y(a) + this.points.y(b) + this.points.y(c)) / 3;
            if (inRegion(rings, x, y)) {
                kept.add(a - 3);
                kept.add(b - 3);
                kept.add(c - 3);
            }
        }
        final double[] out = new double[(this.points.size() - 3) * 2];
        final int[] from = new int[this.points.size() - 3];
        for (int i = 3; i < this.points.size(); i++) {
            out[(i - 3) * 2] = this.points.x(i);
            out[(i - 3) * 2 + 1] = this.points.y(i);
            from[i - 3] = this.sources.get(i);
        }
        return new Result(out, kept.toArray(), from);
    }

    private static long key(final int a, final int b) {
        return a < b ? (long) a << Integer.SIZE | b : (long) b << Integer.SIZE | a;
    }

    /**
     * Triangles over a region.
     *
     * @param points    their corners, as x, y pairs
     * @param triangles the corners of each, three each, counter-clockwise
     * @param sources   for each corner the ring corner it is, counted over every ring, or -1 for one made here
     */
    record Result(double[] points, int[] triangles, int[] sources) {
    }

    /* The points so far, with those near each other found by the cell of a grid they fall in. */
    private static final class PointSet {

        private final List<double[]> all = new ArrayList<>();
        private final Map<Long, List<Integer>> cells = new HashMap<>();
        private final double cell;

        private PointSet(final double cell) {
            this.cell = cell;
        }

        void add(final double x, final double y) {
            this.all.add(new double[] {x, y});
            if (this.all.size() > 3) {
                this.cells.computeIfAbsent(cellOf(x, y), key -> new ArrayList<>()).add(this.all.size() - 1);
            }
        }

        /* The point within {@code epsilon} of {@code x, y}, or -1. */
        int find(final double x, final double y, final double epsilon) {
            final long cx = (long) Math.floor(x / this.cell);
            final long cy = (long) Math.floor(y / this.cell);
            for (long i = cx - 1; i <= cx + 1; i++) {
                for (long j = cy - 1; j <= cy + 1; j++) {
                    final List<Integer> near = this.cells.get(i * 73_856_093L ^ j * 19_349_663L);
                    if (near == null) {
                        continue;
                    }
                    for (final int index : near) {
                        final double[] point = this.all.get(index);
                        if (Math.abs(point[0] - x) <= epsilon && Math.abs(point[1] - y) <= epsilon) {
                            return index;
                        }
                    }
                }
            }
            return NONE;
        }

        double x(final int index) {
            return this.all.get(index)[0];
        }

        double y(final int index) {
            return this.all.get(index)[1];
        }

        int size() {
            return this.all.size();
        }

        private long cellOf(final double x, final double y) {
            final long cx = (long) Math.floor(x / this.cell);
            final long cy = (long) Math.floor(y / this.cell);
            return cx * 73_856_093L ^ cy * 19_349_663L;
        }
    }
}
