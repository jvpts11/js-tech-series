/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

/**
 * Where a door of a model turns and how far: an upright line through {@code (x, z)} of the model, in its pixels as the
 * Core reads it (a Bedrock model's X turned round), the door turning {@code degrees} about it as it opens, the way a
 * turn about the model's up goes (seen from above, positive is anticlockwise), over {@code ticks} ticks. Pure.
 *
 * <pre>{@code
 * Hinge.vertical(-23, 92).turns(-90).inTicks(8)
 * }</pre>
 *
 * @param x       where the line stands across the model, in its pixels
 * @param z       where it stands along the model, in its pixels
 * @param degrees how far the door turns as it opens
 * @param ticks   how long it takes to open or close, in ticks; 0 at once
 */
public record Hinge(float x, float z, float degrees, int ticks) {

    /** A door turns a quarter turn unless it says, over this many ticks. */
    public static final float QUARTER = 90;
    public static final int TICKS = 8;

    public Hinge {
        if (!Float.isFinite(x) || !Float.isFinite(z) || !Float.isFinite(degrees) || ticks < 0) {
            throw new IllegalArgumentException("a hinge stands somewhere, turns a finite way and takes no less than "
                    + "no time: " + x + ", " + z + ", " + degrees + ", " + ticks);
        }
    }

    /** An upright hinge through {@code (x, z)} of the model, turning a quarter turn in {@value #TICKS} ticks. */
    public static Hinge vertical(final float x, final float z) {
        return new Hinge(x, z, QUARTER, TICKS);
    }

    /** The same hinge, its door turning {@code turn} degrees as it opens. */
    public Hinge turns(final float turn) {
        return new Hinge(this.x, this.z, turn, this.ticks);
    }

    /** The same hinge, its door taking {@code time} ticks to open or close. */
    public Hinge inTicks(final int time) {
        return new Hinge(this.x, this.z, this.degrees, time);
    }
}
