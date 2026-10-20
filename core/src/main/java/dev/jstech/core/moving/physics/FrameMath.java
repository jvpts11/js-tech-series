/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.StructureFrame;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

/** A structure's frame, for its physics: its places and ways in the world and in the yard, as vectors. */
final class FrameMath {

    private static final double HALF = 0.5;

    private FrameMath() {
    }

    /** The world's place of the yard's place {@code (x, y, z)}, into {@code into}. */
    static Vector3d world(final StructureFrame frame, final double x, final double y, final double z,
                          final Vector3d into) {
        return into.set(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /** The world's place of the yard's place {@code yard}, into {@code into}. */
    static Vector3d world(final StructureFrame frame, final Vector3d yard, final Vector3d into) {
        return world(frame, yard.x, yard.y, yard.z, into);
    }

    /** The yard's place of the world's place {@code world}, into {@code into}. */
    static Vector3d yard(final StructureFrame frame, final Vector3d world, final Vector3d into) {
        final double x = world.x;
        final double y = world.y;
        final double z = world.z;
        return into.set(frame.yardX(x, y, z), frame.yardY(x, y, z), frame.yardZ(x, y, z));
    }

    /** The world's place of the middle of the yard's block at {@code (x, y, z)}, into {@code into}. */
    static Vector3d middle(final StructureFrame frame, final int x, final int y, final int z, final Vector3d into) {
        return world(frame, x + HALF, y + HALF, z + HALF, into);
    }

    /** The world's way of the yard's way {@code direction}, as the structure is turned, into {@code into}. */
    static Vector3d turned(final Direction direction, final StructureFrame frame, final Vector3d into) {
        final double x = direction.getStepX();
        final double y = direction.getStepY();
        final double z = direction.getStepZ();
        return into.set(frame.turnX(x, y, z), frame.turnY(x, y, z), frame.turnZ(x, y, z));
    }

    /** The world's way of the yard's axis {@code index}, 0 east, 1 up, 2 south, into {@code into}. */
    static Vector3d axis(final StructureFrame frame, final int index, final Vector3d into) {
        return into.set(frame.rotation(0, index), frame.rotation(1, index), frame.rotation(2, index));
    }

    /**
     * Makes {@code box} the world's box of the yard's box {@code shape} of the block at {@code (x, y, z)}, turned as
     * the structure is.
     */
    static OrientedBox box(final StructureFrame frame, final int x, final int y, final int z, final AABB shape,
                           final Vector3d[] axes, final Vector3d scratch, final OrientedBox box) {
        world(frame, x + (shape.minX + shape.maxX) * HALF, y + (shape.minY + shape.maxY) * HALF,
                z + (shape.minZ + shape.maxZ) * HALF, scratch);
        return box.set(scratch, axes[0], axes[1], axes[2], (shape.maxX - shape.minX) * HALF,
                (shape.maxY - shape.minY) * HALF, (shape.maxZ - shape.minZ) * HALF);
    }

    /** The world's ways of the yard's three axes, into {@code into}. */
    static Vector3d[] axes(final StructureFrame frame, final Vector3d[] into) {
        for (int i = 0; i < 3; i++) {
            axis(frame, i, into[i]);
        }
        return into;
    }
}
