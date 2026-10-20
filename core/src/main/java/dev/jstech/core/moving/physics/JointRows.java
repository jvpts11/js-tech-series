/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The rows that make up the joints between two bodies ({@link ConstraintRows}), from where the joint's points and
 * ways are on each body now, in the world: a point held to a point, an axis held along an axis, two bodies held
 * turned as they were, a distance held at most, exactly or as a spring, a point held to a line, and the limits and
 * motors along a line or about an axis. Each row that holds a place says how far the bodies drifted from it, for them
 * to be drawn back once they have moved.
 */
public final class JointRows {

    private final ConstraintRows rows;
    private final Vector3d armA = new Vector3d();
    private final Vector3d armB = new Vector3d();
    private final Vector3d apart = new Vector3d();
    private final Vector3d way = new Vector3d();
    private final Vector3d first = new Vector3d();
    private final Vector3d second = new Vector3d();
    private final Vector3d error = new Vector3d();
    private final Quaterniond turn = new Quaterniond();

    /* Closer than this, two points have no way between them. */
    private static final double NEAR = 1.0e-9;
    /* The world's three axes. */
    private static final Vector3d[] AXES = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1)};
    /* Rows a joint may have, at most; the keys of its rows are counted from its own times this. */
    private static final int ROWS_PER_JOINT = 32;

    public JointRows(final ConstraintRows into) {
        this.rows = into;
    }

    /** The key of the {@code index}th row of the joint keyed {@code joint}. */
    public static long key(final long joint, final int index) {
        return joint * ROWS_PER_JOINT + index;
    }

    /** Holds {@code a}'s point {@code pa} to {@code b}'s point {@code pb}, both in the world: three rows. */
    public void point(final RigidBody a, final Vector3d pa, final RigidBody b, final Vector3d pb, final long key) {
        armA.set(pa).sub(a.position());
        armB.set(pb).sub(b.position());
        apart.set(pa).sub(pb);
        for (int i = 0; i < 3; i++) {
            final Vector3d axis = AXES[i];
            final ConstraintRows.Row row = rows.add(a, b, key(key, i));
            row.linearA().set(axis);
            armA.cross(axis, row.angularA());
            row.linearB().set(axis).negate();
            armB.cross(axis, row.angularB()).negate();
            row.holds(apart.dot(axis));
        }
    }

    /**
     * Holds {@code a}'s axis {@code axisA} along {@code b}'s axis {@code axisB}, both ways of unit length in the
     * world, so the two turn about it only: two rows.
     */
    public void align(final RigidBody a, final Vector3d axisA, final RigidBody b, final Vector3d axisB,
                      final long key) {
        perpendiculars(axisA, first, second);
        axisA.cross(axisB, error);
        angularPair(a, b, first, error, key(key, 0));
        angularPair(a, b, second, error, key(key, 1));
    }

    /**
     * Holds {@code b} turned to {@code a} as their frames {@code frameA} and {@code frameB}, each the world's turn of
     * the joint's frame on that body, say they are to be: three rows.
     */
    public void lock(final RigidBody a, final Quaterniond frameA, final RigidBody b, final Quaterniond frameB,
                     final long key) {
        // The turn from a's frame to b's, in the world: none when they agree.
        frameB.mul(turn.set(frameA).conjugate(), turn);
        if (turn.w < 0) {
            turn.set(-turn.x, -turn.y, -turn.z, -turn.w);
        }
        error.set(turn.x, turn.y, turn.z).mul(2);
        for (int i = 0; i < 3; i++) {
            angularPair(a, b, AXES[i], error, key(key, i));
        }
    }

    /**
     * Holds the distance between {@code a}'s point {@code pa} and {@code b}'s point {@code pb} at {@code length}: as
     * a rope, at most; as a bar, exactly; as a spring of {@code stiffness} newtons a metre and {@code damping} newton
     * seconds a metre, softly, over a step of {@code seconds}. One row; none while the two points are at one place.
     */
    public void distance(final RigidBody a, final Vector3d pa, final RigidBody b, final Vector3d pb,
                         final double length, final Distance kind, final double stiffness, final double damping,
                         final double seconds, final long key) {
        apart.set(pa).sub(pb);
        final double now = apart.length();
        final double soft = damping + seconds * stiffness;
        if (now < NEAR || kind == Distance.SPRING && soft <= 0) {
            return;
        }
        way.set(apart).div(now);
        armA.set(pa).sub(a.position());
        armB.set(pb).sub(b.position());
        final ConstraintRows.Row row = rows.add(a, b, key(key, 0));
        row.linearA().set(way);
        armA.cross(way, row.angularA());
        row.linearB().set(way).negate();
        armB.cross(way, row.angularB()).negate();
        final double off = now - length;
        switch (kind) {
            // Slack, it lets the points part as far as the slack in the step; taut, it pulls, never pushes.
            case AT_MOST -> row.target(off < 0 ? -off / seconds : 0).bounds(Double.NEGATIVE_INFINITY, 0).holds(off);
            case EXACTLY -> row.holds(off);
            case SPRING -> row.softness(1.0 / (seconds * soft)).target(-off * stiffness / soft);
        }
    }

    /**
     * Holds {@code b}'s point {@code pb} on the line through {@code a}'s point {@code pa} along {@code axis}, a way of
     * unit length in the world turning with {@code a}: two rows.
     */
    public void onLine(final RigidBody a, final Vector3d pa, final Vector3d axis, final RigidBody b,
                       final Vector3d pb, final long key) {
        perpendiculars(axis, first, second);
        apart.set(pb).sub(pa);
        final double offFirst = apart.dot(first);
        final double offSecond = apart.dot(second);
        sliding(a, pa, b, pb, first, key(key, 0), 1).holds(offFirst);
        sliding(a, pa, b, pb, second, key(key, 1), 1).holds(offSecond);
    }

    /**
     * Along the line through {@code a}'s point {@code pa} along {@code axis}: keeps {@code b}'s point {@code pb} from
     * going below {@code least} or above {@code most} along it, in metres from {@code pa}, and drives it along at
     * {@code speed} metres a second with up to {@code force} newtons, over a step of {@code seconds}. Rows only for
     * what binds: a limit set, a motor of any force.
     */
    public void slide(final RigidBody a, final Vector3d pa, final Vector3d axis, final RigidBody b,
                      final Vector3d pb, final double least, final double most, final double speed,
                      final double force, final double seconds, final long key) {
        apart.set(pb).sub(pa);
        final double at = apart.dot(axis);
        if (force > 0) {
            final double bound = force * seconds;
            sliding(a, pa, b, pb, axis, key(key, 0), 1).target(speed).bounds(-bound, bound);
        }
        if (least > Double.NEGATIVE_INFINITY) {
            limit(sliding(a, pa, b, pb, axis, key(key, 1), 1), at - least, seconds);
        }
        if (most < Double.POSITIVE_INFINITY) {
            limit(sliding(a, pa, b, pb, axis, key(key, 2), -1), most - at, seconds);
        }
    }

    /**
     * About {@code axis}, a way of unit length in the world: keeps {@code b} turned from {@code a} by {@code angle}
     * radians no less than {@code least} and no more than {@code most}, and drives it about at {@code speed} radians a
     * second with up to {@code torque} newton metres, over a step of {@code seconds}. Rows only for what binds.
     */
    public void turn(final RigidBody a, final RigidBody b, final Vector3d axis, final double angle,
                     final double least, final double most, final double speed, final double torque,
                     final double seconds, final long key) {
        if (torque > 0) {
            final double bound = torque * seconds;
            turning(a, b, axis, key(key, 0), 1).target(speed).bounds(-bound, bound);
        }
        if (least > Double.NEGATIVE_INFINITY) {
            limit(turning(a, b, axis, key(key, 1), 1), angle - least, seconds);
        }
        if (most < Double.POSITIVE_INFINITY) {
            limit(turning(a, b, axis, key(key, 2), -1), most - angle, seconds);
        }
    }

    /**
     * How far {@code b} is turned from {@code a} about {@code axis}, in radians from minus pi to pi: the angle from
     * {@code a}'s reference way {@code refA} to {@code b}'s {@code refB}, each at right angles to the axis.
     */
    public static double angle(final Vector3d axis, final Vector3d refA, final Vector3d refB) {
        final Vector3d across = new Vector3d(refA).cross(refB);
        return Math.atan2(across.dot(axis), refA.dot(refB));
    }

    /** Two ways of unit length at right angles to {@code axis} and to each other, into {@code u} and {@code v}. */
    public static void perpendiculars(final Vector3d axis, final Vector3d u, final Vector3d v) {
        if (Math.abs(axis.x) < 0.57) {
            u.set(1, 0, 0);
        } else {
            u.set(0, 1, 0);
        }
        u.fma(-u.dot(axis), axis).normalize();
        axis.cross(u, v).normalize();
    }

    /*
     * A row turning a and b against each other about {@code axis}, {@code turned} the turn from a's way to b's: b
     * turned ahead of a about the axis is drawn back.
     */
    private void angularPair(final RigidBody a, final RigidBody b, final Vector3d axis, final Vector3d turned,
                             final long key) {
        final ConstraintRows.Row row = rows.add(a, b, key);
        row.angularA().set(axis);
        row.angularB().set(axis).negate();
        row.holds(-turned.dot(axis));
    }

    /*
     * A row of how fast b's point goes along a's line's way {@code along}, turning with a, {@code sign} times: the
     * line's way turns as a turns, so a's turning moves b's point along it too.
     */
    private ConstraintRows.Row sliding(final RigidBody a, final Vector3d pa, final RigidBody b, final Vector3d pb,
                                       final Vector3d along, final long key, final int sign) {
        armA.set(pa).sub(a.position());
        armB.set(pb).sub(b.position());
        apart.set(pb).sub(pa);
        final ConstraintRows.Row row = rows.add(a, b, key);
        row.linearA().set(along).mul(-sign);
        armA.add(apart).cross(along, row.angularA()).mul(-sign);
        row.linearB().set(along).mul(sign);
        armB.cross(along, row.angularB()).mul(sign);
        return row;
    }

    /* A row of how fast b turns from a about {@code axis}, {@code sign} times. */
    private ConstraintRows.Row turning(final RigidBody a, final RigidBody b, final Vector3d axis, final long key,
                                       final int sign) {
        final ConstraintRows.Row row = rows.add(a, b, key);
        row.angularA().set(axis).mul(-sign);
        row.angularB().set(axis).mul(sign);
        return row;
    }

    /*
     * Makes {@code row} a limit with {@code room} left before it, in metres or radians: it only pushes away from the
     * limit; with room, it lets the bodies come up to the limit in the step and no further; past it, it holds them
     * there and draws them back.
     */
    private static void limit(final ConstraintRows.Row row, final double room, final double seconds) {
        row.bounds(0, Double.POSITIVE_INFINITY).target(room > 0 ? -room / seconds : 0).holds(room);
    }

    /** How a distance is held. */
    public enum Distance {
        /** No further than the length, as a rope or a chain. */
        AT_MOST,
        /** At the length, as a bar. */
        EXACTLY,
        /** About the length, softly, as a spring. */
        SPRING
    }
}
