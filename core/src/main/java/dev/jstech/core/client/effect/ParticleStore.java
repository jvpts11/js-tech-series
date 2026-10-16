/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import java.util.Arrays;

/**
 * Every live particle of the Core's effects, a column for each of what a particle is, so thousands live in a few
 * arrays rather than thousands of objects: where it is and was a tick ago, how fast it goes, how old it is and how long
 * it lives, how it is turned and spins, how big it is beside its kind, its colour laid over, the emitter it is of, and
 * the ribbon it leaves. A particle that dies takes the last one's place.
 */
final class ParticleStore {

    int count;
    double[] x = new double[0];
    double[] y = new double[0];
    double[] z = new double[0];
    double[] lastX = new double[0];
    double[] lastY = new double[0];
    double[] lastZ = new double[0];
    float[] vx = new float[0];
    float[] vy = new float[0];
    float[] vz = new float[0];
    float[] age = new float[0];
    float[] life = new float[0];
    float[] angle = new float[0];
    float[] spin = new float[0];
    float[] size = new float[0];
    int[] colour = new int[0];
    EmitterRun[] owner = new EmitterRun[0];
    float[][] trail = new float[0][];
    int[] trailHead = new int[0];
    int[] trailCount = new int[0];

    /** How many places a ribbon keeps. */
    static final int TRAIL = 12;
    private static final int FIRST = 256;

    /** Adds a particle of {@code run} at {@code px, py, pz} going {@code v}; its index, or -1 if there is no room. */
    int add(final EmitterRun run, final double px, final double py, final double pz, final double[] v,
            final float lives, final float turned, final float spins, final float sized, final int laid,
            final int most) {
        if (this.count >= most) {
            return -1;
        }
        if (this.count == this.x.length) {
            grow(Math.min(most, Math.max(FIRST, this.count * 2)));
        }
        final int i = this.count++;
        this.x[i] = px;
        this.y[i] = py;
        this.z[i] = pz;
        this.lastX[i] = px;
        this.lastY[i] = py;
        this.lastZ[i] = pz;
        this.vx[i] = (float) v[0];
        this.vy[i] = (float) v[1];
        this.vz[i] = (float) v[2];
        this.age[i] = 0;
        this.life[i] = lives;
        this.angle[i] = turned;
        this.spin[i] = spins;
        this.size[i] = sized;
        this.colour[i] = laid;
        this.owner[i] = run;
        this.trailHead[i] = 0;
        this.trailCount[i] = 0;
        if (run.emitter().look().trail() > 0) {
            if (this.trail[i] == null) {
                this.trail[i] = new float[TRAIL * 3];
            }
        } else {
            this.trail[i] = null;
        }
        run.alive++;
        return i;
    }

    /** Takes the particle at {@code i} away, the last one taking its place. */
    void remove(final int i) {
        this.owner[i].alive--;
        final int last = --this.count;
        if (i != last) {
            this.x[i] = this.x[last];
            this.y[i] = this.y[last];
            this.z[i] = this.z[last];
            this.lastX[i] = this.lastX[last];
            this.lastY[i] = this.lastY[last];
            this.lastZ[i] = this.lastZ[last];
            this.vx[i] = this.vx[last];
            this.vy[i] = this.vy[last];
            this.vz[i] = this.vz[last];
            this.age[i] = this.age[last];
            this.life[i] = this.life[last];
            this.angle[i] = this.angle[last];
            this.spin[i] = this.spin[last];
            this.size[i] = this.size[last];
            this.colour[i] = this.colour[last];
            this.owner[i] = this.owner[last];
            final float[] swapped = this.trail[i];
            this.trail[i] = this.trail[last];
            this.trail[last] = swapped;
            this.trailHead[i] = this.trailHead[last];
            this.trailCount[i] = this.trailCount[last];
        }
        this.owner[last] = null;
    }

    /** Remembers where the particle at {@code i} is now in its ribbon. */
    void mark(final int i) {
        final float[] ribbon = this.trail[i];
        if (ribbon == null) {
            return;
        }
        final int at = this.trailHead[i] * 3;
        ribbon[at] = (float) this.x[i];
        ribbon[at + 1] = (float) this.y[i];
        ribbon[at + 2] = (float) this.z[i];
        this.trailHead[i] = (this.trailHead[i] + 1) % TRAIL;
        this.trailCount[i] = Math.min(TRAIL, this.trailCount[i] + 1);
    }

    /** Takes every particle away. */
    void clear() {
        for (int i = 0; i < this.count; i++) {
            this.owner[i].alive--;
            this.owner[i] = null;
        }
        this.count = 0;
    }

    private void grow(final int to) {
        this.x = Arrays.copyOf(this.x, to);
        this.y = Arrays.copyOf(this.y, to);
        this.z = Arrays.copyOf(this.z, to);
        this.lastX = Arrays.copyOf(this.lastX, to);
        this.lastY = Arrays.copyOf(this.lastY, to);
        this.lastZ = Arrays.copyOf(this.lastZ, to);
        this.vx = Arrays.copyOf(this.vx, to);
        this.vy = Arrays.copyOf(this.vy, to);
        this.vz = Arrays.copyOf(this.vz, to);
        this.age = Arrays.copyOf(this.age, to);
        this.life = Arrays.copyOf(this.life, to);
        this.angle = Arrays.copyOf(this.angle, to);
        this.spin = Arrays.copyOf(this.spin, to);
        this.size = Arrays.copyOf(this.size, to);
        this.colour = Arrays.copyOf(this.colour, to);
        this.owner = Arrays.copyOf(this.owner, to);
        this.trail = Arrays.copyOf(this.trail, to);
        this.trailHead = Arrays.copyOf(this.trailHead, to);
        this.trailCount = Arrays.copyOf(this.trailCount, to);
    }
}
