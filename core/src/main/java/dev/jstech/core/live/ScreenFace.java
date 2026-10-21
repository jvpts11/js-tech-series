/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import java.util.Optional;

/**
 * Where on a block's face a point of it is as the face is seen from in front of it: across from its left, down from
 * its top, each from 0 to 1; and which pixel of a screen on part of the face that point is. A face looking north is
 * seen by someone standing north of it and looking south, so its left is to the east.
 *
 * <p>Pure: no game types, so the arithmetic is checked without the game.
 */
public final class ScreenFace {

    private ScreenFace() {
    }

    /** The way a face looks out of its block. */
    public enum Side {
        NORTH, SOUTH, EAST, WEST, UP, DOWN
    }

    /**
     * Where the point {@code (x, y, z)} of the block, each 0 to 1 from the block's corner, is on its face looking
     * {@code side}, as {@code {across, down}}: across from the face's left as seen in front of it, down from its top.
     * A face looking up is seen from above with north at its top, one looking down from below with south at it.
     */
    public static double[] point(final Side side, final double x, final double y, final double z) {
        return switch (side) {
            case NORTH -> new double[] {1 - x, 1 - y};
            case SOUTH -> new double[] {x, 1 - y};
            case EAST -> new double[] {1 - z, 1 - y};
            case WEST -> new double[] {z, 1 - y};
            case UP -> new double[] {x, z};
            case DOWN -> new double[] {x, 1 - z};
        };
    }

    /**
     * The pixel of a screen on {@code area} of a face that {@code point} of the face is on, as {@code {x, y}} from the
     * screen's top left; empty off the screen.
     */
    public static Optional<int[]> pixel(final TouchArea area, final double[] point) {
        final double across = (point[0] - area.left()) / (area.right() - area.left());
        final double down = (point[1] - area.top()) / (area.bottom() - area.top());
        if (across < 0 || across >= 1 || down < 0 || down >= 1) {
            return Optional.empty();
        }
        return Optional.of(new int[] {(int) (across * area.width()), (int) (down * area.height())});
    }
}
