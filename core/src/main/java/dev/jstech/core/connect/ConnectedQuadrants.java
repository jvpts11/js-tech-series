/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

/**
 * How a face of connected texture is drawn from five tiles, a quarter of the face at a time. Each quarter looks at the
 * two neighbours along its edges and the one at its corner, and takes the matching quarter of one tile:
 *
 * <ul>
 *   <li>{@link #ALONE}: joined along neither edge, the tile with a border all round;</li>
 *   <li>{@link #ACROSS}: joined sideways only, the tile with borders above and below;</li>
 *   <li>{@link #UPRIGHT}: joined up or down only, the tile with borders left and right;</li>
 *   <li>{@link #INNER}: joined along both edges but not at the corner, the tile with a notch in each corner;</li>
 *   <li>{@link #WHOLE}: joined all round, the tile with no border.</li>
 * </ul>
 *
 * <p>The neighbours come as a mask of eight bits, clockwise from up, as {@link ConnectedFaces} works them out. This
 * holds nothing of the game.
 */
public final class ConnectedQuadrants {

    public static final int UP = 1;
    public static final int UP_RIGHT = 1 << 1;
    public static final int RIGHT = 1 << 2;
    public static final int DOWN_RIGHT = 1 << 3;
    public static final int DOWN = 1 << 4;
    public static final int DOWN_LEFT = 1 << 5;
    public static final int LEFT = 1 << 6;
    public static final int UP_LEFT = 1 << 7;

    public static final int ALONE = 0;
    public static final int ACROSS = 1;
    public static final int UPRIGHT = 2;
    public static final int INNER = 3;
    public static final int WHOLE = 4;
    /** How many tiles a connected texture is drawn from. */
    public static final int TILES = 5;

    private ConnectedQuadrants() {
    }

    /** The tile a quarter takes, joined sideways or not, up or down or not, and at its corner or not. */
    public static int tile(final boolean sideways, final boolean upOrDown, final boolean corner) {
        if (sideways && upOrDown) {
            return corner ? WHOLE : INNER;
        }
        if (sideways) {
            return ACROSS;
        }
        return upOrDown ? UPRIGHT : ALONE;
    }

    /** The tiles of a face's four quarters, joined as {@code mask} says: up-left, up-right, down-right, down-left. */
    public static int[] tiles(final int mask) {
        return new int[] {
            tile(has(mask, LEFT), has(mask, UP), has(mask, UP_LEFT)),
            tile(has(mask, RIGHT), has(mask, UP), has(mask, UP_RIGHT)),
            tile(has(mask, RIGHT), has(mask, DOWN), has(mask, DOWN_RIGHT)),
            tile(has(mask, LEFT), has(mask, DOWN), has(mask, DOWN_LEFT))
        };
    }

    private static boolean has(final int mask, final int bit) {
        return (mask & bit) != 0;
    }
}
