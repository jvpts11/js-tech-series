/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import java.util.Objects;

/**
 * A stamp laid on a model: the point it struck, in the model's own space in blocks as the model stands facing north,
 * the way the surface faced there, how big the damage is, and the seed its rough rim and its petals fall by. Pure.
 *
 * @param stamp the kind of damage
 * @param x     the point it struck
 * @param y     the point it struck
 * @param z     the point it struck
 * @param nx    the way the surface faced there, made one long
 * @param ny    the way the surface faced there
 * @param nz    the way the surface faced there
 * @param size  how big the damage is, in blocks: the stamp's lengths are shares of it
 * @param seed  what its rough rim and its petals fall by
 * @param lost  whether the block its point is the middle of is lost: every face within that block goes first, and the
 *              stamp tears the hole round where it was
 */
public record Strike(DamageStamp stamp, double x, double y, double z, double nx, double ny, double nz, double size,
                     long seed, boolean lost) {

    /** How far out from its point, in shares of its size, a stamp moves, cuts or draws over any face. */
    public static final double REACH = 1.8;
    /* How far out the faces are cut finer, a little past where they move, so the last of them moves smoothly. */
    static final double FINE_REACH = 1.9;

    public Strike {
        Objects.requireNonNull(stamp, "stamp");
        final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (!(length > 0) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("a strike needs the way the surface faced, not (" + nx + ", " + ny
                    + ", " + nz + ")");
        }
        if (!(size > 0) || !Double.isFinite(size)) {
            throw new IllegalArgumentException("a strike's size is more than nothing, not " + size);
        }
        nx /= length;
        ny /= length;
        nz /= length;
    }

    /** A strike that loses no block. */
    public Strike(final DamageStamp stamp, final double x, final double y, final double z, final double nx,
                  final double ny, final double nz, final double size, final long seed) {
        this(stamp, x, y, z, nx, ny, nz, size, seed, false);
    }

    /** How far out from its point it reaches, in blocks: no face farther is moved, cut or drawn over. */
    public double reach() {
        return this.size * REACH;
    }
}
