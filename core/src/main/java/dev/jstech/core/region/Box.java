/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

/**
 * A box of blocks, both corners inside it: what an entry of a {@link SpatialIndex} covers, from one block to a whole
 * area.
 */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("a box's first corner is its lowest: " + minX + "," + minY + ","
                    + minZ + " to " + maxX + "," + maxY + "," + maxZ);
        }
    }

    /** The box of one block. */
    public static Box at(final int x, final int y, final int z) {
        return new Box(x, y, z, x, y, z);
    }

    /**
     * The box of every block within {@code radius} of a block, along each axis; a corner that would pass the end of
     * the int range stops at it, so a huge radius covers the whole world instead of failing.
     */
    public static Box around(final int x, final int y, final int z, final int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("a radius is not negative: " + radius);
        }
        return new Box(low(x, radius), low(y, radius), low(z, radius), high(x, radius), high(y, radius),
                high(z, radius));
    }

    /** The box between two corners, given in any order. */
    public static Box between(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        return new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2),
                Math.max(z1, z2));
    }

    public boolean contains(final int x, final int y, final int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean intersects(final Box other) {
        return other.minX <= maxX && other.maxX >= minX && other.minY <= maxY && other.maxY >= minY
                && other.minZ <= maxZ && other.maxZ >= minZ;
    }

    /** The squared distance from a block to the nearest block of the box; 0 inside it. */
    public long distanceSquared(final int x, final int y, final int z) {
        final long dx = gap(x, minX, maxX);
        final long dy = gap(y, minY, maxY);
        final long dz = gap(z, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static int low(final int value, final int radius) {
        return (int) Math.max(Integer.MIN_VALUE, (long) value - radius);
    }

    private static int high(final int value, final int radius) {
        return (int) Math.min(Integer.MAX_VALUE, (long) value + radius);
    }

    private static long gap(final int value, final int min, final int max) {
        return value < min ? (long) min - value : value > max ? (long) value - max : 0L;
    }
}
