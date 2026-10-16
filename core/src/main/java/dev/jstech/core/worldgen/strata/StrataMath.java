/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.strata;

import java.util.List;

/**
 * Rock in layers: which layer lies at a depth under the ground, its boundaries rising and falling gently across the
 * land by a smooth noise, and which of a kind's sequences of layers a region of the world has, regions meeting along
 * wavy borders. Worked out from the seed alone, the same on any thread. Pure.
 */
public final class StrataMath {

    private static final long MIX_X = -7_046_029_254_386_353_131L;
    private static final long MIX_Z = -4_417_276_706_812_531_889L;
    private static final long MIX_SALT = 6_364_136_223_846_793_005L;
    private static final double TO_UNIT = 1.0 / (1L << 53);

    private StrataMath() {
    }

    /**
     * Which of the layers {@code thicknesses} thick, from the top down, lies {@code depth} blocks under the ground;
     * -1 below the last.
     */
    public static int layerAt(final double depth, final List<Integer> thicknesses) {
        if (depth < 0) {
            return -1;
        }
        double bottom = 0;
        for (int i = 0; i < thicknesses.size(); i++) {
            bottom += thicknesses.get(i);
            if (depth < bottom) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A smooth noise at {@code x, z}, from -1 to 1, changing over about {@code scale} blocks: each corner of a grid
     * of that size a value of its own, blended between them.
     */
    public static double noise(final long seed, final double x, final double z, final double scale) {
        final double gx = x / scale;
        final double gz = z / scale;
        final long x0 = (long) Math.floor(gx);
        final long z0 = (long) Math.floor(gz);
        final double fx = smooth(gx - x0);
        final double fz = smooth(gz - z0);
        final double a = corner(seed, x0, z0);
        final double b = corner(seed, x0 + 1, z0);
        final double c = corner(seed, x0, z0 + 1);
        final double d = corner(seed, x0 + 1, z0 + 1);
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    /**
     * Which of {@code sequences} sequences a region of the world holds at {@code x, z}: regions about {@code size}
     * blocks across, their borders wavy so none runs straight.
     */
    public static int region(final long seed, final double x, final double z, final double size, final int sequences) {
        if (sequences <= 1) {
            return 0;
        }
        final double wx = x + noise(seed ^ MIX_SALT, x, z, size / 2) * size / 3;
        final double wz = z + noise(seed ^ MIX_X, x, z, size / 2) * size / 3;
        final long cx = (long) Math.floor(wx / size);
        final long cz = (long) Math.floor(wz / size);
        return (int) Math.floorMod(hash(seed, cx, cz), sequences);
    }

    private static double corner(final long seed, final long x, final long z) {
        return (hash(seed, x, z) >>> 11) * TO_UNIT * 2 - 1;
    }

    private static long hash(final long seed, final long x, final long z) {
        long h = seed ^ x * MIX_X ^ z * MIX_Z;
        h = (h ^ h >>> 33) * MIX_SALT;
        h = (h ^ h >>> 29) * MIX_X;
        return h ^ h >>> 32;
    }

    /* An easing of the blend, so the noise has no creases at the grid's lines. */
    private static double smooth(final double t) {
        return t * t * (3 - 2 * t);
    }
}
