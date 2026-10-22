/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

/**
 * What dissonance does to the senses, as numbers: from the first band ({@link #SIGHT_WARPS}) the colours of the view
 * swing slowly towards red and towards blue and the view shimmers, more the further it has built up; from the second
 * ({@link #GRAVITY_DRIFTS}) every sound heard plays a little higher or lower than it should, wavering as time goes.
 * Below a band, its sense is as it always is.
 *
 * <p>Pure: no game types, so the player's game draws and plays by it and a test reads it alike.
 */
public final class DissonanceSenses {

    /** Where the sight starts to warp and the colours to shift: the first band, in percent of what kills. */
    public static final double SIGHT_WARPS = 10;
    /** Where the body's gravity starts to drift and sounds to change pitch: the second band. */
    public static final double GRAVITY_DRIFTS = 25;
    /* Where each sense is as far off as it gets. */
    private static final double SIGHT_WORST = 70;
    private static final double SOUND_WORST = 70;
    /* How far the colours swing at the band and at their worst, on the shift's scale of -1 to 1. */
    private static final double LEAST_SHIFT = 0.2;
    private static final double MOST_SHIFT = 0.6;
    /* How much the view shimmers at the band and at its worst, 0 to 1. */
    private static final double LEAST_SHIMMER = 0.1;
    private static final double MOST_SHIMMER = 0.4;
    /* How far a sound's pitch wavers at the band and at its worst, as a share of its pitch. */
    private static final double LEAST_WAVER = 0.08;
    private static final double MOST_WAVER = 0.25;
    /* How long a swing of the colours and a waver of the pitch take, in seconds. */
    private static final double SWING_SECONDS = 6;
    private static final double WAVER_SECONDS = 4;

    private DissonanceSenses() {
    }

    /**
     * How far the colours are shifted at {@code seconds}, on the scale of the laws of light: below 0 towards blue,
     * above it towards red; 0 below the first band.
     */
    public static double shift(final double level, final double seconds) {
        if (level < SIGHT_WARPS) {
            return 0;
        }
        return between(LEAST_SHIFT, MOST_SHIFT, level, SIGHT_WARPS, SIGHT_WORST) * wave(seconds, SWING_SECONDS);
    }

    /** How much the view shimmers, 0 to 1; 0 below the first band. */
    public static double shimmer(final double level) {
        if (level < SIGHT_WARPS) {
            return 0;
        }
        return between(LEAST_SHIMMER, MOST_SHIMMER, level, SIGHT_WARPS, SIGHT_WORST);
    }

    /** What a sound starting at {@code seconds} has its pitch multiplied by; 1 below the second band. */
    public static double pitch(final double level, final double seconds) {
        if (level < GRAVITY_DRIFTS) {
            return 1;
        }
        return 1 + between(LEAST_WAVER, MOST_WAVER, level, GRAVITY_DRIFTS, SOUND_WORST) * wave(seconds, WAVER_SECONDS);
    }

    /* From {@code least} at {@code from} to {@code most} at {@code worst}, held there past it. */
    private static double between(final double least, final double most, final double level, final double from,
                                  final double worst) {
        final double share = Math.clamp((level - from) / (worst - from), 0, 1);
        return least + (most - least) * share;
    }

    /* A slow wave from -1 to 1 and back, once every {@code period} seconds. */
    private static double wave(final double seconds, final double period) {
        return Math.sin(seconds / period * 2 * Math.PI);
    }
}
