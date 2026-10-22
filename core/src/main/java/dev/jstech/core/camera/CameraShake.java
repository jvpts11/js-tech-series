/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * A shake of the view: how strong it is at its strongest, from 0 to 1, a few degrees each way at 1; how long it takes
 * to die away, in seconds; how rough it is, how many times a second it turns; and, for one that comes from a place,
 * where it is and how far away it is felt, weaker the farther. Shakes felt at once add up, the strongest showing most.
 * Pure.
 *
 * @param strength  how strong at its strongest, 0 to 1
 * @param seconds   how long it takes to die away
 * @param roughness how many times a second it turns
 * @param from      where it comes from, or nowhere for one felt the same everywhere
 * @param reach     how far from there it is felt, in blocks
 */
public record CameraShake(double strength, double seconds, double roughness, @Nullable Vector3d from,
                          double reach) {

    /** How rough a shake is unless it says: a rumble felt through the ground. */
    public static final double ROUGH = 12.0;

    public CameraShake {
        if (!(strength >= 0) || !(seconds > 0) || !(roughness > 0) || from != null && !(reach > 0)) {
            throw new IllegalArgumentException("a shake has a strength of 0 or more, a time and a roughness above 0, "
                    + "and a reach above 0 when it comes from a place: " + strength + ", " + seconds + ", "
                    + roughness + ", " + reach);
        }
        from = from == null ? null : new Vector3d(from);
    }

    /** A shake of {@code strength} that dies away over {@code seconds}, felt the same everywhere. */
    public static CameraShake of(final double strength, final double seconds) {
        return new CameraShake(strength, seconds, ROUGH, null, 0);
    }

    /** It, turning {@code timesASecond}. */
    public CameraShake rough(final double timesASecond) {
        return new CameraShake(this.strength, this.seconds, timesASecond, this.from, this.reach);
    }

    /** It, coming from {@code at} and felt as far as {@code blocks} from there. */
    public CameraShake from(final Vector3d at, final double blocks) {
        return new CameraShake(this.strength, this.seconds, this.roughness, at, blocks);
    }

    /** How strong it is felt {@code away} blocks from where it comes from: all of it there, none past its reach. */
    public double felt(final double away) {
        if (this.from == null) {
            return this.strength;
        }
        final double share = Math.clamp(1.0 - away / this.reach, 0.0, 1.0);
        return this.strength * share * share;
    }

    /** How much of it is left {@code elapsed} seconds after it began: all at first, dying away to none at its end. */
    public double left(final double elapsed) {
        final double share = Math.clamp(1.0 - elapsed / this.seconds, 0.0, 1.0);
        return share * share;
    }
}
