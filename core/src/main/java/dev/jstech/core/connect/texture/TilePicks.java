/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

/**
 * The tiles a face takes by where it is rather than by what it joins: one at random, the same for a place every time
 * it is drawn, as likely as its weight says; or a tile of a pattern laid over the world, so a floor or a wall shows a
 * picture bigger than a block. Pure.
 */
public final class TilePicks {

    /** Every face of a block picks for itself, the faces across from each other alike, or every face alike. */
    public static final int EACH_FACE = 0;
    public static final int OPPOSITE_FACES = 1;
    public static final int ALL_FACES = 2;

    /* Odd constants to mix a place into a seed, and the steps of the mixing. */
    private static final long MIX_X = 0x9E37_79B9_7F4A_7C15L;
    private static final long MIX_Y = 0xC2B2_AE3D_27D4_EB4FL;
    private static final long MIX_Z = 0x1656_67B1_9E37_79F9L;
    private static final long MIX_FACE = 0x27D4_EB2F_1656_67C5L;
    private static final long MIX_ONE = 0xBF58_476D_1CE4_E5B9L;
    private static final long MIX_TWO = 0x94D0_49BB_1331_11EBL;
    private static final int SHIFT_ONE = 30;
    private static final int SHIFT_TWO = 27;
    private static final int SHIFT_THREE = 31;
    private static final int FACES = 6;

    private TilePicks() {
    }

    /**
     * The seed of the face {@code face} (its index, 0 to 5, down, up, north, south, west, east) of the block at
     * {@code x}, {@code y}, {@code z}, as {@code symmetry} pairs the faces, mixed {@code loops} times more.
     */
    public static long seed(final int x, final int y, final int z, final int face, final int symmetry,
                            final int loops) {
        final int shared = switch (symmetry) {
            case OPPOSITE_FACES -> face / 2;
            case ALL_FACES -> 0;
            default -> Math.floorMod(face, FACES);
        };
        long seed = mix(x * MIX_X ^ y * MIX_Y ^ z * MIX_Z ^ shared * MIX_FACE);
        for (int i = 0; i < loops; i++) {
            seed = mix(seed);
        }
        return seed;
    }

    /** One of {@code weights.length} tiles picked by {@code seed}, each as likely as its weight; the first if all 0. */
    public static int weighted(final long seed, final int[] weights) {
        long total = 0;
        for (final int weight : weights) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return 0;
        }
        long left = Math.floorMod(seed, total);
        for (int i = 0; i < weights.length; i++) {
            left -= Math.max(0, weights[i]);
            if (left < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    /** A tile of {@code count}, each as likely, picked by {@code seed}. */
    public static int any(final long seed, final int count) {
        return count <= 0 ? 0 : (int) Math.floorMod(seed, (long) count);
    }

    /**
     * The tile of a pattern {@code width} by {@code height} tiles, numbered across then down, laid over a face at
     * {@code across} blocks to the right and {@code down} blocks down, moved by {@code offsetX} and {@code offsetY}.
     */
    public static int pattern(final int across, final int down, final int width, final int height,
                              final int offsetX, final int offsetY) {
        if (width <= 0 || height <= 0) {
            return 0;
        }
        return Math.floorMod(down + offsetY, height) * width + Math.floorMod(across + offsetX, width);
    }

    /* A strong mix of a long's bits, so places side by side pick unlike tiles. */
    private static long mix(final long value) {
        long z = value;
        z = (z ^ (z >>> SHIFT_ONE)) * MIX_ONE;
        z = (z ^ (z >>> SHIFT_TWO)) * MIX_TWO;
        return z ^ (z >>> SHIFT_THREE);
    }
}
