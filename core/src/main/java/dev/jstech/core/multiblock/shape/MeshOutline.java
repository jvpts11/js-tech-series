/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The outline of a block of a formed structure, drawn from its faces as a player sees them: the sharp edges, where
 * the faces meeting turn by more than {@link #SHARP_DEGREES}, and the edges where the faces end, at the open side of a
 * wall or where the block's own walls cut them. Edges of faces lying flat side by side are left out however the model
 * is cut into triangles, even where a triangle's corner sits halfway along its neighbour's edge. Pure, so it is
 * tested without the game.
 */
public final class MeshOutline {

    /** How far two faces meeting along an edge must turn, in degrees, for the edge to be drawn. */
    public static final double SHARP_DEGREES = 30;
    /** How many numbers each line has: where it starts, where it ends. */
    public static final int LINE = 6;
    /* How finely an edge's line and its ends are told apart, in blocks: the rounding of a float, and more. */
    private static final double GRID = 1.0e-4;
    private static final double TURN = Math.cos(Math.toRadians(SHARP_DEGREES));
    private static final int TRIANGLE = 9;
    private static final int AXES = 3;

    private MeshOutline() {
    }

    /**
     * The outline of the faces {@code triangles}.
     *
     * @param triangles nine numbers to a triangle, in the block's own space
     * @return {@link #LINE} numbers to a line
     */
    public static float[] lines(final float[] triangles) {
        final Map<Line, List<Span>> lines = new HashMap<>();
        final double[][] corner = new double[3][AXES];
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
            for (int e = 0; e < 3; e++) {
                edge(corner[e], corner[(e + 1) % 3], corner[(e + 2) % 3], normal, lines);
            }
        }
        final List<double[]> out = new ArrayList<>();
        lines.forEach((line, spans) -> drawn(line, spans, out));
        out.sort((a, b) -> {
            for (int i = 0; i < LINE; i++) {
                final int c = Double.compare(a[i], b[i]);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        });
        final float[] flat = new float[out.size() * LINE];
        for (int i = 0; i < out.size(); i++) {
            for (int k = 0; k < LINE; k++) {
                flat[i * LINE + k] = (float) out.get(i)[k];
            }
        }
        return flat;
    }

    /*
     * The edge from {@code a} to {@code b} of a face turned {@code normal} whose third corner is {@code c}, filed by
     * the line it lies on.
     */
    private static void edge(final double[] a, final double[] b, final double[] c, final double[] normal,
                             final Map<Line, List<Span>> lines) {
        final double[] way = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        final double length = Math.sqrt(way[0] * way[0] + way[1] * way[1] + way[2] * way[2]);
        if (length < GRID) {
            return;
        }
        for (int axis = 0; axis < AXES; axis++) {
            way[axis] /= length;
        }
        // The same line whichever way it runs: its way points to the positive side of its largest axis.
        int largest = 0;
        for (int axis = 1; axis < AXES; axis++) {
            if (Math.abs(way[axis]) > Math.abs(way[largest])) {
                largest = axis;
            }
        }
        if (way[largest] < 0) {
            for (int axis = 0; axis < AXES; axis++) {
                way[axis] = -way[axis];
            }
        }
        final double along = a[0] * way[0] + a[1] * way[1] + a[2] * way[2];
        final double[] foot = {a[0] - way[0] * along, a[1] - way[1] * along, a[2] - way[2] * along};
        final double from = along;
        final double to = b[0] * way[0] + b[1] * way[1] + b[2] * way[2];
        final Line line = new Line(round(way[0]), round(way[1]), round(way[2]), round(foot[0]), round(foot[1]),
                round(foot[2]));
        // Into the face from its edge, across the line, toward its third corner.
        final double third = c[0] * way[0] + c[1] * way[1] + c[2] * way[2];
        final double[] inward = {c[0] - foot[0] - way[0] * third, c[1] - foot[1] - way[1] * third,
            c[2] - foot[2] - way[2] * third};
        final double reach = Math.sqrt(inward[0] * inward[0] + inward[1] * inward[1] + inward[2] * inward[2]);
        if (reach < GRID) {
            return;
        }
        for (int axis = 0; axis < AXES; axis++) {
            inward[axis] /= reach;
        }
        lines.computeIfAbsent(line, any -> new ArrayList<>()).add(new Span(Math.min(from, to), Math.max(from, to),
                normal, inward, way, foot));
    }

    /*
     * The stretches of one line that are drawn: where a face along it does not go on flat across it into another;
     * never where faces lie flat side by side.
     */
    private static void drawn(final Line line, final List<Span> spans, final List<double[]> out) {
        final List<Double> stops = new ArrayList<>();
        for (final Span span : spans) {
            stops.add(span.from());
            stops.add(span.to());
        }
        stops.sort(Double::compare);
        final Span any = spans.getFirst();
        double openFrom = Double.NaN;
        double openTo = Double.NaN;
        for (int i = 0; i + 1 < stops.size(); i++) {
            final double from = stops.get(i);
            final double to = stops.get(i + 1);
            if (to - from < GRID) {
                continue;
            }
            final double middle = (from + to) / 2;
            final List<Span> covering = new ArrayList<>();
            for (final Span span : spans) {
                if (span.from() <= middle && middle <= span.to()) {
                    covering.add(span);
                }
            }
            if (sharp(covering)) {
                if (!Double.isNaN(openTo) && Math.abs(openTo - from) < GRID) {
                    openTo = to;
                } else {
                    flush(any, openFrom, openTo, out);
                    openFrom = from;
                    openTo = to;
                }
            }
        }
        flush(any, openFrom, openTo, out);
    }

    /*
     * Whether the faces {@code covering} a stretch make an edge there: one of them does not go on across it into
     * another facing nearly its way, as a sheet's border, a box's corner or a face's cut end does not.
     */
    private static boolean sharp(final List<Span> covering) {
        for (final Span span : covering) {
            boolean goesOn = false;
            for (final Span other : covering) {
                if (other != span && dot(span.normal(), other.normal()) > TURN
                        && dot(span.inward(), other.inward()) < -TURN) {
                    goesOn = true;
                    break;
                }
            }
            if (!goesOn) {
                return true;
            }
        }
        return false;
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static void flush(final Span on, final double from, final double to, final List<double[]> out) {
        if (Double.isNaN(from)) {
            return;
        }
        final double[] way = on.way();
        final double[] foot = on.foot();
        out.add(new double[] {foot[0] + way[0] * from, foot[1] + way[1] * from, foot[2] + way[2] * from,
            foot[0] + way[0] * to, foot[1] + way[1] * to, foot[2] + way[2] * to});
    }

    private static long round(final double value) {
        return Math.round(value / GRID);
    }

    /* A line, by its way and the point of it nearest the block's corner, rounded so edges along it meet. */
    private record Line(long wx, long wy, long wz, long fx, long fy, long fz) {
    }

    /* A stretch of a line one face's edge covers, the way that face turns, and the way into it across the line. */
    private record Span(double from, double to, double[] normal, double[] inward, double[] way, double[] foot) {
    }
}
