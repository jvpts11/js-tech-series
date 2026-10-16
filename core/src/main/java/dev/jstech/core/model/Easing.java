/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.id.IStableName;
import org.jetbrains.annotations.Nullable;

/**
 * How a keyframe goes over to the next: straight, in a step, along a smooth curve through its neighbours, or by one of
 * the easing curves Blockbench has, each going in, out or both. Some take a number, as the overshoot of a back curve
 * or the bounces of an elastic one.
 */
public enum Easing implements IStableName {

    LINEAR("linear"),
    STEP("step"),
    CATMULLROM("catmullrom"),
    BEZIER("bezier"),
    EASE_IN_SINE("easeInSine"),
    EASE_OUT_SINE("easeOutSine"),
    EASE_IN_OUT_SINE("easeInOutSine"),
    EASE_IN_QUAD("easeInQuad"),
    EASE_OUT_QUAD("easeOutQuad"),
    EASE_IN_OUT_QUAD("easeInOutQuad"),
    EASE_IN_CUBIC("easeInCubic"),
    EASE_OUT_CUBIC("easeOutCubic"),
    EASE_IN_OUT_CUBIC("easeInOutCubic"),
    EASE_IN_QUART("easeInQuart"),
    EASE_OUT_QUART("easeOutQuart"),
    EASE_IN_OUT_QUART("easeInOutQuart"),
    EASE_IN_QUINT("easeInQuint"),
    EASE_OUT_QUINT("easeOutQuint"),
    EASE_IN_OUT_QUINT("easeInOutQuint"),
    EASE_IN_EXPO("easeInExpo"),
    EASE_OUT_EXPO("easeOutExpo"),
    EASE_IN_OUT_EXPO("easeInOutExpo"),
    EASE_IN_CIRC("easeInCirc"),
    EASE_OUT_CIRC("easeOutCirc"),
    EASE_IN_OUT_CIRC("easeInOutCirc"),
    EASE_IN_BACK("easeInBack"),
    EASE_OUT_BACK("easeOutBack"),
    EASE_IN_OUT_BACK("easeInOutBack"),
    EASE_IN_ELASTIC("easeInElastic"),
    EASE_OUT_ELASTIC("easeOutElastic"),
    EASE_IN_OUT_ELASTIC("easeInOutElastic"),
    EASE_IN_BOUNCE("easeInBounce"),
    EASE_OUT_BOUNCE("easeOutBounce"),
    EASE_IN_OUT_BOUNCE("easeInOutBounce");

    private final String serializedName;

    private static final double BACK = 1.70158;
    private static final double ELASTIC = 2 * Math.PI / 3;

    Easing(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The easing a file names, by Blockbench's or Bedrock's name, any case; linear for one not known. */
    public static Easing byName(@Nullable final String name) {
        if (name == null) {
            return LINEAR;
        }
        for (final Easing easing : values()) {
            if (easing.serializedName.equalsIgnoreCase(name)) {
                return easing;
            }
        }
        return LINEAR;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /**
     * How far along the way to the next keyframe the value is at {@code t} of the time between them, both from 0 to 1;
     * {@code argument} is the curve's number, the overshoot of a back curve or the bounciness of an elastic or bounce
     * one, or NaN for the curve's own. Step, smooth and bezier keyframes are worked out by their channel.
     */
    public double apply(final double t, final double argument) {
        return switch (this) {
            case LINEAR, CATMULLROM, BEZIER -> t;
            case STEP -> t < 1.0 ? 0.0 : 1.0;
            case EASE_IN_SINE -> 1 - Math.cos(t * Math.PI / 2);
            case EASE_OUT_SINE -> Math.sin(t * Math.PI / 2);
            case EASE_IN_OUT_SINE -> -(Math.cos(Math.PI * t) - 1) / 2;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT_QUAD -> inOut(t, 2);
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> 1 - Math.pow(1 - t, 3);
            case EASE_IN_OUT_CUBIC -> inOut(t, 3);
            case EASE_IN_QUART -> Math.pow(t, 4);
            case EASE_OUT_QUART -> 1 - Math.pow(1 - t, 4);
            case EASE_IN_OUT_QUART -> inOut(t, 4);
            case EASE_IN_QUINT -> Math.pow(t, 5);
            case EASE_OUT_QUINT -> 1 - Math.pow(1 - t, 5);
            case EASE_IN_OUT_QUINT -> inOut(t, 5);
            case EASE_IN_EXPO -> t == 0 ? 0 : Math.pow(2, 10 * t - 10);
            case EASE_OUT_EXPO -> t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
            case EASE_IN_OUT_EXPO -> t == 0 || t == 1 ? t
                    : t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
            case EASE_IN_CIRC -> 1 - Math.sqrt(1 - t * t);
            case EASE_OUT_CIRC -> Math.sqrt(1 - (t - 1) * (t - 1));
            case EASE_IN_OUT_CIRC -> t < 0.5 ? (1 - Math.sqrt(1 - 4 * t * t)) / 2
                    : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
            case EASE_IN_BACK -> backIn(t, overshoot(argument));
            case EASE_OUT_BACK -> 1 - backIn(1 - t, overshoot(argument));
            case EASE_IN_OUT_BACK -> t < 0.5 ? backIn(2 * t, overshoot(argument)) / 2
                    : 1 - backIn(2 - 2 * t, overshoot(argument)) / 2;
            case EASE_IN_ELASTIC -> 1 - elasticOut(1 - t, bounciness(argument));
            case EASE_OUT_ELASTIC -> elasticOut(t, bounciness(argument));
            case EASE_IN_OUT_ELASTIC -> t < 0.5 ? (1 - elasticOut(1 - 2 * t, bounciness(argument))) / 2
                    : (1 + elasticOut(2 * t - 1, bounciness(argument))) / 2;
            case EASE_IN_BOUNCE -> 1 - bounceOut(1 - t);
            case EASE_OUT_BOUNCE -> bounceOut(t);
            case EASE_IN_OUT_BOUNCE -> t < 0.5 ? (1 - bounceOut(1 - 2 * t)) / 2 : (1 + bounceOut(2 * t - 1)) / 2;
        };
    }

    private static double inOut(final double t, final int power) {
        return t < 0.5 ? Math.pow(2, power - 1) * Math.pow(t, power) : 1 - Math.pow(-2 * t + 2, power) / 2;
    }

    private static double overshoot(final double argument) {
        return Double.isNaN(argument) ? BACK : argument;
    }

    private static double bounciness(final double argument) {
        return Double.isNaN(argument) ? 1.0 : Math.max(argument, 0.0001);
    }

    private static double backIn(final double t, final double overshoot) {
        return (overshoot + 1) * t * t * t - overshoot * t * t;
    }

    /* A spring let go: past the mark and back, settling by the end; more bounciness, more swings. */
    private static double elasticOut(final double t, final double bounciness) {
        if (t <= 0 || t >= 1) {
            return Math.clamp(t, 0, 1);
        }
        return Math.pow(2, -10 * t) * Math.sin((t * 10 * bounciness - 0.75) * ELASTIC) + 1;
    }

    private static double bounceOut(final double t) {
        final double n = 7.5625;
        final double d = 2.75;
        if (t < 1 / d) {
            return n * t * t;
        } else if (t < 2 / d) {
            final double s = t - 1.5 / d;
            return n * s * s + 0.75;
        } else if (t < 2.5 / d) {
            final double s = t - 2.25 / d;
            return n * s * s + 0.9375;
        }
        final double s = t - 2.625 / d;
        return n * s * s + 0.984375;
    }
}
