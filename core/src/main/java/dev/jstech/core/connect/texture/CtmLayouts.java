/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import dev.jstech.core.connect.ConnectedQuadrants;

/**
 * Where a face takes its picture from in each type of the connected textures resource packs write for CTM, from the
 * neighbours it joins, a mask of eight bits clockwise from up as {@link ConnectedQuadrants} numbers them. A picture is
 * a cell of a texture laid out as a grid, {@link Cell}. Pure.
 *
 * <ul>
 *   <li>{@code ctm}: a quarter of the face at a time, from the texture itself where neither edge beside the quarter is
 *       joined, else from its companion, two by two tiles: joined all round, joined up and down, joined sideways, and
 *       joined at both edges but not the corner;</li>
 *   <li>{@code ctm_horizontal} and {@code ctm_vertical}: the whole face from the texture's two by two: alone, the
 *       middle, and the two ends;</li>
 *   <li>{@code ctm_simple}: the whole face from two by two: not joined all round, joined sideways, joined up and down,
 *       and joined all round with every corner;</li>
 *   <li>{@code edges_full}: the whole face from four by four, by where the blocks it changes for meet it.</li>
 * </ul>
 */
public final class CtmLayouts {

    /** The texture itself, and the first of its companions. */
    public static final int OWN = 0;
    public static final int COMPANION = 1;

    private static final int UP = ConnectedQuadrants.UP;
    private static final int UP_RIGHT = ConnectedQuadrants.UP_RIGHT;
    private static final int RIGHT = ConnectedQuadrants.RIGHT;
    private static final int DOWN_RIGHT = ConnectedQuadrants.DOWN_RIGHT;
    private static final int DOWN = ConnectedQuadrants.DOWN;
    private static final int DOWN_LEFT = ConnectedQuadrants.DOWN_LEFT;
    private static final int LEFT = ConnectedQuadrants.LEFT;
    private static final int UP_LEFT = ConnectedQuadrants.UP_LEFT;
    private static final int HALVES = 2;
    private static final int QUARTERS = 4;
    /* The quarters of a face, as {@link ConnectedQuadrants} orders them, with their edges and corner. */
    private static final int[][] QUARTER_NEIGHBOURS = {
        {UP, LEFT, UP_LEFT}, {UP, RIGHT, UP_RIGHT}, {DOWN, RIGHT, DOWN_RIGHT}, {DOWN, LEFT, DOWN_LEFT}
    };
    private static final int[][] QUARTER_AT = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    private CtmLayouts() {
    }

    /**
     * The {@code ctm} type's picture for quarter {@code quarter} (up-left, up-right, down-right, down-left) of a face
     * joined as {@code mask}: the quarter of a tile, each tile half of its texture each way.
     */
    public static Cell quarter(final int quarter, final int mask) {
        final int[] near = QUARTER_NEIGHBOURS[quarter];
        final int x = QUARTER_AT[quarter][0];
        final int y = QUARTER_AT[quarter][1];
        final boolean upOrDown = has(mask, near[0]);
        final boolean sideways = has(mask, near[1]);
        if (!upOrDown && !sideways) {
            return new Cell(OWN, x, y, HALVES, HALVES);
        }
        final int tileX = upOrDown && !(sideways && has(mask, near[2])) ? 1 : 0;
        final int tileY = sideways && !(upOrDown && has(mask, near[2])) ? 1 : 0;
        // Joined all round: the companion's first tile; up and down only its second; sideways only its third; both
        // edges but not the corner its fourth.
        return new Cell(COMPANION, tileX * HALVES + x, tileY * HALVES + y, QUARTERS, QUARTERS);
    }

    /**
     * The {@code edges} type's picture for a quarter: as {@link #quarter}, but a quarter whose corner alone meets the
     * blocks it changes for, neither edge beside it, takes its corner's piece of the tile joined all round.
     */
    public static Cell edgesQuarter(final int quarter, final int mask) {
        final int[] near = QUARTER_NEIGHBOURS[quarter];
        if (!has(mask, near[0]) && !has(mask, near[1]) && has(mask, near[2])) {
            return new Cell(COMPANION, QUARTER_AT[quarter][0], QUARTER_AT[quarter][1], QUARTERS, QUARTERS);
        }
        return quarter(quarter, mask);
    }

    /** The {@code ctm_horizontal} type's picture: alone, the middle, the right end, the left end. */
    public static Cell horizontal(final int mask) {
        final boolean left = has(mask, LEFT);
        final boolean right = has(mask, RIGHT);
        return new Cell(OWN, left ? 1 : 0, left == right ? 0 : 1, HALVES, HALVES);
    }

    /** The {@code ctm_vertical} type's picture: alone, the middle, the bottom end, the top end. */
    public static Cell vertical(final int mask) {
        final boolean up = has(mask, UP);
        return new Cell(OWN, up == has(mask, DOWN) ? 0 : 1, up ? 1 : 0, HALVES, HALVES);
    }

    /** The {@code ctm_simple} type's picture. */
    public static Cell simple(final int mask) {
        final boolean up = has(mask, UP);
        final boolean down = has(mask, DOWN);
        final boolean left = has(mask, LEFT);
        final boolean right = has(mask, RIGHT);
        if (!up || !down) {
            return new Cell(OWN, left && right ? 1 : 0, 0, HALVES, HALVES);
        }
        if (!left || !right) {
            return new Cell(OWN, 0, 1, HALVES, HALVES);
        }
        final boolean corners = has(mask, UP_LEFT) && has(mask, UP_RIGHT) && has(mask, DOWN_LEFT)
                && has(mask, DOWN_RIGHT);
        return new Cell(OWN, corners ? 1 : 0, corners ? 1 : 0, HALVES, HALVES);
    }

    /**
     * The {@code edges_full} type's picture from its companion's four by four, for a face that meets the blocks it
     * changes for as {@code mask}, or wholly when it is {@code obscured} by one; none for a face that meets none,
     * which keeps its own texture.
     */
    public static Cell edgesFull(final int mask, final boolean obscured) {
        if (!obscured && mask == 0) {
            return new Cell(OWN, 0, 0, 1, 1);
        }
        final boolean up = has(mask, UP) || has(mask, UP_LEFT) && has(mask, UP_RIGHT);
        final boolean right = has(mask, RIGHT) || has(mask, UP_RIGHT) && has(mask, DOWN_RIGHT);
        final boolean down = has(mask, DOWN) || has(mask, DOWN_LEFT) && has(mask, DOWN_RIGHT);
        final boolean left = has(mask, LEFT) || has(mask, UP_LEFT) && has(mask, DOWN_LEFT);
        final boolean none = !(up || right || down || left);
        final int[] at;
        if (obscured || up && down || right && left) {
            at = new int[] {1, 2};
        } else if (none && has(mask, UP_LEFT) && has(mask, DOWN_RIGHT)) {
            at = new int[] {1, 0};
        } else if (none && has(mask, UP_RIGHT) && has(mask, DOWN_LEFT)) {
            at = new int[] {2, 0};
        } else {
            at = edgesFullCorner(mask, up, right, down, left);
        }
        return new Cell(COMPANION, at[0], at[1], QUARTERS, QUARTERS);
    }

    /* The rest of {@code edges_full}: an inner corner of two edges, one edge, or one corner alone. */
    private static int[] edgesFullCorner(final int mask, final boolean up, final boolean right, final boolean down,
                                         final boolean left) {
        if (!(down || right) && (has(mask, LEFT) || has(mask, DOWN_LEFT)) && (has(mask, UP) || has(mask, UP_RIGHT))) {
            return new int[] {3, 0};
        }
        if (!(down || left) && (has(mask, UP) || has(mask, UP_LEFT)) && (has(mask, RIGHT) || has(mask, DOWN_RIGHT))) {
            return new int[] {3, 1};
        }
        if (!(up || left) && (has(mask, RIGHT) || has(mask, UP_RIGHT)) && (has(mask, DOWN) || has(mask, DOWN_LEFT))) {
            return new int[] {3, 2};
        }
        if (!(up || right) && (has(mask, DOWN) || has(mask, DOWN_RIGHT)) && (has(mask, LEFT) || has(mask, UP_LEFT))) {
            return new int[] {3, 3};
        }
        if (down) {
            return new int[] {1, 1};
        }
        if (right) {
            return new int[] {0, 2};
        }
        if (left) {
            return new int[] {2, 2};
        }
        if (up) {
            return new int[] {1, 3};
        }
        if (has(mask, DOWN_LEFT)) {
            return new int[] {2, 1};
        }
        if (has(mask, DOWN_RIGHT)) {
            return new int[] {0, 1};
        }
        if (has(mask, UP_RIGHT)) {
            return new int[] {0, 3};
        }
        return has(mask, UP_LEFT) ? new int[] {2, 3} : new int[] {0, 0};
    }

    private static boolean has(final int mask, final int bit) {
        return (mask & bit) != 0;
    }

    /**
     * A cell of a texture laid out as a grid of {@code columns} by {@code rows}.
     *
     * @param texture which texture: {@link #OWN}, or a companion, from {@link #COMPANION}
     * @param x       its column, from the left
     * @param y       its row, from the top
     * @param columns how many columns the texture is laid out in
     * @param rows    how many rows
     */
    public record Cell(int texture, int x, int y, int columns, int rows) {

        /** Where the cell starts across its texture, from 0 to 1. */
        public float u0() {
            return (float) this.x / this.columns;
        }

        /** Where it starts down its texture. */
        public float v0() {
            return (float) this.y / this.rows;
        }

        /** How wide it is, a share of its texture. */
        public float width() {
            return 1.0F / this.columns;
        }

        /** How tall it is. */
        public float height() {
            return 1.0F / this.rows;
        }
    }
}
