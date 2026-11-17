/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import java.util.List;
import java.util.Optional;

/**
 * The way a roller door's slats run as it rolls up, in the door's cross-section: from its sill along a line of
 * straight runs, the hull's wall and then its chamfer, to the drum at its end, where a slat carried past it is wound
 * out of sight. Shut, the slats lie along the way, each resting at its own distance along it; opening, every slat is
 * carried on by the same share of the door's travel, turning as it goes round a corner
 * (space/docs/design/starships.md, the Side Door hold). Pure: the art generator lays the same way into the door's
 * rig, which the game reads.
 *
 * @param points the way's corners, from the sill to the drum, in blocks across and up the ship
 */
public record RollPath(List<Point> points) {

    /* How near the drum's mouth a slat is wound, in blocks. */
    private static final double WOUND = 1.0e-6;

    /** Checks the way has a length: two corners at least, none on the one before it. */
    public RollPath {
        points = List.copyOf(points);
        if (points.size() < 2) {
            throw new IllegalArgumentException("A roller door's way needs two corners at least, not " + points.size());
        }
        for (int k = 1; k < points.size(); k++) {
            if (points.get(k).distance(points.get(k - 1)) <= 0) {
                throw new IllegalArgumentException("A roller door's way has two corners on one place: " + points);
            }
        }
    }

    /** How long the way is, from the sill to the drum, in blocks. */
    public double length() {
        double length = 0;
        for (int k = 1; k < this.points.size(); k++) {
            length += this.points.get(k).distance(this.points.get(k - 1));
        }
        return length;
    }

    /**
     * Where the way is {@code arc} blocks along from the sill, held to its ends, and which way it runs there, in
     * degrees from across the ship toward up.
     */
    public Place at(final double arc) {
        double left = Math.max(0, arc);
        for (int k = 1; k < this.points.size(); k++) {
            final Point a = this.points.get(k - 1);
            final Point b = this.points.get(k);
            final double run = b.distance(a);
            if (left <= run || k == this.points.size() - 1) {
                final double t = Math.min(1, left / run);
                return new Place(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t,
                        Math.toDegrees(Math.atan2(b.y() - a.y(), b.x() - a.x())));
            }
            left -= run;
        }
        throw new IllegalStateException("A roller door's way with no run");
    }

    /**
     * How the slat resting {@code rest} blocks along the way has moved with the door open {@code share} of its
     * {@code travel}: how far across and up, and how far it has turned; none once it is wound into the drum.
     */
    public Optional<Shift> shift(final double rest, final double travel, final double share) {
        final double arc = rest + travel * share;
        if (arc >= length() - WOUND) {
            return Optional.empty();
        }
        final Place from = at(rest);
        final Place to = at(arc);
        return Optional.of(new Shift(to.x() - from.x(), to.y() - from.y(), to.angle() - from.angle()));
    }

    /**
     * A corner of the way.
     *
     * @param x across the ship, in blocks
     * @param y up the ship, in blocks
     */
    public record Point(double x, double y) {

        /** How far it is from {@code other}, in blocks. */
        public double distance(final Point other) {
            return Math.hypot(this.x - other.x, this.y - other.y);
        }
    }

    /**
     * A place on the way.
     *
     * @param x     across the ship, in blocks
     * @param y     up the ship, in blocks
     * @param angle which way the way runs there, in degrees from across the ship toward up
     */
    public record Place(double x, double y, double angle) {
    }

    /**
     * How a slat has moved from where it rests.
     *
     * @param dx   across the ship, in blocks
     * @param dy   up the ship, in blocks
     * @param turn how far it has turned, in degrees from across the ship toward up
     */
    public record Shift(double dx, double dy, double turn) {
    }
}
