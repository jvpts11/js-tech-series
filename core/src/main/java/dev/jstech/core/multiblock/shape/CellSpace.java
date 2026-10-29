/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import dev.jstech.core.multiblock.Orientation;

/**
 * A block of a formed structure as its model has it and as it stands in the world: the block's own space, from 0 to 1
 * along each axis, turned about its middle as its structure stands, and the block's place among the structure's,
 * turned about its terminal. Pure, so it is tested without the game.
 */
public final class CellSpace {

    private static final double HALF = 0.5;
    private static final int AXES = 3;

    private CellSpace() {
    }

    /** The boxes {@code boxes} of a block as the model has it, in sixteenths, as they stand turned {@code turn}. */
    public static int[] boxes(final int[] boxes, final Orientation turn) {
        final int[] from = froms(turn);
        final int[] sign = signs(turn);
        final int[] out = new int[boxes.length];
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            for (int axis = 0; axis < AXES; axis++) {
                final int low = boxes[b + from[axis]];
                final int high = boxes[b + AXES + from[axis]];
                out[b + axis] = sign[axis] > 0 ? low : MeshVoxels.FINE - high;
                out[b + AXES + axis] = sign[axis] > 0 ? high : MeshVoxels.FINE - low;
            }
        }
        return out;
    }

    /** Puts in {@code out} the point {@code local} of a block as the model has it, as it stands turned {@code turn}. */
    public static void toWorld(final double[] local, final Orientation turn, final double[] out) {
        final int[] from = froms(turn);
        final int[] sign = signs(turn);
        final double x = local[from[0]];
        final double y = local[from[1]];
        final double z = local[from[2]];
        out[0] = HALF + sign[0] * (x - HALF);
        out[1] = HALF + sign[1] * (y - HALF);
        out[2] = HALF + sign[2] * (z - HALF);
    }

    /** Puts in {@code out} the point {@code world} of a block standing turned {@code turn}, as the model has it. */
    public static void toModel(final double[] world, final Orientation turn, final double[] out) {
        final int[] from = froms(turn);
        final int[] sign = signs(turn);
        final double x = world[0];
        final double y = world[1];
        final double z = world[2];
        out[from[0]] = HALF + sign[0] * (x - HALF);
        out[from[1]] = HALF + sign[1] * (y - HALF);
        out[from[2]] = HALF + sign[2] * (z - HALF);
    }

    /** The block of the model at the offset {@code (x, y, z)} from its terminal in the world, turned {@code turn}. */
    public static int[] cellOf(final int x, final int y, final int z, final Orientation turn) {
        final int[] from = froms(turn);
        final int[] sign = signs(turn);
        final int[] out = new int[AXES];
        out[from[0]] = sign[0] * x;
        out[from[1]] = sign[1] * y;
        out[from[2]] = sign[2] * z;
        return out;
    }

    private static int[] froms(final Orientation turn) {
        return new int[] {turn.xFrom(), turn.yFrom(), turn.zFrom()};
    }

    private static int[] signs(final Orientation turn) {
        return new int[] {turn.xSign(), turn.ySign(), turn.zSign()};
    }
}
