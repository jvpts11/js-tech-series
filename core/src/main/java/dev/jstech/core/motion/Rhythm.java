/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

/**
 * Reads the motions that go round and round instead of ending, at a time since they began: a cursor blinking, a
 * throbber turning, a bar for a wait with no known end. Each pass takes the motion's time; one that does not move
 * stands still in its first position, and a blink that does not move is always on.
 */
public final class Rhythm {

    private Rhythm() {
    }

    /** Whether a {@link MotionStyles#BLINK} shows its thing {@code elapsedMs} after it began: on, off, on again. */
    public static boolean on(final MotionSpec spec, final double elapsedMs) {
        if (!spec.moves() || elapsedMs <= 0.0) {
            return true;
        }
        return ((long) Math.floor(elapsedMs / spec.duration())) % 2 == 0;
    }

    /** How far through the pass it is in, from 0 to just under 1, along its curve. */
    public static double pass(final MotionSpec spec, final double elapsedMs) {
        if (!spec.moves() || elapsedMs <= 0.0) {
            return 0.0;
        }
        return spec.easing().apply(within(spec, elapsedMs));
    }

    /** The picture a {@link MotionStyles#LOOP} of {@code frames} pictures shows, counting from 0. */
    public static int frame(final MotionSpec spec, final double elapsedMs) {
        final int frames = Math.max(1, (int) spec.param("frames", 1.0));
        if (!spec.moves() || elapsedMs <= 0.0) {
            return 0;
        }
        return Math.min(frames - 1, (int) Math.floor(within(spec, elapsedMs) * frames));
    }

    /**
     * Where a {@link MotionStyles#BOUNCE} block is: 0 at the end it starts from and 1 at the other, the way there
     * taking the motion's time and the way back as long, both along its curve.
     */
    public static double bounce(final MotionSpec spec, final double elapsedMs) {
        if (!spec.moves() || elapsedMs <= 0.0) {
            return 0.0;
        }
        final double ways = elapsedMs / spec.duration();
        final double along = spec.easing().apply(ways - Math.floor(ways));
        return ((long) Math.floor(ways)) % 2 == 0 ? along : 1.0 - along;
    }

    private static double within(final MotionSpec spec, final double elapsedMs) {
        return (elapsedMs % spec.duration()) / spec.duration();
    }
}
