/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The way an electric arc takes between two points: the straight line split again and again, each new middle pushed
 * aside at random by a share of its piece's length, so it zigzags as lightning does, ending where it must; and the
 * branches it throws off part of the way along, shorter and fainter. A new seed each few ticks makes it flicker. Pure.
 */
public final class ArcPath {

    /** How many times the line is split: 2 to the power of this many pieces. */
    public static final int SPLITS = 5;
    /* How long a branch is, a share of the arc, and how far it turns off. */
    private static final double BRANCH_LENGTH = 0.35;
    private static final double BRANCH_TURN = 0.6;
    private static final double LEAST = 1e-9;

    private ArcPath() {
    }

    /** The points of the arc from {@code from} to {@code to} (x y z), straying by {@code jagged}, for {@code seed}. */
    public static List<double[]> of(final double[] from, final double[] to, final double jagged, final long seed) {
        final SplittableRandom random = new SplittableRandom(seed);
        List<double[]> points = new ArrayList<>(List.of(from.clone(), to.clone()));
        double stray = jagged * distance(from, to);
        for (int split = 0; split < SPLITS; split++) {
            final List<double[]> next = new ArrayList<>(points.size() * 2);
            for (int i = 0; i < points.size() - 1; i++) {
                final double[] a = points.get(i);
                final double[] b = points.get(i + 1);
                next.add(a);
                next.add(push(middle(a, b), aside(a, b, random), stray * (random.nextDouble() * 2 - 1)));
            }
            next.add(points.getLast());
            points = next;
            stray /= 2;
        }
        return List.copyOf(points);
    }

    /**
     * {@code count} branches off {@code arc}, each from a point of it, turning off its way and as long as a third of
     * it, jagged as it is.
     */
    public static List<List<double[]>> branches(final List<double[]> arc, final int count, final double jagged,
                                                final long seed) {
        final SplittableRandom random = new SplittableRandom(seed ^ Long.MAX_VALUE);
        final List<List<double[]>> out = new ArrayList<>();
        final double length = distance(arc.getFirst(), arc.getLast()) * BRANCH_LENGTH;
        for (int b = 0; b < count && arc.size() > 2; b++) {
            final int at = 1 + random.nextInt(arc.size() - 2);
            final double[] start = arc.get(at);
            final double[] way = unit(minus(arc.get(at + 1), start));
            final double[] off = aside(start, arc.get(at + 1), random);
            final double[] end = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                end[axis] = start[axis] + (way[axis] + off[axis] * BRANCH_TURN) * length;
            }
            out.add(of(start, end, jagged, random.nextLong()));
        }
        return List.copyOf(out);
    }

    private static double[] middle(final double[] a, final double[] b) {
        return new double[] {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
    }

    private static double[] push(final double[] at, final double[] way, final double by) {
        return new double[] {at[0] + way[0] * by, at[1] + way[1] * by, at[2] + way[2] * by};
    }

    /* A unit vector at right angles to the piece from {@code a} to {@code b}, turned at random about it. */
    private static double[] aside(final double[] a, final double[] b, final SplittableRandom random) {
        final double[] along = unit(minus(b, a));
        final double[] other = Math.abs(along[1]) < 0.9 ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
        final double[] first = unit(cross(along, other));
        final double[] second = cross(along, first);
        final double turn = 2 * Math.PI * random.nextDouble();
        return new double[] {first[0] * Math.cos(turn) + second[0] * Math.sin(turn),
            first[1] * Math.cos(turn) + second[1] * Math.sin(turn),
            first[2] * Math.cos(turn) + second[2] * Math.sin(turn)};
    }

    private static double[] minus(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(final double[] v) {
        final double length = Math.max(LEAST, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    private static double distance(final double[] a, final double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2])
                * (a[2] - b[2]));
    }
}
