/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * Where a moving structure is and how it is turned: the place in the world its pivot stands at, and its rotation as a
 * unit quaternion, so it turns on every axis with no lock and blends smoothly between two poses. Pure: no part of the
 * game, so its arithmetic is tested on its own.
 *
 * @param x  the pivot's place in the world, east
 * @param y  the pivot's place in the world, up
 * @param z  the pivot's place in the world, south
 * @param qx the rotation's quaternion, x part
 * @param qy the rotation's quaternion, y part
 * @param qz the rotation's quaternion, z part
 * @param qw the rotation's quaternion, real part
 */
public record StructurePose(double x, double y, double z, double qx, double qy, double qz, double qw) {

    /* Below this length a quaternion is taken for none, and a rotation that small for none. */
    private static final double TINY = 1.0e-12;
    /* Above this dot product two rotations are so close that blending them straight is exact enough. */
    private static final double NEARLY_SAME = 0.9995;

    /**
     * Keeps the rotation a unit quaternion, and refuses a pose that is no place.
     *
     * @throws IllegalArgumentException when a part is not a number, or the rotation has no length
     */
    public StructurePose {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("a pose stands somewhere: " + x + ", " + y + ", " + z);
        }
        final double length = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        if (!(length > TINY) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("a rotation has a length: " + qx + ", " + qy + ", " + qz + ", " + qw);
        }
        qx /= length;
        qy /= length;
        qz /= length;
        qw /= length;
    }

    /** A pose at {@code (x, y, z)}, not turned at all. */
    public static StructurePose at(final double x, final double y, final double z) {
        return new StructurePose(x, y, z, 0, 0, 0, 1);
    }

    /** The rotation of {@code angle} radians about the axis {@code (ax, ay, az)}, which need not be of unit length. */
    public static StructurePose turned(final double x, final double y, final double z, final double ax,
                                       final double ay, final double az, final double angle) {
        final double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (length < TINY || Math.abs(angle) < TINY) {
            return at(x, y, z);
        }
        final double half = angle / 2;
        final double s = Math.sin(half) / length;
        return new StructurePose(x, y, z, ax * s, ay * s, az * s, Math.cos(half));
    }

    /**
     * The pose a share {@code t} of the way from {@code from} to {@code to}: the place in a straight line, the
     * rotation along the shortest arc, so a structure seen between two of its poses turns as it really turned.
     */
    public static StructurePose between(final StructurePose from, final StructurePose to, final double t) {
        final double x = from.x + (to.x - from.x) * t;
        final double y = from.y + (to.y - from.y) * t;
        final double z = from.z + (to.z - from.z) * t;
        double bx = to.qx;
        double by = to.qy;
        double bz = to.qz;
        double bw = to.qw;
        double dot = from.qx * bx + from.qy * by + from.qz * bz + from.qw * bw;
        if (dot < 0) {
            dot = -dot;
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
        }
        final double a;
        final double b;
        if (dot > NEARLY_SAME) {
            a = 1 - t;
            b = t;
        } else {
            final double theta = Math.acos(dot);
            final double sin = Math.sin(theta);
            a = Math.sin((1 - t) * theta) / sin;
            b = Math.sin(t * theta) / sin;
        }
        return new StructurePose(x, y, z, a * from.qx + b * bx, a * from.qy + b * by, a * from.qz + b * bz,
                a * from.qw + b * bw);
    }

    /** This pose moved to {@code (x, y, z)}, turned as it is. */
    public StructurePose movedTo(final double toX, final double toY, final double toZ) {
        return new StructurePose(toX, toY, toZ, qx, qy, qz, qw);
    }

    /** This pose turned further by {@code angle} radians about the world's axis {@code (ax, ay, az)}. */
    public StructurePose turnedBy(final double ax, final double ay, final double az, final double angle) {
        final StructurePose by = turned(0, 0, 0, ax, ay, az, angle);
        return then(by);
    }

    /** This pose's place with the rotation {@code by} applied after its own, in the world's axes. */
    public StructurePose then(final StructurePose by) {
        final double w = by.qw * qw - by.qx * qx - by.qy * qy - by.qz * qz;
        final double i = by.qw * qx + by.qx * qw + by.qy * qz - by.qz * qy;
        final double j = by.qw * qy - by.qx * qz + by.qy * qw + by.qz * qx;
        final double k = by.qw * qz + by.qx * qy - by.qy * qx + by.qz * qw;
        return new StructurePose(x, y, z, i, j, k, w);
    }

    /** The angle in radians this pose's rotation is turned from {@code other}'s, the shortest way. */
    public double angleTo(final StructurePose other) {
        final double dot = Math.min(1, Math.abs(qx * other.qx + qy * other.qy + qz * other.qz + qw * other.qw));
        return 2 * Math.acos(dot);
    }

    /** How far this pose's place is from {@code other}'s, in blocks. */
    public double distanceTo(final StructurePose other) {
        final double dx = other.x - x;
        final double dy = other.y - y;
        final double dz = other.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Whether this pose is not turned at all, to within a hair. */
    public boolean unturned() {
        return Math.abs(qw) > 1 - TINY;
    }

    /**
     * Which way the structure's own south, its forward, points in the world, as the game's yaw in degrees: what a
     * player standing on it turns by when it turns.
     */
    public double yawDegrees() {
        final double fx = 2 * (qx * qz + qw * qy);
        final double fz = 1 - 2 * (qx * qx + qy * qy);
        return Math.toDegrees(Math.atan2(-fx, fz));
    }
}
