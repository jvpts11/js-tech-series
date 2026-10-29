/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import java.util.Arrays;

/**
 * A map of a round world, as data: a value from 0 to 1 at every longitude and latitude, read off a picture laid as the
 * maps worlds' spheres are drawn with, longitude from -180 at its left edge to 180 at its right and latitude from 90 at
 * its top to -90 at its bottom. It is read smoothly between its texels, and across the edge where -180 meets 180, so
 * a texel thousands of blocks wide shades into the next rather than stepping.
 *
 * <p>A round world lays its blocks on its sphere so: X is the longitude, 0 at x 0 and 180 at the world's east end, and
 * Z the latitude, north towards -Z, the world half as long north to south as it is wide.
 */
public final class RoundMap {

    private final int width;
    private final int height;
    private final float[] values;

    /** A map {@code width} texels across and {@code height} down, its values row by row from the top. */
    public RoundMap(final int width, final int height, final float[] values) {
        if (width <= 0 || height <= 0 || values.length != width * height) {
            throw new IllegalArgumentException("a map of " + width + " by " + height + " holds " + width * height
                    + " values, not " + values.length);
        }
        this.width = width;
        this.height = height;
        this.values = Arrays.copyOf(values, values.length);
    }

    /** The longitude, in degrees, of block {@code x} on a round world {@code worldWidth} blocks across. */
    public static double longitude(final double x, final double worldWidth) {
        return x / worldWidth * 360;
    }

    /** The latitude, in degrees, of block {@code z} on a round world {@code worldWidth} blocks across. */
    public static double latitude(final double z, final double worldWidth) {
        return -z / (worldWidth / 2) * 180;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /** The map's value at {@code longitude} and {@code latitude}, in degrees, between its texels' values. */
    public double at(final double longitude, final double latitude) {
        final double u = (longitude + 180) / 360 * this.width - 0.5;
        final double v = (90 - latitude) / 180 * this.height - 0.5;
        final int x0 = (int) Math.floor(u);
        final int y0 = (int) Math.floor(v);
        final double fx = u - x0;
        final double fy = v - y0;
        final double top = texel(x0, y0) * (1 - fx) + texel(x0 + 1, y0) * fx;
        final double bottom = texel(x0, y0 + 1) * (1 - fx) + texel(x0 + 1, y0 + 1) * fx;
        return top * (1 - fy) + bottom * fy;
    }

    /** The map's value at block {@code x}, {@code z} of a round world {@code worldWidth} blocks across. */
    public double atBlock(final double x, final double z, final double worldWidth) {
        return at(longitude(x, worldWidth), latitude(z, worldWidth));
    }

    /* A texel's value, round the world east and west, held at the poles north and south. */
    private double texel(final int x, final int y) {
        return this.values[Math.max(0, Math.min(this.height - 1, y)) * this.width + Math.floorMod(x, this.width)];
    }
}
