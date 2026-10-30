/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import java.util.SplittableRandom;

/*
 * A strike as its stamp is laid: its point, the way out of the surface and two ways across it, and the rough rim and
 * petals its seed gives; the same for the faces it moves and for the pixels of its edge. Pure.
 */
final class StrikeFrame {

    final Strike strike;
    final double px;
    final double py;
    final double pz;
    final double[] out;
    final double[] across;
    final double[] up;
    final double petals;
    private final double[] amplitude = new double[WAVES];
    private final double[] phase = new double[WAVES];

    /* The rim's roughness is the sum of these waves round it, from three to eight to a turn. */
    private static final int FIRST_WAVE = 3;
    private static final int WAVES = 6;
    /* Past this much of the way up, the way across is laid along X rather than up. */
    private static final double MOSTLY_UP = 0.9;

    StrikeFrame(final Strike strike) {
        this.strike = strike;
        this.px = strike.x();
        this.py = strike.y();
        this.pz = strike.z();
        this.out = new double[] {strike.nx(), strike.ny(), strike.nz()};
        final double[] helper = Math.abs(strike.ny()) < MOSTLY_UP ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
        this.across = unit(cross(helper, this.out));
        this.up = cross(this.out, this.across);
        final SplittableRandom random = new SplittableRandom(strike.seed());
        for (int k = 0; k < WAVES; k++) {
            this.amplitude[k] = random.nextDouble(-1, 1);
            this.phase[k] = random.nextDouble(0, 2 * Math.PI);
        }
        this.petals = random.nextDouble(0, 2 * Math.PI);
    }

    /* How far out of the surface the point (x, y, z) lies, along the way it faced. */
    double along(final double x, final double y, final double z) {
        return (x - this.px) * this.out[0] + (y - this.py) * this.out[1] + (z - this.pz) * this.out[2];
    }

    /* How far across the surface the point lies, the first way. */
    double a(final double x, final double y, final double z) {
        return (x - this.px) * this.across[0] + (y - this.py) * this.across[1] + (z - this.pz) * this.across[2];
    }

    /* How far across the surface the point lies, the second way. */
    double b(final double x, final double y, final double z) {
        return (x - this.px) * this.up[0] + (y - this.py) * this.up[1] + (z - this.pz) * this.up[2];
    }

    /* How far out the rim of its hole is at {@code angle} round its point, in blocks; 0 for a stamp that cuts none. */
    double rim(final double angle) {
        final DamageStamp stamp = this.strike.stamp();
        if (!stamp.cuts()) {
            return 0;
        }
        double wave = 0;
        for (int k = 0; k < WAVES; k++) {
            final int times = FIRST_WAVE + k;
            wave += this.amplitude[k] * Math.sin(times * angle + this.phase[k]) / times;
        }
        return this.strike.size() * stamp.hole() * (1 + stamp.rough() * wave);
    }

    private static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(final double[] v) {
        final double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }
}
