/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.Objects;

/**
 * A keyframe of a channel: when it is, the value the channel comes to there ({@code pre}) and the value it leaves
 * with ({@code post}), the same unless the file says otherwise, each a number or an expression; and how the channel
 * comes to it from the keyframe before.
 *
 * @param time      when it is, in seconds
 * @param pre       the value the channel comes to, one expression for each of its numbers
 * @param post      the value it leaves with
 * @param easing    how the channel comes to it from the keyframe before
 * @param argument  the easing's number, as the overshoot of a back curve; NaN for its own
 * @param handles   for a bezier keyframe, the time and value of its left and right handles for each number, four each
 *                  (left time, left value, right time, right value), relative to the keyframe; empty otherwise
 * @param tangents  for a keyframe of a glTF cubic spline, the in and out tangent of each number, two each; empty
 *                  otherwise
 */
public record Keyframe(double time, ModelExpression[] pre, ModelExpression[] post, Easing easing, double argument,
                       float[] handles, float[] tangents) {

    public Keyframe {
        Objects.requireNonNull(pre, "pre");
        Objects.requireNonNull(post, "post");
        Objects.requireNonNull(easing, "easing");
        Objects.requireNonNull(handles, "handles");
        Objects.requireNonNull(tangents, "tangents");
        if (pre.length != post.length) {
            throw new IllegalArgumentException("a keyframe comes to and leaves with values of as many numbers");
        }
    }

    /** A keyframe that comes to and leaves with the same {@code value}, by {@code easing}. */
    public static Keyframe of(final double time, final ModelExpression[] value, final Easing easing) {
        return new Keyframe(time, value, value, easing, Double.NaN, new float[0], new float[0]);
    }

    /** A keyframe of fixed numbers, reached in a straight line. */
    public static Keyframe of(final double time, final float... value) {
        final ModelExpression[] values = new ModelExpression[value.length];
        for (int i = 0; i < value.length; i++) {
            values[i] = ModelExpression.constant(value[i]);
        }
        return of(time, values, Easing.LINEAR);
    }
}
