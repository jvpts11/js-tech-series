/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * The Overworld's sphere as seen from space, drawn from the world's seed: random continents whose coasts wander, polar
 * caps always, Minecraft's biomes laid out by warmth and wetness, mountain ranges in a few places, painted in a few
 * tones a class as a pixel artist lays them, with the relief its light is shaded by; and its clouds, the game's own
 * cloud map wrapped round it in bands, each
 * drifting at its own pace, gathered by the weather into belts and fronts. The Overworld is the series' home, never the
 * real Earth: its map looks like nothing on the Earth.
 *
 * <p>Maps are equirectangular, {@code width} texels round from 180° west and {@code width / 2} from pole to pole.
 *
 * <p>Pure: no game types.
 */
public final class OverworldMap {

    /** The tones of each class, as RGB: the seas, the beaches, the biomes, the mountains and the ice. */
    private static final int[][] TONES = {
        {0x203D8A, 0x244494, 0x28499C}, {0x2C55AE, 0x305CB8, 0x3563C0}, {0x2A80C4, 0x2F8ACE, 0x3594D6},
        {0xD4CB9A, 0xDED6A8, 0xE8E0B6}, {0x6A9A40, 0x74A548, 0x7EAE50}, {0x3C6A28, 0x44762E, 0x4C8234},
        {0x2B4C1C, 0x325722, 0x386028}, {0x3A5D3B, 0x426842, 0x4A7349}, {0xDDE6E8, 0xE9EFF0, 0xF4F8F8, 0x5A7A5E},
        {0xD2C28A, 0xDCCD98, 0xE5D7A6}, {0x9E9B4C, 0xAAA656, 0xB5B160}, {0xA8552C, 0xB86636, 0xC87642, 0xD6955E},
        {0x2C7A1A, 0x338720, 0x3A9426}, {0x6E6E6E, 0x7A7A7A, 0x878787}, {0xE6ECEE, 0xF2F6F7},
        {0xD8E4F2, 0xE4EDF7, 0xEEF4FA, 0xC4D4EA}, {0x8AA6DC, 0xA2BAEA, 0xBCCFF2}, {0x4C5A30, 0x566638},
    };
    private static final int DEEP = 0;
    private static final int OCEAN = 1;
    private static final int WARM_SHALLOWS = 2;
    private static final int BEACH = 3;
    private static final int PLAINS = 4;
    private static final int FOREST = 5;
    private static final int DARK_FOREST = 6;
    private static final int TAIGA = 7;
    private static final int SNOWY = 8;
    private static final int DESERT = 9;
    private static final int SAVANNA = 10;
    private static final int BADLANDS = 11;
    private static final int JUNGLE = 12;
    private static final int STONE_PEAKS = 13;
    private static final int SNOW_PEAKS = 14;
    private static final int POLAR_CAP = 15;
    private static final int SEA_ICE = 16;
    private static final int SWAMP = 17;
    /* The 4 by 4 ordered dither that softens the seam between two tones. */
    private static final double[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    /* The share of the sphere under the sea. */
    private static final double SEA_SHARE = 0.60;
    /* The game's cloud map is 256 texels square, and the bands of cloud drifting past each other 32 texels tall. */
    private static final int CLOUD_TILE = 256;
    private static final int CLOUD_BAND = 32;

    private OverworldMap() {
    }

    /**
     * The Overworld's maps for {@code seed}.
     *
     * @param width how many texels round, even
     */
    public static Maps maps(final int width, final long seed) {
        final int height = width / 2;
        final long s = seed * 101;
        final double[] warpLon = fbm(width, height, 6, s + 1, 4, 1.0, 0.5);
        final double[] warpLat = fbm(width, height, 6, s + 8, 4, 1.0, 0.5);
        final double[] big = fbm(width, height, 7, s + 2, 7, 1.0, 0.55);
        final double[] field = new double[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int i = y * width + x;
                final double lon = lon(x, width) + (warpLon[i] - 0.5) * 2 * 14;
                final double lat = lat(y, height) + (warpLat[i] - 0.5) * 2 * 14 * 0.7;
                final int sx = Math.floorMod((int) (((lon + 180) % 360 + 360) % 360 / 360 * width), width);
                final int sy = Math.max(0, Math.min(height - 1, (int) ((90 - lat) / 180 * height)));
                field[i] = big[sy * width + sx];
            }
        }
        final double sea = quantile(field, SEA_SHARE);
        final double[] warmthNoise = fbm(width, height, 10, s + 3, 4, 1.0, 0.5);
        final double[] wet = fbm(width, height, 9, s + 4, 5, 1.0, 0.5);
        final double[] ridgeNoise = fbm(width, height, 14, s + 5, 5, 1.0, 0.5);
        final double[] ranges = fbm(width, height, 5, s + 10, 4, 1.0, 0.5);
        final double[] capNoise = fbm(width, height, 24, s + 6, 4, 1.0, 0.5);
        final double[] tone = fbm(width, height, 96, s + 7, 4, 1.0, 0.5);
        final double[] rough = fbm(width, height, 120, s + 8, 3, 1.0, 0.5);
        final double[] capRough = fbm(width, height, 60, s + 9, 3, 1.0, 0.5);
        final boolean[] land = new boolean[width * height];
        for (int i = 0; i < land.length; i++) {
            land[i] = field[i] - sea > 0;
        }
        final boolean[] nearLand = dilate(land, width, height, 3);
        final boolean[] nearSea = dilate(not(land), width, height, 1);
        final int[] out = new int[width * height];
        final double[] heights = new double[width * height];
        for (int y = 0; y < height; y++) {
            final double lat = lat(y, height);
            for (int x = 0; x < width; x++) {
                final int i = y * width + x;
                final double elev = field[i] - sea;
                final double warmth = Math.cos(Math.toRadians(lat)) * 1.15 - 0.15 - Math.max(0, elev) * 0.7
                        + (warmthNoise[i] - 0.5) * 0.45;
                final double ridge = 1 - Math.abs(ridgeNoise[i] * 2 - 1);
                final boolean mountain = land[i] && ranges[i] > 0.60 && ridge > 0.84 && elev > 0.02;
                int cls = elev < -0.07 ? DEEP : OCEAN;
                if (nearLand[i] && !land[i] && warmth > 0.80) {
                    cls = WARM_SHALLOWS;
                }
                if (land[i]) {
                    cls = biome(warmth, wet[i], ridge, elev);
                    if (nearSea[i] && elev < 0.02 && warmth > 0.3 && cls != SNOWY && cls != TAIGA) {
                        cls = BEACH;
                    }
                    if (mountain) {
                        cls = ridge > 0.95 || warmth < 0.40 ? SNOW_PEAKS : STONE_PEAKS;
                    }
                }
                // The land rises with its height and its ranges, a little rough; the sea is flat; the caps are a low
                // rough sheet over land and sea alike.
                if (land[i]) {
                    heights[i] = Math.max(0, elev) * 1.5 + (mountain ? (ridge - 0.8) * 2.0 : 0)
                            + (rough[i] - 0.5) * 0.05;
                }
                final double cap = 71 + (capNoise[i] - 0.5) * 10;
                if (!land[i] && Math.abs(lat) > cap - 5) {
                    cls = SEA_ICE;
                }
                if (Math.abs(lat) > cap) {
                    cls = POLAR_CAP;
                    heights[i] = 0.08 + (capRough[i] - 0.5) * 0.04;
                }
                out[i] = paint(cls, tone[i], x, y);
            }
        }
        return new Maps(out, heights);
    }

    /**
     * The weather the clouds are gathered by, for {@code seed}: worked out once, then slid east as the clouds turn.
     */
    public static double[] weather(final int width, final long seed) {
        return fbm(width, width / 2, 6, seed * 7 + 5, 4, 1.0, 0.5);
    }

    /**
     * The Overworld's cloud cover at {@code turn} turns of the clouds round it, a texel at a time from the north pole:
     * {@code tile} is the game's 256 by 256 cloud map, true where it has cloud, and {@code weather} the seed's
     * ({@link #weather}).
     */
    public static boolean[] clouds(final int width, final double turn, final long seed, final boolean[] tile,
                                   final double[] weather) {
        final int height = width / 2;
        final SplittableRandom random = new SplittableRandom(seed * 7 + 3);
        final int scale = Math.max(1, width / (2 * CLOUD_TILE));
        final boolean[] out = new boolean[width * height];
        for (int top = 0; top < height; top += CLOUD_BAND) {
            final int startY = random.nextInt(CLOUD_TILE);
            final int startX = random.nextInt(CLOUD_TILE * scale);
            final double speed = 1.0 + (random.nextDouble() - 0.5) * 0.5;
            final boolean flip = random.nextBoolean();
            final int shift = (int) (startX + turn * width * speed);
            for (int y = top; y < Math.min(height, top + CLOUD_BAND); y++) {
                final int ty = Math.floorMod(y / scale + startY, CLOUD_TILE);
                for (int x = 0; x < width; x++) {
                    int tx = Math.floorMod(Math.floorDiv(x + shift, scale), CLOUD_TILE);
                    if (flip) {
                        tx = CLOUD_TILE - 1 - tx;
                    }
                    out[y * width + x] = tile[ty * CLOUD_TILE + tx];
                }
            }
        }
        // The weather gathers the clouds into belts at the equator and the middle latitudes, and fronts drifting east.
        final int drift = (int) (turn * width * 1.1);
        final boolean[] gathered = new boolean[out.length];
        for (int y = 0; y < height; y++) {
            final double lat = lat(y, height);
            final double alat = Math.abs(lat);
            final double belts = 0.55 * Math.exp(-Math.pow((lat - 4) / 9, 2))
                    + 0.55 * Math.exp(-Math.pow((alat - 52) / 14, 2))
                    - 0.35 * Math.exp(-Math.pow((alat - 25) / 8, 2)) - (alat > 76 ? 0.25 : 0);
            for (int x = 0; x < width; x++) {
                final double front = weather[y * width + Math.floorMod(x - drift, width)];
                gathered[y * width + x] = out[y * width + x] && front + belts > 0.58;
            }
        }
        // Lone specks dropped and neighbours joined, so from afar the clouds read as clumps.
        final boolean[] clumped = new boolean[out.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        final int yy = Math.floorMod(y + dy, height);
                        if (gathered[yy * width + Math.floorMod(x + dx, width)]) {
                            count++;
                        }
                    }
                }
                clumped[y * width + x] = count >= 4 || gathered[y * width + x] && count >= 3;
            }
        }
        return clumped;
    }

    /* The biome of land by its warmth, wetness, ridge and height, in the order the mock lays them. */
    private static int biome(final double warmth, final double wet, final double ridge, final double elev) {
        int cls = PLAINS;
        if (wet > 0.52) {
            cls = FOREST;
        }
        if (wet > 0.62) {
            cls = DARK_FOREST;
        }
        if (wet < 0.40 && warmth > 0.62) {
            cls = SAVANNA;
        }
        if (wet < 0.36 && warmth > 0.72) {
            cls = DESERT;
        }
        if (wet < 0.30 && warmth > 0.66 && ridge > 0.62) {
            cls = BADLANDS;
        }
        if (wet > 0.55 && warmth > 0.80) {
            cls = JUNGLE;
        }
        if (wet > 0.58 && warmth > 0.55 && warmth < 0.72 && elev < 0.012) {
            cls = SWAMP;
        }
        if (warmth < 0.34) {
            cls = TAIGA;
        }
        if (warmth < 0.20) {
            cls = SNOWY;
        }
        return cls;
    }

    /* A class's tone at a texel: the noise picks it in clumps, and only a narrow seam is dithered. */
    private static int paint(final int cls, final double noise, final int x, final int y) {
        final int[] tones = TONES[cls];
        final double level = noise * (tones.length - 1);
        int index = (int) Math.floor(level);
        final double threshold = BAYER[(y & 3) * 4 + (x & 3)] / 16.0;
        if (level - index > 0.5 + (threshold - 0.5) * 0.3) {
            index++;
        }
        return tones[Math.max(0, Math.min(tones.length - 1, index))];
    }

    /* Value noise that wraps round in longitude, {@code cells} across, smoothed between its lattice points. */
    private static double[] noise(final int width, final int height, final int cells, final long seed,
                                  final double stretch) {
        final SplittableRandom random = new SplittableRandom(seed);
        final int rows = Math.max(2, (int) (cells / 2 * stretch)) + 2;
        final double[] lattice = new double[rows * cells];
        for (int i = 0; i < lattice.length; i++) {
            lattice[i] = random.nextDouble();
        }
        final double[] out = new double[width * height];
        for (int y = 0; y < height; y++) {
            final double gy = (double) y / height * (rows - 2);
            final int y0 = (int) Math.floor(gy);
            final double ty = smooth(gy - y0);
            for (int x = 0; x < width; x++) {
                final double gx = (double) x / width * cells;
                final int x0 = (int) Math.floor(gx);
                final double tx = smooth(gx - x0);
                final int xa = Math.floorMod(x0, cells);
                final int xb = Math.floorMod(x0 + 1, cells);
                final double a = lattice[y0 * cells + xa] + (lattice[y0 * cells + xb] - lattice[y0 * cells + xa]) * tx;
                final double b = lattice[(y0 + 1) * cells + xa]
                        + (lattice[(y0 + 1) * cells + xb] - lattice[(y0 + 1) * cells + xa]) * tx;
                out[y * width + x] = a + (b - a) * ty;
            }
        }
        return out;
    }

    /* Octaves of value noise, each twice as fine and {@code gain} as strong as the last. */
    private static double[] fbm(final int width, final int height, final int cells, final long seed,
                                final int octaves, final double stretch, final double gain) {
        final double[] total = new double[width * height];
        double amplitude = 1.0;
        double norm = 0.0;
        for (int octave = 0; octave < octaves; octave++) {
            final double[] layer = noise(width, height, cells << octave, seed + 17L * octave, stretch);
            for (int i = 0; i < total.length; i++) {
                total[i] += amplitude * layer[i];
            }
            norm += amplitude;
            amplitude *= gain;
        }
        for (int i = 0; i < total.length; i++) {
            total[i] /= norm;
        }
        return total;
    }

    private static boolean[] dilate(final boolean[] mask, final int width, final int height, final int radius) {
        final boolean[] out = mask.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y * width + x]) {
                    continue;
                }
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (dx * dx + dy * dy <= radius * radius) {
                            out[Math.floorMod(y + dy, height) * width + Math.floorMod(x + dx, width)] = true;
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean[] not(final boolean[] mask) {
        final boolean[] out = new boolean[mask.length];
        for (int i = 0; i < mask.length; i++) {
            out[i] = !mask[i];
        }
        return out;
    }

    /* The value below which {@code share} of {@code values} lie. */
    private static double quantile(final double[] values, final double share) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[Math.min(sorted.length - 1, (int) (share * (sorted.length - 1)))];
    }

    private static double smooth(final double t) {
        return t * t * (3 - 2 * t);
    }

    /* The longitude at the middle of texel column {@code x}, and the latitude of row {@code y}, in degrees. */
    private static double lon(final int x, final int width) {
        return (x + 0.5) / width * 360 - 180;
    }

    private static double lat(final int y, final int height) {
        return 90 - (y + 0.5) / height * 180;
    }

    /**
     * The Overworld's maps, a texel at a time, row by row from the north pole.
     *
     * @param colours its colours, as RGB
     * @param heights its relief: 0 at the sea, rising to about 1 on the highest peaks
     */
    public record Maps(int[] colours, double[] heights) {
    }
}
