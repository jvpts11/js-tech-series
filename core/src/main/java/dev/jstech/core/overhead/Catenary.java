/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a wire hung between two insulators: it sags under its own weight, lowest midway along, by a share of
 * its length, as a real overhead wire does. The same curve is drawn, checked for what stands in its way when it is
 * hung, and touched. Pure.
 */
public final class Catenary {

    /** How far a wire sags midway along, as a share of its length. */
    public static final double SAG = 0.05;
    /** How many pieces a block of wire is drawn and checked in. */
    public static final int PIECES_A_BLOCK = 2;
    /** The fewest pieces a wire is drawn in, however short. */
    public static final int FEWEST_PIECES = 4;

    private Catenary() {
    }

    /** The point of the wire from {@code a} to {@code b} a share {@code t} along it, from 0 at {@code a} to 1. */
    public static Point at(final Point a, final Point b, final double t) {
        final double drop = sag(a, b) * 4.0 * t * (1.0 - t);
        return new Point(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t - drop,
                a.z() + (b.z() - a.z()) * t);
    }

    /** How far the wire from {@code a} to {@code b} sags at its lowest. */
    public static double sag(final Point a, final Point b) {
        return length(a, b) * SAG;
    }

    /** The wire's length, as it is counted for what it loses: the straight distance between its insulators. */
    public static double length(final Point a, final Point b) {
        final double dx = b.x() - a.x();
        final double dy = b.y() - a.y();
        final double dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** How many pieces the wire is drawn and checked in. */
    public static int pieces(final Point a, final Point b) {
        return Math.max(FEWEST_PIECES, (int) Math.ceil(length(a, b) * PIECES_A_BLOCK));
    }

    /** The points the wire passes, from {@code a} to {@code b}, one more than its pieces. */
    public static List<Point> points(final Point a, final Point b) {
        final int pieces = pieces(a, b);
        final List<Point> points = new ArrayList<>(pieces + 1);
        for (int k = 0; k <= pieces; k++) {
            points.add(at(a, b, (double) k / pieces));
        }
        return points;
    }

    /** How near {@code p} comes to the wire from {@code a} to {@code b}. */
    public static double distance(final Point p, final Point a, final Point b) {
        final List<Point> points = points(a, b);
        double nearest = Double.MAX_VALUE;
        for (int k = 1; k < points.size(); k++) {
            nearest = Math.min(nearest, toSegment(p, points.get(k - 1), points.get(k)));
        }
        return nearest;
    }

    /* How near {@code p} comes to the straight piece from {@code s} to {@code e}. */
    private static double toSegment(final Point p, final Point s, final Point e) {
        final double dx = e.x() - s.x();
        final double dy = e.y() - s.y();
        final double dz = e.z() - s.z();
        final double square = dx * dx + dy * dy + dz * dz;
        double t = square == 0 ? 0 : ((p.x() - s.x()) * dx + (p.y() - s.y()) * dy + (p.z() - s.z()) * dz) / square;
        t = Math.max(0, Math.min(1, t));
        final double cx = s.x() + dx * t - p.x();
        final double cy = s.y() + dy * t - p.y();
        final double cz = s.z() + dz * t - p.z();
        return Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    /**
     * A point in the world.
     *
     * @param x its x
     * @param y its y
     * @param z its z
     */
    public record Point(double x, double y, double z) {
    }
}
