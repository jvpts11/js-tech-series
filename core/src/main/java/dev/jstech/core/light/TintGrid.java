/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import java.util.Arrays;

/**
 * The colour light gives the places around a block, the block's own and the 26 beside it, and between them: a corner
 * of one of its faces takes the colours of the places it looks into, blended by how near it is to each, so colour
 * fades smoothly across a floor as light does. A place no light reaches (inside a wall) has no say. Pure; colours are
 * {@code 0xRRGGBB}.
 */
public final class TintGrid {

    private final int[] colours;
    private final boolean[] lit;

    /** The places around a block, three a side. */
    public static final int SIZE = 27;

    private static final int CHANNEL = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final double NONE = 1e-6;

    /**
     * @param colours the colour at each place, by {@link #index}
     * @param lit     whether light reaches each place, by {@link #index}
     */
    public TintGrid(final int[] colours, final boolean[] lit) {
        if (colours.length != SIZE || lit.length != SIZE) {
            throw new IllegalArgumentException("a tint grid holds " + SIZE + " places, not " + colours.length);
        }
        this.colours = colours.clone();
        this.lit = lit.clone();
    }

    /** Where the place {@code dx, dy, dz} from the block (each -1, 0 or 1) is kept. */
    public static int index(final int dx, final int dy, final int dz) {
        return (dx + 1) * 9 + (dy + 1) * 3 + dz + 1;
    }

    /** Whether it gives nothing a colour. */
    public boolean plain() {
        for (int i = 0; i < SIZE; i++) {
            if (this.lit[i] && this.colours[i] != LightMath.WHITE) {
                return false;
            }
        }
        return true;
    }

    /**
     * The colour at {@code x, y, z} in the block's own measure (0 to 1 across it, out to -0.5 and 1.5 beside it):
     * the colours of the lit places nearest, blended by nearness; white where none is lit.
     */
    public int at(final double x, final double y, final double z) {
        final double u = Math.clamp(x - 0.5, -1.0, 1.0);
        final double v = Math.clamp(y - 0.5, -1.0, 1.0);
        final double w = Math.clamp(z - 0.5, -1.0, 1.0);
        final int i0 = low(u);
        final int j0 = low(v);
        final int k0 = low(w);
        final double fu = u - i0;
        final double fv = v - j0;
        final double fw = w - k0;
        double red = 0;
        double green = 0;
        double blue = 0;
        double weight = 0;
        for (int corner = 0; corner < 8; corner++) {
            final int i = i0 + (corner & 1);
            final int j = j0 + (corner >> 1 & 1);
            final int k = k0 + (corner >> 2 & 1);
            final int at = index(i, j, k);
            if (!this.lit[at]) {
                continue;
            }
            final double share = ((corner & 1) == 0 ? 1 - fu : fu) * ((corner >> 1 & 1) == 0 ? 1 - fv : fv)
                    * ((corner >> 2 & 1) == 0 ? 1 - fw : fw);
            final int colour = this.colours[at];
            red += share * (colour >> RED_SHIFT & CHANNEL);
            green += share * (colour >> GREEN_SHIFT & CHANNEL);
            blue += share * (colour & CHANNEL);
            weight += share;
        }
        if (weight < NONE) {
            return LightMath.WHITE;
        }
        return (int) Math.round(red / weight) << RED_SHIFT | (int) Math.round(green / weight) << GREEN_SHIFT
                | (int) Math.round(blue / weight);
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof TintGrid grid && Arrays.equals(this.colours, grid.colours)
                && Arrays.equals(this.lit, grid.lit);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.colours) * 31 + Arrays.hashCode(this.lit);
    }

    /* The lower of the two places a point between them is blended from, so the upper is at most 1. */
    private static int low(final double along) {
        return Math.min(0, (int) Math.floor(along));
    }
}
