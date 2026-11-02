/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import java.util.SplittableRandom;

/**
 * The picture of a galaxy's band across the night, as one like ours shows from inside it: a map of the sky round the
 * band in galactic longitude, from -180 to 180 degrees across, and latitude, from {@link #REACH} north to south down.
 * The band thickens and warms towards its heart at longitude 0, a bulge of star clouds; clouds of stars mottle it at
 * two scales; a dark rift of dust winds along its middle through the inner half, as the Great Rift does from Cygnus
 * to Sagittarius; a few small pink glows of nebulae lie in it. Its light comes in a dozen levels, dithered so the
 * levels never draw contour lines, as the game's pixels would. Pure: no game types.
 */
public final class GalaxyPicture {

    /** How far north and south of its middle line the picture reaches, in degrees. */
    public static final double REACH = 24.0;
    /** The colour of its nebulae's glow. */
    public static final int NEBULA = 0xD94D6B;
    private static final int LEVELS = 12;
    private static final int NEBULA_LEVELS = 5;
    private static final double[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    /* The band's half width at its rim and the more at its heart; how far the heart's widening and warmth reach. */
    private static final double RIM_WIDTH = 4.5;
    private static final double HEART_WIDTH = 9.5;
    private static final double HEART_REACH = 42.0;
    private static final double WARM_REACH = 48.0;
    /* Its light along its length: a floor, the heart's bulge and a second swell where its arms pass, either side. */
    private static final double FLOOR = 0.30;
    private static final double BULGE = 0.70;
    private static final double BULGE_REACH = 32.0;
    private static final double ARMS = 0.28;
    private static final double ARMS_AT = 78.0;
    private static final double ARMS_REACH = 22.0;
    /* The star clouds, at two scales: the noise's cells round the sky and how much each mottles the band. */
    private static final int BIG_CELLS = 48;
    private static final int FINE_CELLS = 160;
    private static final double CLOUD_FLOOR = 0.12;
    private static final double BIG_CLOUDS = 1.30;
    private static final double FINE_CLOUDS = 0.50;
    /* The rift: how far north of the middle it runs, how much it wanders, how wide, how far along and how dark. */
    private static final double RIFT_AT = 1.2;
    private static final double RIFT_WANDER = 3.0;
    private static final double RIFT_WIDTH = 1.3;
    private static final double RIFT_MORE_WIDTH = 0.8;
    private static final double RIFT_ALONG = 95.0;
    private static final double RIFT_DARK = 0.92;
    /* The nebulae: longitude, latitude and radius, in degrees; and how bright, beside the band. */
    private static final double[][] NEBULAE = {{-18, -1.0, 2.6}, {10, 0.5, 1.8}, {60, 2.0, 2.2}, {-82, -1.5, 1.6}};
    private static final double NEBULA_SHARE = 0.55;
    private static final int CHANNEL = 0xFF;

    private GalaxyPicture() {
    }

    /**
     * The picture, {@code width} texels across and {@code height} down, each {@code 0xRRGGBB}: black where the sky is
     * empty, the band's light from {@code rim}'s colour at its ends to {@code heart}'s at its heart, its nebulae
     * glowing pink, at full strength; the same for the same {@code seed}.
     */
    public static int[] draw(final long seed, final int width, final int height, final int rim, final int heart) {
        final double[] big = grid(seed, BIG_CELLS, BIG_CELLS / 2);
        final double[] fine = grid(seed + 1, FINE_CELLS, FINE_CELLS / 2);
        final double[] cool = rgb(rim);
        final double[] warm = rgb(heart);
        final double[] pink = rgb(NEBULA);
        final int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            final double b = REACH - (y + 0.5) * 2 * REACH / height;
            for (int x = 0; x < width; x++) {
                final double l = -180 + (x + 0.5) * 360 / width;
                final double u = (l + 180) / 360;
                final double v = (b + 90) / 180;
                final double coarse = noise(big, BIG_CELLS, BIG_CELLS / 2, u, v);
                final double grain = noise(fine, FINE_CELLS, FINE_CELLS / 2, u, v);
                final double dither = BAYER[y % 4][x % 4] / 16.0 - 0.5;
                final double light = levels(light(l, b, coarse, grain), LEVELS, dither);
                final double mix = Math.exp(-sq(l / WARM_REACH));
                final double[] colour = new double[3];
                for (int c = 0; c < 3; c++) {
                    colour[c] = light * (warm[c] * mix + cool[c] * (1 - mix));
                }
                for (final double[] nebula : NEBULAE) {
                    final double d2 = sq((l - nebula[0]) / nebula[2]) + sq((b - nebula[1]) / (nebula[2] * 0.8));
                    final double glow = levels(Math.exp(-d2) * (NEBULA_SHARE + (1 - NEBULA_SHARE) * grain),
                            NEBULA_LEVELS, 0);
                    for (int c = 0; c < 3; c++) {
                        colour[c] += glow * pink[c] * NEBULA_SHARE;
                    }
                }
                out[y * width + x] = pack(colour);
            }
        }
        return out;
    }

    /**
     * How bright the band is at galactic longitude {@code l} and latitude {@code b}, in degrees, 0 to about 2, given
     * the star clouds' noise there at its two scales, each 0 to 1.
     */
    public static double light(final double l, final double b, final double coarse, final double grain) {
        final double width = RIM_WIDTH + HEART_WIDTH * Math.exp(-sq(l / HEART_REACH));
        final double band = Math.exp(-sq(b / width));
        final double along = FLOOR + BULGE * Math.exp(-sq(l / BULGE_REACH))
                + ARMS * Math.exp(-sq((Math.abs(l) - ARMS_AT) / ARMS_REACH));
        final double clouds = CLOUD_FLOOR + BIG_CLOUDS * coarse * coarse + FINE_CLOUDS * Math.pow(grain, 1.5);
        final double rift = Math.abs(l) < RIFT_ALONG
                ? Math.exp(-sq((b - RIFT_AT - (coarse - 0.5) * RIFT_WANDER) / (RIFT_WIDTH + RIFT_MORE_WIDTH * grain)))
                * (0.5 + 0.5 * coarse) : 0;
        return band * along * clouds * (1 - RIFT_DARK * rift);
    }

    /* {@code value} brought to {@code levels} steps, nudged by {@code dither} first, and back to 0 to 1. */
    private static double levels(final double value, final int levels, final double dither) {
        return Math.floor(Math.max(0, Math.min(levels, value * levels + dither))) / levels;
    }

    /* A grid of noise round the sky: {@code across} cells round, {@code down + 1} rows from pole to pole. */
    private static double[] grid(final long seed, final int across, final int down) {
        final SplittableRandom random = new SplittableRandom(seed);
        final double[] grid = new double[(down + 1) * across];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = random.nextDouble();
        }
        return grid;
    }

    /* Value noise from {@code grid} at {@code u} round the sky, wrapping, and {@code v} from pole to pole. */
    private static double noise(final double[] grid, final int across, final int down, final double u,
                                final double v) {
        final double x = (u - Math.floor(u)) * across;
        final double y = Math.max(0, Math.min(0.9999, v)) * down;
        final int x0 = (int) Math.floor(x);
        final int y0 = (int) Math.floor(y);
        final double fx = smooth(x - x0);
        final double fy = smooth(y - y0);
        final int x1 = (x0 + 1) % across;
        final int y1 = Math.min(y0 + 1, down);
        final double top = grid[y0 * across + x0 % across] * (1 - fx) + grid[y0 * across + x1] * fx;
        final double bottom = grid[y1 * across + x0 % across] * (1 - fx) + grid[y1 * across + x1] * fx;
        return top * (1 - fy) + bottom * fy;
    }

    private static double smooth(final double f) {
        return f * f * (3 - 2 * f);
    }

    private static double sq(final double x) {
        return x * x;
    }

    private static double[] rgb(final int colour) {
        return new double[] {(colour >> 16 & CHANNEL) / 255.0, (colour >> 8 & CHANNEL) / 255.0,
                (colour & CHANNEL) / 255.0};
    }

    private static int pack(final double[] colour) {
        int out = 0;
        for (final double c : colour) {
            out = out << 8 | (int) Math.round(Math.max(0, Math.min(1, c)) * CHANNEL);
        }
        return out;
    }
}
