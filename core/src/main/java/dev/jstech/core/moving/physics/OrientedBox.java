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
 * A box turned any way in the world: its middle, its three axes, of unit length and square to each other, and how far
 * it reaches along each from its middle. A block of a turned structure is one; a block of the world is one whose axes
 * are the world's. Reused, so a step of many contacts makes none anew.
 */
public final class OrientedBox {

    private final Vector3d centre = new Vector3d();
    private final Vector3d[] axes = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1)};
    private final double[] half = new double[3];

    /** Its middle. */
    public Vector3d centre() {
        return centre;
    }

    /** Its axis {@code index}, from 0 to 2. */
    public Vector3d axis(final int index) {
        return axes[index];
    }

    /** How far it reaches from its middle along its axis {@code index}. */
    public double half(final int index) {
        return half[index];
    }

    /** Makes it the box of the world from {@code (minX, minY, minZ)} to {@code (maxX, maxY, maxZ)}, square to it. */
    public OrientedBox square(final double minX, final double minY, final double minZ, final double maxX,
                              final double maxY, final double maxZ) {
        centre.set((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        axes[0].set(1, 0, 0);
        axes[1].set(0, 1, 0);
        axes[2].set(0, 0, 1);
        half[0] = (maxX - minX) / 2;
        half[1] = (maxY - minY) / 2;
        half[2] = (maxZ - minZ) / 2;
        return this;
    }

    /** Makes it a box of middle {@code centre}, axes {@code x}, {@code y} and {@code z}, and reaches. */
    public OrientedBox set(final Vector3d middle, final Vector3d x, final Vector3d y, final Vector3d z,
                           final double halfX, final double halfY, final double halfZ) {
        centre.set(middle);
        axes[0].set(x);
        axes[1].set(y);
        axes[2].set(z);
        half[0] = halfX;
        half[1] = halfY;
        half[2] = halfZ;
        return this;
    }

    /** How far it reaches along the way {@code way}, of unit length, from its middle. */
    public double reach(final Vector3d way) {
        return half[0] * Math.abs(axes[0].dot(way)) + half[1] * Math.abs(axes[1].dot(way))
                + half[2] * Math.abs(axes[2].dot(way));
    }

    /** Its corner {@code index}, from 0 to 7, into {@code into}: the bits of the index pick each axis's side. */
    public Vector3d corner(final int index, final Vector3d into) {
        into.set(centre);
        into.fma((index & 1) == 0 ? -half[0] : half[0], axes[0]);
        into.fma((index & 2) == 0 ? -half[1] : half[1], axes[1]);
        into.fma((index & 4) == 0 ? -half[2] : half[2], axes[2]);
        return into;
    }

    /** Whether the world's point {@code point} is in it, grown by {@code margin} on every side. */
    public boolean holds(final Vector3d point, final double margin) {
        final double dx = point.x - centre.x;
        final double dy = point.y - centre.y;
        final double dz = point.z - centre.z;
        for (int i = 0; i < 3; i++) {
            final Vector3d axis = axes[i];
            if (Math.abs(dx * axis.x + dy * axis.y + dz * axis.z) > half[i] + margin) {
                return false;
            }
        }
        return true;
    }

    /** Its point furthest along {@code way}, a corner, into {@code into}. */
    public Vector3d furthest(final Vector3d way, final Vector3d into) {
        into.set(centre);
        for (int i = 0; i < 3; i++) {
            into.fma(axes[i].dot(way) >= 0 ? half[i] : -half[i], axes[i]);
        }
        return into;
    }
}
