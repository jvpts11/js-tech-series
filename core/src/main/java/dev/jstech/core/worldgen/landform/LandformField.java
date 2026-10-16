/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Where the landforms of a kind lie in a world: the world cut into square cells, each holding at most one landform at a
 * place, size and turn worked out from the world's seed and the kind's salt alone. Any chunk finds every landform that
 * reaches it without asking another chunk, so a landform of any size is drawn a chunk at a time, on any thread, the
 * same for the same seed. Pure.
 *
 * @param cell     a cell's side, in blocks: no two landforms of the kind lie closer than about half of it
 * @param chance   how many of the cells hold one, 0 to 1
 * @param smallest the least radius, in blocks
 * @param largest  the most radius, in blocks
 * @param salt     what keeps this kind's places apart from another's on the same seed
 */
public record LandformField(int cell, double chance, int smallest, int largest, long salt) {

    /* Two odd numbers of no pattern, the golden ratio's and a hash's, that spread neighbouring cells far apart. */
    private static final long MIX_X = -7_046_029_254_386_353_131L;
    private static final long MIX_Z = -4_417_276_706_812_531_889L;

    public LandformField {
        if (cell < 16) {
            throw new IllegalArgumentException("a landform's cell is at least a chunk across, not " + cell);
        }
        if (smallest < 1 || largest < smallest) {
            throw new IllegalArgumentException("a landform's size runs from 1 up, not " + smallest + " to " + largest);
        }
    }

    /** Every landform of the kind that reaches the square from {@code x0, z0} to {@code x1, z1}, for {@code seed}. */
    public List<Instance> reaching(final long seed, final int x0, final int z0, final int x1, final int z1) {
        return reaching(seed, x0, z0, x1, z1, 1.0);
    }

    /**
     * Every landform of the kind whose reach, {@code factor} times its radius, touches the square from {@code x0, z0}
     * to {@code x1, z1}: a crater's rays reach past its rim.
     */
    public List<Instance> reaching(final long seed, final int x0, final int z0, final int x1, final int z1,
                                   final double factor) {
        final List<Instance> out = new ArrayList<>();
        final int reach = (int) Math.ceil(this.largest * factor);
        final int cx0 = Math.floorDiv(x0 - reach, this.cell);
        final int cz0 = Math.floorDiv(z0 - reach, this.cell);
        final int cx1 = Math.floorDiv(x1 + reach, this.cell);
        final int cz1 = Math.floorDiv(z1 + reach, this.cell);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                final Instance one = in(seed, cx, cz);
                final double own = one == null ? 0 : one.radius() * factor;
                if (one != null && one.x() + own >= x0 && one.x() - own <= x1 && one.z() + own >= z0
                        && one.z() - own <= z1) {
                    out.add(one);
                }
            }
        }
        return out;
    }

    /** The landform of the cell {@code cx, cz}, or none if it holds none. */
    public Instance in(final long seed, final int cx, final int cz) {
        final SplittableRandom random = new SplittableRandom(seed ^ this.salt ^ cx * MIX_X ^ cz * MIX_Z);
        if (random.nextDouble() >= this.chance) {
            return null;
        }
        final int margin = Math.min(this.cell / 4, this.largest / 2);
        final int x = cx * this.cell + margin + random.nextInt(Math.max(1, this.cell - 2 * margin));
        final int z = cz * this.cell + margin + random.nextInt(Math.max(1, this.cell - 2 * margin));
        final int radius = this.smallest + random.nextInt(this.largest - this.smallest + 1);
        return new Instance(x, z, radius, random.nextDouble() * 2 * Math.PI, random.nextLong());
    }

    /**
     * A landform where it lies.
     *
     * @param x      its middle across
     * @param z      and along
     * @param radius how far it reaches, in blocks
     * @param turn   which way it lies, in radians: a canyon's run, the dunes' ridges
     * @param seed   what picks its own scattered details
     */
    public record Instance(int x, int z, int radius, double turn, long seed) {

        /** How far {@code px, pz} is from its middle. */
        public double distance(final double px, final double pz) {
            return Math.hypot(px - this.x, pz - this.z);
        }

        /** How far {@code px, pz} lies along its turn from its middle, and across it. */
        public double along(final double px, final double pz) {
            return (px - this.x) * Math.cos(this.turn) + (pz - this.z) * Math.sin(this.turn);
        }

        /** How far {@code px, pz} lies across its turn from its middle line. */
        public double across(final double px, final double pz) {
            return -(px - this.x) * Math.sin(this.turn) + (pz - this.z) * Math.cos(this.turn);
        }
    }
}
