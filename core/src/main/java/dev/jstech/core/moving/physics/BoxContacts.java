/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import org.joml.Vector3d;

/**
 * Where two boxes, each turned any way, meet. The separating axes, the fifteen ways one could be kept apart from the
 * other, give the way they are least deep into each other along, or nearest along when apart. Where that way is a
 * face's, the face of the other box that meets it is cut to it, and they meet at the corners of what is left: a block
 * lying on another meets it all over the face they share, at its four corners, so it rests and does not rock, however
 * far over the edge it hangs. Where the way is across two edges, they meet where the edges cross. Boxes further apart
 * than a margin do not meet; nearer, they meet ahead of time, so a body falling fast is stopped where it touches,
 * never through.
 */
public final class BoxContacts {

    private final Vector3d between = new Vector3d();
    private final Vector3d axis = new Vector3d();
    private final Vector3d best = new Vector3d();
    private final Vector3d normal = new Vector3d();
    private final Vector3d facing = new Vector3d();
    private final Vector3d middle = new Vector3d();
    private final Vector3d point = new Vector3d();
    private final Vector3d edgeA = new Vector3d();
    private final Vector3d edgeB = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    /* The face being cut, as up to eight corners, and what is left of it after each cut. */
    private final Vector3d[] polygon = corners();
    private final Vector3d[] cut = corners();
    private int bestKind;
    private int bestFirst;
    private int bestSecond;

    /** The feature of the point taken where two edges cross. */
    public static final int EDGES = 16;
    /* How the way they are kept apart along was found: by a face of a, a face of b, or two edges. */
    private static final int FACE_OF_A = 0;
    private static final int FACE_OF_B = 1;
    private static final int ACROSS_EDGES = 2;
    /* A way across two edges shorter than this, the edges side by side, keeps nothing apart a face does not. */
    private static final double PARALLEL = 1.0e-6;
    /* An edge's way is taken over a face's only when it keeps them further apart by this: faces meet more steadily. */
    private static final double FACE_FIRST = 0.02;
    private static final double TIE = 1.0e-3;
    private static final int MOST = 8;

    /**
     * Where box {@code a} meets box {@code b}, no further apart than {@code margin}: each point to {@code sink}, with
     * the way pointing from {@code b} to {@code a}, how far apart they are there (less than nought when into each
     * other), and which it is, from 0 to 7 of a face of {@code b}, 8 to 15 of a face of {@code a}, or
     * {@link #EDGES}. How many points it found; none when they are apart.
     */
    public int collide(final OrientedBox a, final OrientedBox b, final double margin, final IPointSink sink) {
        between.set(b.centre()).sub(a.centre());
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 3; i++) {
            final double apart = apart(a, b, a.axis(i));
            if (apart > margin) {
                return 0;
            }
            if (apart > deepest) {
                deepest = apart;
                take(FACE_OF_A, i, 0);
            }
        }
        for (int i = 0; i < 3; i++) {
            final double apart = apart(a, b, b.axis(i));
            if (apart > margin) {
                return 0;
            }
            // A face of b, the ground or what a rests on, is taken over a's as near as makes no difference.
            if (apart > (bestKind == FACE_OF_A ? deepest - TIE : deepest)) {
                deepest = apart;
                take(FACE_OF_B, i, 0);
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                a.axis(i).cross(b.axis(j), axis);
                final double length = axis.length();
                if (length < PARALLEL) {
                    continue;
                }
                axis.div(length);
                final double apart = apart(a, b, axis);
                if (apart > margin) {
                    return 0;
                }
                if (apart > deepest + FACE_FIRST) {
                    deepest = apart;
                    take(ACROSS_EDGES, i, j);
                }
            }
        }
        // The way from b to a.
        normal.set(best);
        if (normal.dot(between) > 0) {
            normal.negate();
        }
        if (bestKind == FACE_OF_B) {
            return faces(b, bestFirst, facing.set(normal), a, margin, 0, sink);
        }
        if (bestKind == FACE_OF_A) {
            return faces(a, bestFirst, facing.set(normal).negate(), b, margin, MOST, sink);
        }
        edges(a, b, deepest, sink);
        return 1;
    }

    /*
     * The face of {@code reference}, on its axis {@code index} and looking {@code out}, towards {@code incident},
     * meets the face of {@code incident} looking most against it: that face is cut to the reference face's sides,
     * and each corner left within the margin is a point.
     */
    private int faces(final OrientedBox reference, final int index, final Vector3d out, final OrientedBox incident,
                      final double margin, final int firstFeature, final IPointSink sink) {
        // The reference face: its middle, and its two sides' axes.
        middle.set(reference.centre()).fma(reference.half(index), out);
        final int u = (index + 1) % 3;
        final int v = (index + 2) % 3;
        // The incident face: the one of the incident box looking most against the reference face.
        int most = 0;
        double mostDot = 0;
        for (int i = 0; i < 3; i++) {
            final double dot = Math.abs(incident.axis(i).dot(out));
            if (dot > mostDot) {
                mostDot = dot;
                most = i;
            }
        }
        final double sign = incident.axis(most).dot(out) > 0 ? -1 : 1;
        final int p = (most + 1) % 3;
        final int q = (most + 2) % 3;
        scratch.set(incident.centre()).fma(sign * incident.half(most), incident.axis(most));
        int count = 0;
        for (int corner = 0; corner < 4; corner++) {
            final double sp = corner == 0 || corner == 3 ? -1 : 1;
            final double sq = corner < 2 ? -1 : 1;
            polygon[count++].set(scratch).fma(sp * incident.half(p), incident.axis(p))
                    .fma(sq * incident.half(q), incident.axis(q));
        }
        count = clip(count, reference.axis(u), reference.half(u));
        count = clip(count, reference.axis(v), reference.half(v));
        int found = 0;
        for (int i = 0; i < count; i++) {
            final double apart = scratch.set(polygon[i]).sub(middle).dot(out);
            if (apart > margin) {
                continue;
            }
            // Halfway between the two faces, and the way from b to a.
            point.set(polygon[i]).fma(-apart / 2, out);
            sink.point(point, normal, apart, firstFeature + i);
            found++;
        }
        return found;
    }

    /*
     * Cuts the polygon to the slab of the reference face's side along {@code side}, as far as {@code reach} from its
     * middle on each hand: corners outside go, and where an edge leaves, a corner is made on the side.
     */
    private int clip(final int count, final Vector3d side, final double reach) {
        int kept = clipPlane(count, side, reach, 1);
        kept = clipPlane(kept, side, reach, -1);
        return kept;
    }

    private int clipPlane(final int count, final Vector3d side, final double reach, final double hand) {
        if (count == 0) {
            return 0;
        }
        int kept = 0;
        for (int i = 0; i < count; i++) {
            final Vector3d from = polygon[i];
            final Vector3d to = polygon[(i + 1) % count];
            final double fromOut = hand * scratch.set(from).sub(middle).dot(side) - reach;
            final double toOut = hand * scratch.set(to).sub(middle).dot(side) - reach;
            if (fromOut <= 0 && kept < MOST) {
                cut[kept++].set(from);
            }
            if ((fromOut <= 0) != (toOut <= 0) && kept < MOST) {
                final double share = fromOut / (fromOut - toOut);
                cut[kept++].set(to).sub(from).mul(share).add(from);
            }
        }
        for (int i = 0; i < kept; i++) {
            polygon[i].set(cut[i]);
        }
        return kept;
    }

    /* Two edges crossing: the point halfway between them where they come nearest. */
    private void edges(final OrientedBox a, final OrientedBox b, final double deepest, final IPointSink sink) {
        // Each box's edge along its axis, on its side towards the other.
        edgeA.set(a.centre());
        for (int k = 0; k < 3; k++) {
            if (k != bestFirst) {
                edgeA.fma(a.axis(k).dot(normal) > 0 ? -a.half(k) : a.half(k), a.axis(k));
            }
        }
        edgeB.set(b.centre());
        for (int k = 0; k < 3; k++) {
            if (k != bestSecond) {
                edgeB.fma(b.axis(k).dot(normal) > 0 ? b.half(k) : -b.half(k), b.axis(k));
            }
        }
        final Vector3d alongA = a.axis(bestFirst);
        final Vector3d alongB = b.axis(bestSecond);
        scratch.set(edgeB).sub(edgeA);
        final double ab = alongA.dot(alongB);
        final double da = alongA.dot(scratch);
        final double db = alongB.dot(scratch);
        final double under = 1 - ab * ab;
        double ta = under > PARALLEL ? (da - ab * db) / under : 0;
        ta = Math.max(-a.half(bestFirst), Math.min(a.half(bestFirst), ta));
        double tb = ta * ab - db;
        tb = Math.max(-b.half(bestSecond), Math.min(b.half(bestSecond), tb));
        point.set(edgeA).fma(ta, alongA);
        scratch.set(edgeB).fma(tb, alongB);
        point.add(scratch).mul(0.5);
        sink.point(point, normal, deepest, EDGES);
    }

    /* How far apart a and b are along {@code way}: less than nought when into each other. */
    private double apart(final OrientedBox a, final OrientedBox b, final Vector3d way) {
        axis.set(way);
        return Math.abs(between.dot(way)) - a.reach(way) - b.reach(way);
    }

    private void take(final int kind, final int first, final int second) {
        best.set(axis);
        bestKind = kind;
        bestFirst = first;
        bestSecond = second;
    }

    private static Vector3d[] corners() {
        final Vector3d[] corners = new Vector3d[MOST];
        for (int i = 0; i < MOST; i++) {
            corners[i] = new Vector3d();
        }
        return corners;
    }

    /** Takes each point two boxes meet at. */
    @FunctionalInterface
    public interface IPointSink {

        /**
         * A point: where, the way from the second box to the first, how far apart they are there, and which it is.
         * The vectors are reused; copy what is kept.
         */
        void point(Vector3d at, Vector3d way, double apart, int feature);
    }
}
