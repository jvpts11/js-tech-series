/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnimationChannelTest {

    private static final double EPSILON = 1.0e-4;

    @Test
    void evaluate_holdsTheFirstAndLastValuesOutsideItsKeyframes() {
        final AnimationChannel channel = position(Keyframe.of(1, 2, 0, 0), Keyframe.of(2, 4, 0, 0));
        assertEquals(2, at(channel, 0)[0], EPSILON);
        assertEquals(4, at(channel, 5)[0], EPSILON);
        assertEquals(3, at(channel, 1.5)[0], EPSILON);
    }

    @Test
    void evaluate_stepsHoldUntilTheNextKeyframe() {
        final ModelExpression[] ten = {ModelExpression.constant(10), ModelExpression.ZERO, ModelExpression.ZERO};
        final AnimationChannel channel = position(Keyframe.of(0, 0, 0, 0), Keyframe.of(1, ten, Easing.STEP));
        assertEquals(0, at(channel, 0.99)[0], EPSILON);
        assertEquals(10, at(channel, 1)[0], EPSILON);
    }

    @Test
    void evaluate_takesAKeyframesValueBeforeAndAfterApart() {
        final ModelExpression[] one = {ModelExpression.constant(1), ModelExpression.ZERO, ModelExpression.ZERO};
        final ModelExpression[] five = {ModelExpression.constant(5), ModelExpression.ZERO, ModelExpression.ZERO};
        final AnimationChannel channel = position(Keyframe.of(0, 0, 0, 0),
                new Keyframe(1, one, five, Easing.LINEAR, Double.NaN, new float[0], new float[0]),
                Keyframe.of(2, 5, 0, 0));
        assertEquals(0.5, at(channel, 0.5)[0], EPSILON, "coming to the value before");
        assertEquals(5, at(channel, 1.5)[0], EPSILON, "leaving with the value after");
    }

    @Test
    void evaluate_catmullRomPassesThroughEveryKeyframe() {
        final ModelExpression[] up = {ModelExpression.constant(4), ModelExpression.ZERO, ModelExpression.ZERO};
        final AnimationChannel channel = position(Keyframe.of(0, 0, 0, 0), Keyframe.of(1, up, Easing.CATMULLROM),
                Keyframe.of(2, 0, 0, 0));
        assertEquals(4, at(channel, 1)[0], EPSILON);
        final float between = at(channel, 0.5)[0];
        assertTrue(between > 2, "and bends toward the next one: " + between);
    }

    @Test
    void constructor_refusesKeyframesOfTheWrongWidth() {
        assertThrows(IllegalArgumentException.class, () -> new AnimationChannel(AnimationChannel.Kind.QUATERNION,
                List.of(Keyframe.of(0, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new AnimationChannel(AnimationChannel.Kind.POSITION,
                List.of()));
    }

    private static AnimationChannel position(final Keyframe... keyframes) {
        return new AnimationChannel(AnimationChannel.Kind.POSITION, List.of(keyframes));
    }

    private static float[] at(final AnimationChannel channel, final double time) {
        final float[] out = new float[3];
        channel.evaluate(time, new ExpressionContext(), out);
        return out;
    }
}
