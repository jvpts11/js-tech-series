/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

/**
 * One motion under way: how it moves and when it started, read against the clock each frame.
 *
 * <p>It holds no timer of its own and is not ticked: whoever draws it asks how far along it is at the time of the
 * frame, which is what keeps it smooth between ticks and costs nothing once it is over.
 *
 * @param spec    how it moves
 * @param startMs when it started, on the clock it is read against
 */
public record Motion(MotionSpec spec, double startMs) {

    /** A motion that is already over: what anything that does not move is drawn as. */
    public static final Motion FINISHED = new Motion(MotionSpec.NONE, 0.0);

    /** How much of its time has passed at {@code nowMs}, from 0 to 1, before the curve. */
    public double elapsed(final double nowMs) {
        if (!spec.moves()) {
            return 1.0;
        }
        final double since = nowMs - startMs - spec.delay();
        return Math.max(0.0, Math.min(1.0, since / spec.duration()));
    }

    /** How far along it is at {@code nowMs}, from 0 to 1, along its curve. */
    public double progress(final double nowMs) {
        return spec.easing().apply(elapsed(nowMs));
    }

    /** Whether it is over at {@code nowMs}. */
    public boolean done(final double nowMs) {
        return elapsed(nowMs) >= 1.0;
    }

    /** Whether it is of that style, whether or not it is over. */
    public boolean is(final String style) {
        return spec.style().equals(style);
    }

    /**
     * The size across a {@link MotionStyles#SCALE} motion draws at, at {@code nowMs}: from its {@code from} size to
     * its {@code to} size, its own size where it does not say. A window coming up grows from less to 1; one going
     * away shrinks from 1 to less.
     */
    public double scale(final double nowMs) {
        final double from = spec.param("from", 1.0);
        return from + (spec.param("to", 1.0) - from) * progress(nowMs);
    }

    /** The size downwards a {@link MotionStyles#SCALE} motion draws at: as {@link #scale}, unless it grows unevenly. */
    public double scaleY(final double nowMs) {
        final double from = spec.param("from_y", spec.param("from", 1.0));
        return from + (spec.param("to_y", spec.param("to", 1.0)) - from) * progress(nowMs);
    }

    /**
     * How far a {@link MotionStyles#SLIDE} motion is from where it rests, at {@code nowMs}, in the moving thing's own
     * heights: {@code distance} at the start, nothing at the end; or the other way for one going away, {@code out}.
     */
    public double offset(final double nowMs) {
        final double distance = spec.param("distance", 0.0);
        final double along = progress(nowMs);
        return outward() ? distance * along : distance * (1.0 - along);
    }

    /**
     * How far a {@link MotionStyles#ZOOM} or {@link MotionStyles#OUTLINE} motion has got from the thing towards the
     * place that stands for it, at {@code nowMs}: 0 where the thing stands, 1 at that place. Going there runs from 0
     * to 1; coming back from 1 to 0.
     */
    public double toward(final double nowMs) {
        final double along = progress(nowMs);
        return outward() ? along : 1.0 - along;
    }

    /** Whether it carries the thing away rather than bringing it in. */
    public boolean outward() {
        return spec.param("out", 0.0) > 0.0;
    }
}
