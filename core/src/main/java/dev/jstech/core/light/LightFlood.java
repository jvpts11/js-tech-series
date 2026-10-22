/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

/**
 * The light of one source spread through the places around it as the game spreads its own: a level less each place it
 * goes into, more through a block that dims it, none through a wall, round corners by the way it can go; and its
 * colour taken from every block it passes that tints it, as stained glass does, so light through a red pane is red on
 * the far side and white on the near. Each place holds the level it is lit to and the colour it is lit in, by the
 * brightest way there. Pure; colours are {@code 0xRRGGBB}.
 */
public final class LightFlood {

    private final int x;
    private final int y;
    private final int z;
    private final int level;
    private final int colour;
    private final int reach;
    private final int side;
    private final byte[] levels;
    @Nullable
    private final int[] colours;
    private final boolean tinted;

    /** The most a block dims light: a wall's, which none goes through. */
    public static final int OPAQUE = LightMath.BRIGHTEST;

    private static final int[][] STEPS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final ThreadLocal<int[]> SCRATCH = ThreadLocal.withInitial(() -> new int[0]);

    private LightFlood(final int x, final int y, final int z, final int level, final int colour, final int reach,
                       final byte[] levels, @Nullable final int[] colours, final boolean tinted) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.level = level;
        this.colour = colour;
        this.reach = reach;
        this.side = 2 * reach + 1;
        this.levels = levels;
        this.colours = colours;
        this.tinted = tinted;
    }

    /** The light of a source of {@code level} and {@code colour} at {@code x, y, z}, spread through {@code passage}. */
    public static LightFlood spread(final int x, final int y, final int z, final int level, final int colour,
                                    final IPassage passage) {
        final int strength = Math.clamp(level, 0, LightMath.BRIGHTEST);
        final int reach = Math.max(0, strength - 1);
        final int side = 2 * reach + 1;
        final byte[] levels = new byte[side * side * side];
        final int[] colours = scratch(levels.length);
        // A bucket of places for each level, the brightest worked out first, so each place keeps its brightest way.
        final int[][] buckets = new int[strength + 1][];
        final int[] counts = new int[strength + 1];
        final int origin = cell(reach, reach, reach, side);
        boolean tinted = false;
        if (strength > 0) {
            levels[origin] = (byte) strength;
            colours[origin] = colour;
            push(buckets, counts, strength, origin);
        }
        for (int lit = strength; lit > 1; lit--) {
            for (int i = 0; i < counts[lit]; i++) {
                final int at = buckets[lit][i];
                if (levels[at] != lit) {
                    continue;
                }
                final int cx = at / (side * side);
                final int cy = at / side % side;
                final int cz = at % side;
                for (final int[] step : STEPS) {
                    final int nx = cx + step[0];
                    final int ny = cy + step[1];
                    final int nz = cz + step[2];
                    if (nx < 0 || ny < 0 || nz < 0 || nx >= side || ny >= side || nz >= side) {
                        continue;
                    }
                    final int wx = x + nx - reach;
                    final int wy = y + ny - reach;
                    final int wz = z + nz - reach;
                    final int dims = passage.dims(wx, wy, wz);
                    if (dims >= OPAQUE) {
                        continue;
                    }
                    final int next = lit - Math.max(1, dims);
                    final int there = cell(nx, ny, nz, side);
                    if (next <= levels[there]) {
                        continue;
                    }
                    final int tint = passage.tint(wx, wy, wz);
                    levels[there] = (byte) next;
                    colours[there] = tint == LightMath.WHITE ? colours[at] : LightMath.multiply(colours[at], tint);
                    tinted |= colours[there] != colour;
                    push(buckets, counts, next, there);
                }
            }
        }
        // Light that kept its colour all the way keeps no colours of its own: each place is lit in the source's.
        return new LightFlood(x, y, z, strength, colour, reach, levels,
                tinted ? Arrays.copyOf(colours, levels.length) : null, tinted);
    }

    /** The level the place {@code x, y, z} is lit to, 0 where the light does not reach. */
    public int level(final int px, final int py, final int pz) {
        final int at = index(px, py, pz);
        return at < 0 ? 0 : this.levels[at];
    }

    /** The colour the place {@code x, y, z} is lit in; the source's own where the light does not reach. */
    public int colour(final int px, final int py, final int pz) {
        final int at = index(px, py, pz);
        return at < 0 || this.colours == null || this.levels[at] == 0 ? this.colour : this.colours[at];
    }

    /** Whether any place is lit in another colour than the source's, the light having passed a block that tints. */
    public boolean tinted() {
        return this.tinted;
    }

    /** How far the light can go from its source along each axis. */
    public int reach() {
        return this.reach;
    }

    /** The level of the source. */
    public int level() {
        return this.level;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LightFlood that && this.x == that.x && this.y == that.y && this.z == that.z
                && this.level == that.level && this.colour == that.colour && Arrays.equals(this.levels, that.levels)
                && Arrays.equals(this.colours, that.colours);
    }

    @Override
    public int hashCode() {
        return ((this.x * 31 + this.y) * 31 + this.z) * 31 + Arrays.hashCode(this.levels);
    }

    private int index(final int px, final int py, final int pz) {
        final int dx = px - this.x + this.reach;
        final int dy = py - this.y + this.reach;
        final int dz = pz - this.z + this.reach;
        if (dx < 0 || dy < 0 || dz < 0 || dx >= this.side || dy >= this.side || dz >= this.side) {
            return -1;
        }
        return cell(dx, dy, dz, this.side);
    }

    /* The colours a spread works in, kept for the next one on its thread: a spread reads only the places it lit. */
    private static int[] scratch(final int size) {
        int[] colours = SCRATCH.get();
        if (colours.length < size) {
            colours = new int[size];
            SCRATCH.set(colours);
        }
        return colours;
    }

    private static int cell(final int dx, final int dy, final int dz, final int side) {
        return (dx * side + dy) * side + dz;
    }

    private static void push(final int[][] buckets, final int[] counts, final int level, final int cell) {
        if (buckets[level] == null) {
            buckets[level] = new int[64];
        } else if (counts[level] == buckets[level].length) {
            buckets[level] = Arrays.copyOf(buckets[level], counts[level] * 2);
        }
        buckets[level][counts[level]++] = cell;
    }

    /** What a block does to light going into it. */
    public interface IPassage {

        /** How much the block at a place dims light going into it: 0 for air or glass, {@link #OPAQUE} for a wall. */
        int dims(int x, int y, int z);

        /** The colour the block at a place gives light passing through it; white for none. */
        int tint(int x, int y, int z);
    }
}
