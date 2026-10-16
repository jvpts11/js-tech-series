/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import dev.jstech.core.connect.ConnectedQuadrants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Which tile a face takes in each method of the connected textures resource packs write for OptiFine and Continuity,
 * from the neighbours it joins: a mask of eight bits, clockwise from up, as {@link ConnectedQuadrants} numbers them.
 * The tiles are numbered as those packs' templates number them. Pure.
 *
 * <ul>
 *   <li>{@code ctm}: 47 tiles, one for each way a face can be joined once a corner is left out where either edge
 *       beside it is not joined;</li>
 *   <li>{@code ctm_compact}: 5 tiles, drawn a quarter of the face at a time;</li>
 *   <li>{@code horizontal} and {@code vertical}: 4 tiles, joined along one way only; {@code horizontal+vertical} and
 *       {@code vertical+horizontal}: 7, joined one way first and the other when not;</li>
 *   <li>{@code overlay}: 17 tiles laid over a face where blocks of another kind meet it, at its edges and corners.</li>
 * </ul>
 */
public final class OptifineLayouts {

    /** How many tiles each method takes. */
    public static final int FULL_TILES = 47;
    public static final int COMPACT_TILES = 5;
    public static final int LINE_TILES = 4;
    public static final int LINES_TILES = 7;
    public static final int OVERLAY_TILES = 17;

    private static final int UP = ConnectedQuadrants.UP;
    private static final int UP_RIGHT = ConnectedQuadrants.UP_RIGHT;
    private static final int RIGHT = ConnectedQuadrants.RIGHT;
    private static final int DOWN_RIGHT = ConnectedQuadrants.DOWN_RIGHT;
    private static final int DOWN = ConnectedQuadrants.DOWN;
    private static final int DOWN_LEFT = ConnectedQuadrants.DOWN_LEFT;
    private static final int LEFT = ConnectedQuadrants.LEFT;
    private static final int UP_LEFT = ConnectedQuadrants.UP_LEFT;
    private static final int MASKS = 256;
    /*
     * The 47 tiles of the template, each as the mask it stands for once reduced: a corner only where both edges beside
     * it are joined. Read from the template's picture, a border where an edge is not joined and a notch in a corner
     * that is not.
     */
    private static final int[] FULL = {
        0, RIGHT, RIGHT | LEFT, LEFT, RIGHT | DOWN, DOWN | LEFT, UP | RIGHT | DOWN, RIGHT | DOWN | LEFT,
        UP | RIGHT | DOWN | LEFT | UP_RIGHT, UP | RIGHT | DOWN | LEFT | DOWN_RIGHT,
        UP | RIGHT | DOWN | LEFT | UP_LEFT | DOWN_LEFT, UP | RIGHT | DOWN | LEFT | UP_LEFT | UP_RIGHT,
        DOWN, RIGHT | DOWN | DOWN_RIGHT, RIGHT | DOWN | LEFT | DOWN_RIGHT | DOWN_LEFT, DOWN | LEFT | DOWN_LEFT,
        UP | RIGHT, UP | LEFT, UP | RIGHT | LEFT, UP | DOWN | LEFT,
        UP | RIGHT | DOWN | LEFT | UP_LEFT, UP | RIGHT | DOWN | LEFT | DOWN_LEFT,
        UP | RIGHT | DOWN | LEFT | DOWN_RIGHT | DOWN_LEFT, UP | RIGHT | DOWN | LEFT | UP_RIGHT | DOWN_RIGHT,
        UP | DOWN, UP | RIGHT | DOWN | UP_RIGHT | DOWN_RIGHT, MASKS - 1, UP | DOWN | LEFT | UP_LEFT | DOWN_LEFT,
        UP | RIGHT | DOWN | DOWN_RIGHT, RIGHT | DOWN | LEFT | DOWN_LEFT, UP | RIGHT | DOWN | UP_RIGHT,
        RIGHT | DOWN | LEFT | DOWN_RIGHT,
        UP | RIGHT | DOWN | LEFT | UP_LEFT | UP_RIGHT | DOWN_LEFT, UP | RIGHT | DOWN | LEFT | UP_LEFT | UP_RIGHT
                | DOWN_RIGHT,
        UP | RIGHT | DOWN | LEFT | UP_RIGHT | DOWN_LEFT, UP | RIGHT | DOWN | LEFT | UP_LEFT | DOWN_RIGHT,
        UP, UP | RIGHT | UP_RIGHT, UP | RIGHT | LEFT | UP_LEFT | UP_RIGHT, UP | LEFT | UP_LEFT,
        UP | RIGHT | LEFT | UP_RIGHT, UP | DOWN | LEFT | UP_LEFT, UP | RIGHT | LEFT | UP_LEFT,
        UP | DOWN | LEFT | DOWN_LEFT,
        UP | RIGHT | DOWN | LEFT | UP_LEFT | DOWN_RIGHT | DOWN_LEFT, UP | RIGHT | DOWN | LEFT | UP_RIGHT | DOWN_RIGHT
                | DOWN_LEFT,
        UP | RIGHT | DOWN | LEFT
    };
    /* The tile of every mask, worked out once from the template. */
    private static final int[] FULL_BY_MASK = fullByMask();
    /* The compact method's tiles, as the five of {@link ConnectedQuadrants} are numbered there. */
    private static final int[] COMPACT_BY_QUADRANT_TILE = {0, 3, 2, 4, 1};
    /* The overlay's tiles at its corners, and for the edges it meets, by those edges as a mask. */
    private static final int OVERLAY_DOWN_RIGHT = 0;
    private static final int OVERLAY_DOWN_LEFT = 2;
    private static final int OVERLAY_UP_RIGHT = 14;
    private static final int OVERLAY_UP_LEFT = 16;

    private OptifineLayouts() {
    }

    /** {@code mask} with each corner left out where either edge beside it is not joined. */
    public static int reduce(final int mask) {
        int out = mask & (UP | RIGHT | DOWN | LEFT);
        out |= corner(mask, UP, RIGHT, UP_RIGHT);
        out |= corner(mask, DOWN, RIGHT, DOWN_RIGHT);
        out |= corner(mask, DOWN, LEFT, DOWN_LEFT);
        out |= corner(mask, UP, LEFT, UP_LEFT);
        return out;
    }

    /** The tile of the {@code ctm} method's 47 for a face joined as {@code mask}. */
    public static int full(final int mask) {
        return FULL_BY_MASK[mask & (MASKS - 1)];
    }

    /** The mask the tile {@code tile} of the {@code ctm} method's 47 stands for, reduced. */
    public static int maskOfFull(final int tile) {
        return FULL[tile];
    }

    /**
     * The {@code ctm_compact} method's tiles for the four quarters of a face joined as {@code mask}: up-left,
     * up-right, down-right, down-left.
     */
    public static int[] compact(final int mask) {
        final int[] quarters = ConnectedQuadrants.tiles(mask);
        for (int i = 0; i < quarters.length; i++) {
            quarters[i] = COMPACT_BY_QUADRANT_TILE[quarters[i]];
        }
        return quarters;
    }

    /** The {@code horizontal} method's tile: joined on the right only, both ways, on the left only, or neither. */
    public static int horizontal(final int mask) {
        return line(has(mask, LEFT), has(mask, RIGHT));
    }

    /** The {@code vertical} method's tile: joined above only, both ways, below only, or neither. */
    public static int vertical(final int mask) {
        return line(has(mask, DOWN), has(mask, UP));
    }

    /** The {@code horizontal+vertical} method's tile: joined sideways as {@code horizontal}, else up or down. */
    public static int horizontalVertical(final int mask) {
        if (has(mask, LEFT) || has(mask, RIGHT)) {
            return horizontal(mask);
        }
        return secondLine(has(mask, DOWN), has(mask, UP));
    }

    /** The {@code vertical+horizontal} method's tile: joined up or down as {@code vertical}, else sideways. */
    public static int verticalHorizontal(final int mask) {
        if (has(mask, UP) || has(mask, DOWN)) {
            return vertical(mask);
        }
        return secondLine(has(mask, LEFT), has(mask, RIGHT));
    }

    /**
     * The {@code overlay} method's tiles laid over a face where the neighbours in {@code sources} are of the kind
     * that overlays it: one for the edges it meets, two for opposite edges alone, and one for each corner it meets
     * where neither edge beside it does. None where it meets none.
     */
    public static int[] overlay(final int sources) {
        final boolean up = has(sources, UP);
        final boolean right = has(sources, RIGHT);
        final boolean down = has(sources, DOWN);
        final boolean left = has(sources, LEFT);
        final List<Integer> tiles = new ArrayList<>();
        edges(up, right, down, left, tiles);
        if (!up && !right && has(sources, UP_RIGHT)) {
            tiles.add(OVERLAY_UP_RIGHT);
        }
        if (!down && !right && has(sources, DOWN_RIGHT)) {
            tiles.add(OVERLAY_DOWN_RIGHT);
        }
        if (!down && !left && has(sources, DOWN_LEFT)) {
            tiles.add(OVERLAY_DOWN_LEFT);
        }
        if (!up && !left && has(sources, UP_LEFT)) {
            tiles.add(OVERLAY_UP_LEFT);
        }
        return tiles.stream().mapToInt(Integer::intValue).toArray();
    }

    /* The overlay's tile for the edges it meets, by the template: all four, three, two beside each other, or one. */
    private static void edges(final boolean up, final boolean right, final boolean down, final boolean left,
                              final List<Integer> tiles) {
        final int count = (up ? 1 : 0) + (right ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0);
        if (count == 4) {
            tiles.add(8);
        } else if (count == 3) {
            tiles.add(!up ? 5 : !right ? 6 : !left ? 12 : 13);
        } else if (count == 2 && up != down) {
            tiles.add(up ? (right ? 10 : 11) : (right ? 3 : 4));
        } else {
            // One edge, or two across from each other, each its own tile.
            if (up) {
                tiles.add(15);
            }
            if (right) {
                tiles.add(7);
            }
            if (down) {
                tiles.add(1);
            }
            if (left) {
                tiles.add(9);
            }
        }
    }

    /* A line's tile: the end joined towards {@code to} only, the middle, the end joined towards {@code from}. */
    private static int line(final boolean from, final boolean to) {
        if (from && to) {
            return 1;
        }
        if (to) {
            return 0;
        }
        return from ? 2 : 3;
    }

    /* The second line of a seven-tile method, after its first four: one way only, both, the other only, neither. */
    private static int secondLine(final boolean from, final boolean to) {
        if (from && to) {
            return 5;
        }
        if (to) {
            return 4;
        }
        return from ? 6 : 3;
    }

    private static int corner(final int mask, final int edge, final int other, final int corner) {
        return has(mask, edge) && has(mask, other) && has(mask, corner) ? corner : 0;
    }

    private static boolean has(final int mask, final int bit) {
        return (mask & bit) != 0;
    }

    private static int[] fullByMask() {
        final int[] byMask = new int[MASKS];
        Arrays.fill(byMask, -1);
        for (int mask = 0; mask < MASKS; mask++) {
            final int reduced = reduce(mask);
            for (int tile = 0; tile < FULL.length; tile++) {
                if (FULL[tile] == reduced) {
                    byMask[mask] = tile;
                    break;
                }
            }
        }
        return byMask;
    }
}
