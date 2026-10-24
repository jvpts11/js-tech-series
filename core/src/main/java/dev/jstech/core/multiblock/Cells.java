/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import org.jetbrains.annotations.Nullable;

/** The arithmetic of places the shapes share: moving a packed place, reading its block, saying what is wrong. Pure. */
final class Cells {

    /** The block an empty place reads as. */
    static final String AIR = "minecraft:air";
    /* The six ways out of a place: east, west, up, down, south, north. */
    private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private Cells() {
    }

    /** The place {@code dx}, {@code dy}, {@code dz} away from {@code pos}. */
    static long offset(final long pos, final int dx, final int dy, final int dz) {
        final int[] xyz = PatternMatcher.decodePosition(pos);
        return PatternMatcher.encodePosition(xyz[0] + dx, xyz[1] + dy, xyz[2] + dz);
    }

    /** The six places beside {@code pos}. */
    static long[] beside(final long pos) {
        final int[] xyz = PatternMatcher.decodePosition(pos);
        final long[] out = new long[SIDES.length];
        for (int i = 0; i < SIDES.length; i++) {
            out[i] = PatternMatcher.encodePosition(xyz[0] + SIDES[i][0], xyz[1] + SIDES[i][1], xyz[2] + SIDES[i][2]);
        }
        return out;
    }

    /** The block at {@code pos}, air when the world names none. */
    static String blockAt(final IBlockProvider provider, final long pos) {
        final String id = provider.blockAt(pos);
        return id == null ? AIR : id;
    }

    /** A place holding the wrong block. */
    static ShapeMatch.Failure wrongBlock(final long terminal, final long at, final String wanted,
                                         final String found) {
        final int[] c = PatternMatcher.decodePosition(terminal);
        final int[] p = PatternMatcher.decodePosition(at);
        return new ShapeMatch.Failure(ShapeMatch.Wrong.BLOCK, at, p[0] - c[0], p[1] - c[1], p[2] - c[2], wanted,
                found);
    }

    /** A shape too small or too large along {@code which}. */
    static ShapeMatch.Failure wrongSize(final long terminal, final String which, final int size) {
        return new ShapeMatch.Failure(ShapeMatch.Wrong.SIZE, terminal, 0, 0, 0, which, Integer.toString(size));
    }

    /**
     * The failure of the attempt that went furthest before failing, the earliest of equals: of a structure's
     * orientations, the one the player most likely meant, so the place it names is one the player can see is wrong.
     */
    static final class Best {

        @Nullable
        private ShapeMatch.Failure failure;
        private int progress = -1;

        /** Offers the failure of an attempt that matched {@code progress} places first. */
        void offer(final ShapeMatch.Failure failed, final int matched) {
            if (matched > this.progress) {
                this.failure = failed;
                this.progress = matched;
            }
        }

        /** The failure kept. */
        ShapeMatch.Failure failure() {
            if (this.failure == null) {
                throw new IllegalStateException("no attempt was offered");
            }
            return this.failure;
        }
    }
}
