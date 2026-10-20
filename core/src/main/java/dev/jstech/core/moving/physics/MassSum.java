/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import org.joml.Matrix3d;
import org.joml.Vector3d;

/**
 * The mass of a body made of blocks, kept as the blocks come and go: the whole mass, the centre of mass and the
 * inertia about it, each block a cube of its own mass at its place. A block added or taken away costs the same
 * whatever the body's size, so a structure's mass follows every block set on it or broken off it, live.
 *
 * <p>The places may be of any frame, the yard's for a structure. The sums are kept about the first block's place, so
 * they stay exact however far from nought the frame puts the body, as the yard's places, tens of millions out, are.
 */
public final class MassSum {

    /* The place the sums are kept about: the first block's. */
    private double originX;
    private double originY;
    private double originZ;
    private boolean placed;
    private double mass;
    /* The sum of each mass times its place, and of each mass times the products of its place's parts. */
    private double sumX;
    private double sumY;
    private double sumZ;
    private double sumXX;
    private double sumYY;
    private double sumZZ;
    private double sumXY;
    private double sumXZ;
    private double sumYZ;
    /* The sum of each block's own inertia about its own middle, the same on every axis for a cube. */
    private double own;

    /* A cube's own inertia about its middle, on each axis, as a share of its mass times its side squared. */
    private static final double CUBE = 1.0 / 6.0;
    /* Less than this is no mass at all, as rounding leaves after a block taken away. */
    private static final double NONE = 1.0e-9;

    /** Adds a block of {@code kilograms} with its middle at {@code (x, y, z)}, a cube of side {@code side}. */
    public void add(final double x, final double y, final double z, final double kilograms, final double side) {
        change(x, y, z, kilograms, side, 1);
    }

    /** Takes away a block {@link #add added} with the same values. */
    public void remove(final double x, final double y, final double z, final double kilograms, final double side) {
        change(x, y, z, kilograms, side, -1);
    }

    /** The whole mass, in kilograms; zero for a body of nothing. */
    public double mass() {
        return mass < NONE ? 0 : mass;
    }

    /** The centre of mass, into {@code into}; the origin for a body of nothing. */
    public Vector3d centre(final Vector3d into) {
        if (mass < NONE) {
            return into.set(0, 0, 0);
        }
        return into.set(originX + sumX / mass, originY + sumY / mass, originZ + sumZ / mass);
    }

    /**
     * The inertia about the centre of mass, into {@code into}, in kilograms times metres squared: the blocks' own and
     * that of their places about the centre, by the parallel axis rule.
     */
    public Matrix3d inertia(final Matrix3d into) {
        if (mass < NONE) {
            return into.identity().scale(0);
        }
        final double cx = sumX / mass;
        final double cy = sumY / mass;
        final double cz = sumZ / mass;
        // About the origin, then moved to the centre: I_c = I_o - m (|c|^2 E - c c^T).
        final double xx = sumXX - mass * cx * cx;
        final double yy = sumYY - mass * cy * cy;
        final double zz = sumZZ - mass * cz * cz;
        final double xy = sumXY - mass * cx * cy;
        final double xz = sumXZ - mass * cx * cz;
        final double yz = sumYZ - mass * cy * cz;
        // Column-major: m00 m01 m02 is the first column.
        return into.set(yy + zz + own, -xy, -xz,
                -xy, xx + zz + own, -yz,
                -xz, -yz, xx + yy + own);
    }

    /** Whether it has no mass left. */
    public boolean empty() {
        return mass < NONE;
    }

    private void change(final double placeX, final double placeY, final double placeZ, final double kilograms,
                        final double side, final int sign) {
        if (!placed) {
            originX = placeX;
            originY = placeY;
            originZ = placeZ;
            placed = true;
        }
        final double x = placeX - originX;
        final double y = placeY - originY;
        final double z = placeZ - originZ;
        final double m = sign * kilograms;
        mass += m;
        sumX += m * x;
        sumY += m * y;
        sumZ += m * z;
        sumXX += m * x * x;
        sumYY += m * y * y;
        sumZZ += m * z * z;
        sumXY += m * x * y;
        sumXZ += m * x * z;
        sumYZ += m * y * z;
        own += m * side * side * CUBE;
    }
}
