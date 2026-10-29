/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import java.util.List;

/**
 * How loose stones lie on the ground of a block: one to three small stones, each of one or two boxes, never a whole
 * block, measured in sixteenths of a block as they lie facing north. The same boxes draw a stone's model and make its
 * outline, so what is seen is what is pointed at.
 */
public final class StoneArrangements {

    /** The arrangements, the first a stone with a smaller one on it, which its item shows. */
    public static final List<List<Box>> ALL = List.of(
            // A stone with a smaller one lying on it.
            List.of(new Box(2, 0, 3, 13, 6, 13), new Box(4, 6, 5, 10, 9, 10)),
            // One small stone.
            List.of(new Box(5, 0, 6, 10, 3, 10)),
            // Two small stones.
            List.of(new Box(2, 0, 3, 7, 3, 7), new Box(9, 0, 8, 13, 2, 12)),
            // Three small stones.
            List.of(new Box(2, 0, 9, 6, 2, 13), new Box(7, 0, 2, 13, 4, 7), new Box(10, 0, 10, 14, 3, 14)));
    /** A block's side, in sixteenths. */
    public static final int BLOCK = 16;

    private StoneArrangements() {
    }

    /**
     * A box of a stone, from its lower corner to its upper, in sixteenths of a block.
     *
     * @param x0 the west side
     * @param y0 the bottom
     * @param z0 the north side
     * @param x1 the east side
     * @param y1 the top
     * @param z1 the south side
     */
    public record Box(int x0, int y0, int z0, int x1, int y1, int z1) {

        /**
         * The box turned {@code quarterTurns} quarters clockwise about the block's middle, seen from above, as a
         * block state turns a model: a quarter turn takes the north side to the east.
         */
        public Box turned(final int quarterTurns) {
            Box box = this;
            for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
                box = new Box(BLOCK - box.z1, box.y0, box.x0, BLOCK - box.z0, box.y1, box.x1);
            }
            return box;
        }

        /** Whether it overlaps {@code other} by more than a face. */
        public boolean overlaps(final Box other) {
            return this.x0 < other.x1 && other.x0 < this.x1 && this.y0 < other.y1 && other.y0 < this.y1
                    && this.z0 < other.z1 && other.z0 < this.z1;
        }
    }
}
