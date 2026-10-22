/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

/**
 * The arithmetic of how the view moves on its own: the noise a shake turns it by, smooth and never repeating as a
 * sine would, and the way a turn of the view is eased towards where it is wanted and back, as fast as it can without
 * swinging past. Pure.
 */
public final class CameraMotions {

    /** How far a shake of strength 1 turns the view each way at most, in degrees. */
    public static final double MOST_DEGREES = 3.0;

    private static final long MIX = 0x9E3779B97F4A7C15L;
    private static final double TO_UNIT = 1.0 / (1L << 53);

    private CameraMotions() {
    }

    /**
     * Smooth noise at {@code time}, from -1 to 1: values at whole times drawn from {@code seed}, eased between, so it
     * wanders as a hand-held camera does. Each axis of a shake takes its own seed.
     */
    public static double noise(final double time, final long seed) {
        final long whole = (long) Math.floor(time);
        final double part = time - whole;
        final double eased = part * part * part * (part * (part * 6 - 15) + 10);
        final double before = lattice(whole, seed);
        final double after = lattice(whole + 1, seed);
        return before + (after - before) * eased;
    }

    /**
     * How far a shake turns the view, in degrees, as strong as it is felt now: the square of its strength, so a light
     * shake is a tremor and a strong one throws the view about.
     */
    public static double shakeDegrees(final double felt) {
        final double strength = Math.clamp(felt, 0.0, 1.0);
        return MOST_DEGREES * strength * strength;
    }

    /**
     * One step of easing {@code value}, going at {@code speed}, towards {@code target} over about {@code seconds},
     * {@code step} seconds on: as fast as it can get there without swinging past. Returns the new value and speed.
     */
    public static double[] settle(final double value, final double speed, final double target, final double seconds,
                                  final double step) {
        final double time = Math.max(1e-4, seconds);
        final double omega = 2.0 / time;
        final double x = omega * step;
        final double fade = 1.0 / (1.0 + x + 0.48 * x * x + 0.235 * x * x * x);
        final double change = value - target;
        final double pull = (speed + omega * change) * step;
        return new double[] {target + (change + pull) * fade, (speed - omega * pull) * fade};
    }

    /* A value from -1 to 1 for each whole time, the same for the same seed. */
    private static double lattice(final long at, final long seed) {
        long hash = (at ^ seed * MIX) * MIX;
        hash ^= hash >>> 31;
        hash *= MIX;
        hash ^= hash >>> 29;
        return ((hash >>> 11) * TO_UNIT) * 2.0 - 1.0;
    }
}
