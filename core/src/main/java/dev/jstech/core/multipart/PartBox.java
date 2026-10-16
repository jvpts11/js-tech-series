/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.ArrayList;
import java.util.List;

/**
 * A box within a block space, in pixels from 0 to 16 along each axis: the room a part takes. Pure.
 *
 * @param x0 its least x
 * @param y0 its least y
 * @param z0 its least z
 * @param x1 its most x
 * @param y1 its most y
 * @param z1 its most z
 */
public record PartBox(int x0, int y0, int z0, int x1, int y1, int z1) {

    /** A block's side, in pixels. */
    public static final int SIDE = 16;

    public PartBox {
        if (x0 < 0 || y0 < 0 || z0 < 0 || x1 > SIDE || y1 > SIDE || z1 > SIDE || x0 >= x1 || y0 >= y1 || z0 >= z1) {
            throw new IllegalArgumentException("a box within a block runs from 0 to 16, each side longer than nothing; "
                    + "got " + x0 + "," + y0 + "," + z0 + " to " + x1 + "," + y1 + "," + z1);
        }
    }

    /** Whether it shares room with {@code other}: touching is not sharing. */
    public boolean overlaps(final PartBox other) {
        return this.x0 < other.x1 && other.x0 < this.x1 && this.y0 < other.y1 && other.y0 < this.y1
                && this.z0 < other.z1 && other.z0 < this.z1;
    }

    /** Its least coordinate along {@code axis}. */
    public int min(final int axis) {
        return switch (axis) {
            case SpaceSlots.X -> this.x0;
            case SpaceSlots.Y -> this.y0;
            default -> this.z0;
        };
    }

    /** Its most coordinate along {@code axis}. */
    public int max(final int axis) {
        return switch (axis) {
            case SpaceSlots.X -> this.x1;
            case SpaceSlots.Y -> this.y1;
            default -> this.z1;
        };
    }

    /** What of it lies outside {@code other}: itself when they share no room, else up to six boxes. */
    public List<PartBox> minus(final PartBox other) {
        if (!overlaps(other)) {
            return List.of(this);
        }
        final List<PartBox> out = new ArrayList<>();
        final int[] least = {this.x0, this.y0, this.z0};
        final int[] most = {this.x1, this.y1, this.z1};
        // Slice off what lies before and after the other box along each axis in turn; what is left lies inside it.
        for (int axis = SpaceSlots.X; axis <= SpaceSlots.Z; axis++) {
            if (least[axis] < other.min(axis)) {
                final int[] to = most.clone();
                to[axis] = other.min(axis);
                out.add(of(least, to));
                least[axis] = other.min(axis);
            }
            if (most[axis] > other.max(axis)) {
                final int[] from = least.clone();
                from[axis] = other.max(axis);
                out.add(of(from, most));
                most[axis] = other.max(axis);
            }
        }
        return out;
    }

    /**
     * It, made for a part facing north, turned about the block's middle to the face numbered {@code face} in the order
     * of the game's directions, as the part's model is turned there.
     */
    public PartBox turnedTo(final int face) {
        final int[] a = turn(face, this.x0, this.y0, this.z0);
        final int[] b = turn(face, this.x1, this.y1, this.z1);
        return new PartBox(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]),
                Math.max(a[1], b[1]), Math.max(a[2], b[2]));
    }

    /** Whether the point at ({@code x}, {@code y}, {@code z}), in pixels, lies in it or on its sides. */
    public boolean contains(final double x, final double y, final double z) {
        return x >= this.x0 && x <= this.x1 && y >= this.y0 && y <= this.y1 && z >= this.z0 && z <= this.z1;
    }

    /** A box from each axis's least to its most, the axes in the order x, y, z. */
    public static PartBox of(final int[] least, final int[] most) {
        return new PartBox(least[SpaceSlots.X], least[SpaceSlots.Y], least[SpaceSlots.Z], most[SpaceSlots.X],
                most[SpaceSlots.Y], most[SpaceSlots.Z]);
    }

    /*
     * A point turned about the block's middle from facing north to the face numbered {@code face}: a quarter turn up
     * or down about x for the floor and ceiling, a turn about y for the sides.
     */
    private static int[] turn(final int face, final int x, final int y, final int z) {
        final int half = SIDE / 2;
        final int px = x - half;
        final int py = y - half;
        final int pz = z - half;
        final int[] turned = switch (face) {
            case 0 -> new int[] {px, pz, -py};
            case 1 -> new int[] {px, -pz, py};
            case 3 -> new int[] {-px, py, -pz};
            case 4 -> new int[] {pz, py, -px};
            case 5 -> new int[] {-pz, py, px};
            default -> new int[] {px, py, pz};
        };
        return new int[] {turned[0] + half, turned[1] + half, turned[2] + half};
    }
}
