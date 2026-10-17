/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

/*
 * One line out of a blast, walked block by block from the centre, each block it crosses in turn: how far it has gone
 * and what share of the wave it still carries, the rest spent on what it broke. Distances are along the line, in
 * metres, to where it entered the block it is in.
 */
final class BlastRay {

    private final int stepX;
    private final int stepY;
    private final int stepZ;
    private final double eachX;
    private final double eachY;
    private final double eachZ;
    private int x;
    private int y;
    private int z;
    private double nextX;
    private double nextY;
    private double nextZ;
    private double distance;
    private double share = 1;
    private int crossed = -1;
    private boolean alive = true;

    /* From the point (fromX, fromY, fromZ) along the unit direction (dx, dy, dz). */
    BlastRay(final double fromX, final double fromY, final double fromZ, final double dx, final double dy,
             final double dz) {
        this.x = (int) Math.floor(fromX);
        this.y = (int) Math.floor(fromY);
        this.z = (int) Math.floor(fromZ);
        this.stepX = (int) Math.signum(dx);
        this.stepY = (int) Math.signum(dy);
        this.stepZ = (int) Math.signum(dz);
        this.eachX = dx == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dx);
        this.eachY = dy == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dy);
        this.eachZ = dz == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dz);
        this.nextX = dx == 0 ? Double.POSITIVE_INFINITY
                : (this.stepX > 0 ? this.x + 1 - fromX : fromX - this.x) * this.eachX;
        this.nextY = dy == 0 ? Double.POSITIVE_INFINITY
                : (this.stepY > 0 ? this.y + 1 - fromY : fromY - this.y) * this.eachY;
        this.nextZ = dz == 0 ? Double.POSITIVE_INFINITY
                : (this.stepZ > 0 ? this.z + 1 - fromZ : fromZ - this.z) * this.eachZ;
    }

    /* How far along the line the next block it would enter begins: the centre's own first, at nothing. */
    double nextDistance() {
        return this.crossed < 0 ? 0 : Math.min(this.nextX, Math.min(this.nextY, this.nextZ));
    }

    /* Enters the next block: the centre's own the first time. */
    void advance() {
        this.crossed++;
        if (this.crossed == 0) {
            return;
        }
        if (this.nextX <= this.nextY && this.nextX <= this.nextZ) {
            this.distance = this.nextX;
            this.x += this.stepX;
            this.nextX += this.eachX;
        } else if (this.nextY <= this.nextZ) {
            this.distance = this.nextY;
            this.y += this.stepY;
            this.nextY += this.eachY;
        } else {
            this.distance = this.nextZ;
            this.z += this.stepZ;
            this.nextZ += this.eachZ;
        }
    }

    int x() {
        return this.x;
    }

    int y() {
        return this.y;
    }

    int z() {
        return this.z;
    }

    double distance() {
        return this.distance;
    }

    double share() {
        return this.share;
    }

    boolean alive() {
        return this.alive;
    }

    /* Whether it is in the block the blast went off in. */
    boolean atCentre() {
        return this.crossed == 0;
    }

    /* Spends {@code part} of the wave it carries; it stops when nothing is left. */
    void spend(final double part) {
        this.share -= part;
        if (this.share <= 0) {
            stop();
        }
    }

    void stop() {
        this.alive = false;
    }
}
