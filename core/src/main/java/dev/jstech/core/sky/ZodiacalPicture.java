/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

/**
 * The picture of the zodiacal light, the dust round a sun lit by it along its worlds' plane: a map of the sky round
 * the sun, in elongation along the sun's path, from {@link #REACH} before the sun to as far after it, across, and in
 * latitude from the path, {@link #WIDE} north to south, down. Its light falls away from the sun and widens as it goes,
 * a cone standing up from the horizon along the path before sunrise and after sunset; it comes in sixteen levels,
 * dithered so the levels never draw contour lines. Pure: no game types.
 */
public final class ZodiacalPicture {

    /** How far along the sun's path the light reaches either side of the sun, in degrees. */
    public static final double REACH = 110.0;
    /** How far north and south of the path the picture reaches, in degrees. */
    public static final double WIDE = 40.0;
    private static final int LEVELS = 16;
    private static final double[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    /* How fast it fades along the path; how wide it is at the sun and how much wider it grows; where it ends. */
    private static final double FADING = 22.0;
    private static final double WIDTH = 7.0;
    private static final double WIDENING = 0.25;
    private static final double ENDING = 30.0;

    private ZodiacalPicture() {
    }

    /** The picture, {@code width} texels across and {@code height} down, each its light, 0 to 1. */
    public static double[] draw(final int width, final int height) {
        final double[] out = new double[width * height];
        for (int y = 0; y < height; y++) {
            final double beta = WIDE - (y + 0.5) * 2 * WIDE / height;
            for (int x = 0; x < width; x++) {
                final double along = -REACH + (x + 0.5) * 2 * REACH / width;
                final double dither = BAYER[y % 4][x % 4] / 16.0 - 0.5;
                out[y * width + x] = Math.floor(Math.max(0, Math.min(LEVELS, light(along, beta) * LEVELS + dither)))
                        / LEVELS;
            }
        }
        return out;
    }

    /**
     * How bright the zodiacal light is {@code along} degrees along the sun's path from the sun and {@code beta}
     * degrees from the path, 0 to 1: brightest at the sun, gone past {@link #REACH}.
     */
    public static double light(final double along, final double beta) {
        final double elongation = Math.abs(along);
        final double wide = WIDTH + elongation * WIDENING;
        return Math.exp(-elongation / FADING) * Math.exp(-(beta / wide) * (beta / wide))
                * Math.max(0, Math.min(1, (REACH - elongation) / ENDING));
    }
}
