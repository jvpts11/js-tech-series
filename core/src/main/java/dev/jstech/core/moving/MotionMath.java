/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * The arithmetic of moving a structure by rule, a tick at a time: going toward a pose no faster than a speed and
 * turning no faster than a rate, spinning on an axis, turning about a hinge, and turning to face a direction. Pure: no
 * part of the game. Speeds are in blocks and angles in radians, each per tick.
 */
public final class MotionMath {

    /** Nearer than this to its target place, in blocks, a structure has arrived. */
    public static final double ARRIVED = 1.0e-6;
    /** Closer than this to its target rotation, in radians, a structure has finished turning. */
    public static final double TURNED = 1.0e-6;
    /* Below this length a direction is taken for none. */
    private static final double TINY = 1.0e-12;

    private MotionMath() {
    }

    /**
     * The pose one tick on from {@code now} toward {@code target}: its place moved no more than {@code most} blocks,
     * its rotation turned no more than {@code mostTurn} radians, each arriving exactly when it is close enough.
     */
    public static StructurePose towards(final StructurePose now, final StructurePose target, final double most,
                                        final double mostTurn) {
        final double distance = now.distanceTo(target);
        final double move = distance <= most || distance < ARRIVED ? 1 : most / distance;
        final double x = now.x() + (target.x() - now.x()) * move;
        final double y = now.y() + (target.y() - now.y()) * move;
        final double z = now.z() + (target.z() - now.z()) * move;
        final double angle = now.angleTo(target);
        final double turn = angle <= mostTurn || angle < TURNED ? 1 : mostTurn / angle;
        final StructurePose turned = StructurePose.between(now, target, turn);
        return turned.movedTo(x, y, z);
    }

    /** Whether {@code now} has reached {@code target}, in its place and in its rotation. */
    public static boolean arrived(final StructurePose now, final StructurePose target) {
        return now.distanceTo(target) < ARRIVED && now.angleTo(target) < TURNED;
    }

    /** The pose turned {@code angle} radians about the world's axis {@code (ax, ay, az)} through its own pivot. */
    public static StructurePose spin(final StructurePose now, final double ax, final double ay, final double az,
                                     final double angle) {
        return now.turnedBy(ax, ay, az, angle);
    }

    /**
     * The pose turned {@code angle} radians about the world's axis {@code (ax, ay, az)} through the world's place
     * {@code (px, py, pz)}: a drawbridge on its hinge, a door on its post; the pivot swings round the hinge.
     */
    public static StructurePose about(final StructurePose now, final double px, final double py, final double pz,
                                      final double ax, final double ay, final double az, final double angle) {
        final StructurePose by = StructurePose.turned(0, 0, 0, ax, ay, az, angle);
        final StructureFrame frame = new StructureFrame(by, 0, 0, 0);
        final double dx = now.x() - px;
        final double dy = now.y() - py;
        final double dz = now.z() - pz;
        return now.then(by).movedTo(px + frame.turnX(dx, dy, dz), py + frame.turnY(dx, dy, dz),
                pz + frame.turnZ(dx, dy, dz));
    }

    /**
     * The rotation that turns a structure's forward, its own south, toward the world's direction {@code (dx, dy, dz)},
     * its own up kept as near the world's up as that allows: an antenna aimed at a satellite, a turret at a target.
     * A direction straight up or down keeps the structure's heading.
     */
    public static StructurePose facing(final StructurePose now, final double dx, final double dy, final double dz) {
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < TINY) {
            return now;
        }
        final double fx = dx / length;
        final double fy = dy / length;
        final double fz = dz / length;
        double rx = fz;
        double rz = -fx;
        double rLength = Math.sqrt(rx * rx + rz * rz);
        if (rLength < TINY) {
            final double yaw = Math.toRadians(now.yawDegrees());
            rx = Math.cos(yaw);
            rz = Math.sin(yaw);
            rLength = 1;
        }
        rx /= rLength;
        rz /= rLength;
        final double ux = fy * rz;
        final double uy = fz * rx - fx * rz;
        final double uz = -fy * rx;
        return fromColumns(now, rx, 0, rz, ux, uy, uz, fx, fy, fz);
    }

    /* The rotation whose matrix has the columns given, the structure's east, up and south in the world. */
    private static StructurePose fromColumns(final StructurePose at, final double m00, final double m10,
                                             final double m20, final double m01, final double m11, final double m21,
                                             final double m02, final double m12, final double m22) {
        final double trace = m00 + m11 + m22;
        final double qw;
        final double qx;
        final double qy;
        final double qz;
        if (trace > 0) {
            final double s = Math.sqrt(trace + 1) * 2;
            qw = s / 4;
            qx = (m21 - m12) / s;
            qy = (m02 - m20) / s;
            qz = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            final double s = Math.sqrt(1 + m00 - m11 - m22) * 2;
            qw = (m21 - m12) / s;
            qx = s / 4;
            qy = (m01 + m10) / s;
            qz = (m02 + m20) / s;
        } else if (m11 > m22) {
            final double s = Math.sqrt(1 + m11 - m00 - m22) * 2;
            qw = (m02 - m20) / s;
            qx = (m01 + m10) / s;
            qy = s / 4;
            qz = (m12 + m21) / s;
        } else {
            final double s = Math.sqrt(1 + m22 - m00 - m11) * 2;
            qw = (m10 - m01) / s;
            qx = (m02 + m20) / s;
            qy = (m12 + m21) / s;
            qz = s / 4;
        }
        return new StructurePose(at.x(), at.y(), at.z(), qx, qy, qz, qw);
    }
}
