/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The keyframes of one thing an animation moves: a bone's turn, its place or its size, or a morph target's weight,
 * in their order, worked out at any time by easing from one to the next. A turn is either three angles in degrees,
 * added to the bone's rest turn as Bedrock and Blockbench do, or a quaternion that takes the bone's turn's place, as
 * glTF and FBX do. Pure.
 *
 * @param kind      what it moves
 * @param keyframes its keyframes, earliest first
 */
public record AnimationChannel(Kind kind, List<Keyframe> keyframes) {

    private static final double SAME_TIME = 1.0e-9;
    private static final int BEZIER_STEPS = 24;

    public AnimationChannel {
        Objects.requireNonNull(kind, "kind");
        final List<Keyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort(Comparator.comparingDouble(Keyframe::time));
        keyframes = List.copyOf(sorted);
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("a channel has at least one keyframe");
        }
        for (final Keyframe keyframe : keyframes) {
            if (keyframe.pre().length != kind.numbers()) {
                throw new IllegalArgumentException("a channel's keyframes have " + kind.numbers() + " numbers each");
            }
        }
    }

    /** The time of its last keyframe, in seconds. */
    public double length() {
        return this.keyframes.getLast().time();
    }

    /** Works out its value at {@code time}, in seconds, into {@code out}, as many numbers as its kind has. */
    public void evaluate(final double time, final ExpressionContext context, final float[] out) {
        final int last = this.keyframes.size() - 1;
        int next = 0;
        while (next <= last && this.keyframes.get(next).time() <= time + SAME_TIME) {
            next++;
        }
        if (next == 0) {
            values(this.keyframes.getFirst().pre(), context, out);
            return;
        }
        final Keyframe from = this.keyframes.get(next - 1);
        if (next > last) {
            values(from.post(), context, out);
            return;
        }
        final Keyframe to = this.keyframes.get(next);
        final double span = to.time() - from.time();
        final double t = span <= SAME_TIME ? 1.0 : (time - from.time()) / span;
        final int numbers = this.kind.numbers();
        final float[] a = new float[numbers];
        final float[] b = new float[numbers];
        values(from.post(), context, a);
        values(to.pre(), context, b);
        switch (to.easing()) {
            case STEP -> System.arraycopy(a, 0, out, 0, numbers);
            case CATMULLROM -> catmullRom(next, t, a, b, context, out);
            case BEZIER -> bezier(from, to, t, span, a, b, out);
            default -> {
                if (from.tangents().length > 0 && to.tangents().length > 0) {
                    hermite(from, to, t, span, a, b, out);
                } else {
                    blend(a, b, (float) to.easing().apply(t, to.argument()), out);
                }
            }
        }
        if (this.kind == Kind.QUATERNION) {
            normalise(out);
        }
    }

    /* From {@code a} to {@code b} by {@code s}: a straight line, or the short arc between two turns. */
    private void blend(final float[] a, final float[] b, final float s, final float[] out) {
        if (this.kind == Kind.QUATERNION) {
            final float dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
            final float sign = dot < 0 ? -1.0F : 1.0F;
            for (int i = 0; i < 4; i++) {
                out[i] = a[i] + (b[i] * sign - a[i]) * s;
            }
            return;
        }
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i] + (b[i] - a[i]) * s;
        }
    }

    /* Through the keyframes either side, so the value moves without a corner. */
    private void catmullRom(final int next, final double t, final float[] a, final float[] b,
                            final ExpressionContext context, final float[] out) {
        final float[] before = new float[a.length];
        final float[] after = new float[a.length];
        values(this.keyframes.get(Math.max(0, next - 2)).post(), context, before);
        values(this.keyframes.get(Math.min(this.keyframes.size() - 1, next + 1)).pre(), context, after);
        final double t2 = t * t;
        final double t3 = t2 * t;
        for (int i = 0; i < a.length; i++) {
            out[i] = (float) (0.5 * (2 * a[i] + (b[i] - before[i]) * t
                    + (2 * before[i] - 5 * a[i] + 4 * b[i] - after[i]) * t2
                    + (3 * a[i] - before[i] - 3 * b[i] + after[i]) * t3));
        }
    }

    /*
     * Blockbench's bezier: each number a curve through the two keyframes and the right handle of the first and the
     * left of the second, as time against value; the value is read at the time, found along the curve.
     */
    private static void bezier(final Keyframe from, final Keyframe to, final double t, final double span,
                               final float[] a, final float[] b, final float[] out) {
        for (int i = 0; i < a.length; i++) {
            final double rightTime = from.handles().length > i * 4 + 3 ? from.handles()[i * 4 + 2] : span / 3;
            final double rightValue = from.handles().length > i * 4 + 3 ? from.handles()[i * 4 + 3] : 0;
            final double leftTime = to.handles().length > i * 4 + 1 ? to.handles()[i * 4] : -span / 3;
            final double leftValue = to.handles().length > i * 4 + 1 ? to.handles()[i * 4 + 1] : 0;
            final double x1 = Math.clamp(rightTime / span, 0, 1);
            final double x2 = Math.clamp(1 + leftTime / span, 0, 1);
            // The curve's parameter whose time is t, by halving: the times along a bezier rise with its parameter.
            double low = 0;
            double high = 1;
            double u = t;
            for (int step = 0; step < BEZIER_STEPS; step++) {
                u = (low + high) / 2;
                if (cubic(0, x1, x2, 1, u) < t) {
                    low = u;
                } else {
                    high = u;
                }
            }
            out[i] = (float) cubic(a[i], a[i] + rightValue, b[i] + leftValue, b[i], u);
        }
    }

    /* glTF's cubic spline: from each keyframe's value along its out tangent, to the next's along its in tangent. */
    private static void hermite(final Keyframe from, final Keyframe to, final double t, final double span,
                                final float[] a, final float[] b, final float[] out) {
        final double t2 = t * t;
        final double t3 = t2 * t;
        for (int i = 0; i < a.length; i++) {
            final double outTangent = from.tangents()[i * 2 + 1] * span;
            final double inTangent = to.tangents()[i * 2] * span;
            out[i] = (float) ((2 * t3 - 3 * t2 + 1) * a[i] + (t3 - 2 * t2 + t) * outTangent
                    + (-2 * t3 + 3 * t2) * b[i] + (t3 - t2) * inTangent);
        }
    }

    private static double cubic(final double p0, final double p1, final double p2, final double p3, final double u) {
        final double v = 1 - u;
        return v * v * v * p0 + 3 * v * v * u * p1 + 3 * v * u * u * p2 + u * u * u * p3;
    }

    private static void values(final ModelExpression[] values, final ExpressionContext context, final float[] out) {
        for (int i = 0; i < values.length; i++) {
            out[i] = (float) values[i].evaluate(context);
        }
    }

    private static void normalise(final float[] q) {
        final double length = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (length > 0) {
            for (int i = 0; i < 4; i++) {
                q[i] = (float) (q[i] / length);
            }
        }
    }

    /** What a channel moves. */
    public enum Kind {
        /** A bone's turn, three angles in degrees added to its rest turn. */
        EULER(3),
        /** A bone's turn, a quaternion (x, y, z, w) that takes its rest turn's place. */
        QUATERNION(4),
        /** How far a bone is moved from its rest place, in pixels. */
        POSITION(3),
        /** A bone's size, by which its rest size is multiplied. */
        SCALE(3),
        /** A morph target's weight, from 0 to 1. */
        WEIGHT(1);

        private final int numbers;

        Kind(final int numbers) {
            this.numbers = numbers;
        }

        /** How many numbers a value of it has. */
        public int numbers() {
            return this.numbers;
        }
    }
}
