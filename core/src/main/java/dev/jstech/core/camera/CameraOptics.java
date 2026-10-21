/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

/**
 * The optics of looking through a lens, as numbers: the field of view a magnification leaves, and how much slower the
 * mouse must turn the view to feel the same through it.
 */
public final class CameraOptics {

    /** The strongest magnification a lens of the kit gives: a large telescope's. */
    public static final double MOST_POWER = 1000.0;
    /* The game's mouse sensitivity is turned into a speed as (s * 0.6 + 0.2) cubed. */
    private static final double SENSITIVITY_SCALE = 0.6;
    private static final double SENSITIVITY_FLOOR = 0.2;

    private CameraOptics() {
    }

    /**
     * The field of view, in degrees, through a lens of {@code power} for an eye that sees {@code fov} degrees: the
     * tangent of half the angle divided by the power, as a lens divides it. A power of 1 leaves it as it is.
     */
    public static double fieldOfView(final double fov, final double power) {
        if (power <= 1.0) {
            return fov;
        }
        final double half = Math.toRadians(fov) / 2.0;
        return Math.toDegrees(2.0 * Math.atan(Math.tan(half) / power));
    }

    /**
     * The game's mouse sensitivity, from 0 to 1, that turns the view {@code power} times slower than {@code
     * sensitivity} does: the game turns by the cube of {@code sensitivity * 0.6 + 0.2}, so that is what is divided.
     */
    public static double sensitivity(final double sensitivity, final double power) {
        if (power <= 1.0) {
            return sensitivity;
        }
        final double base = (sensitivity * SENSITIVITY_SCALE + SENSITIVITY_FLOOR) / Math.cbrt(power);
        return (base - SENSITIVITY_FLOOR) / SENSITIVITY_SCALE;
    }

    /**
     * The place of the power after the one at {@code from} among {@code count} powers, lowest first: one step up or
     * down, held at the ends.
     */
    public static int step(final int from, final int count, final boolean up) {
        if (count <= 0) {
            return 0;
        }
        final int next = up ? from + 1 : from - 1;
        return Math.max(0, Math.min(count - 1, next));
    }
}
