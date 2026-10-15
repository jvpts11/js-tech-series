/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

/**
 * What a source of rotation gives at each speed, as real ones do: its full torque from a standstill, falling evenly as
 * it turns faster to nothing at its free speed, as a water wheel's or an electric motor's does, and never more than
 * its power allows. A water wheel, a windmill, a steam engine, an electric motor and a hand crank each declare
 * theirs; a network of too little power for what it drives turns slower, where the torque it gets meets the torque it
 * is asked. Pure.
 *
 * <pre>{@code
 * TorqueCurve crank = TorqueCurve.of(20, 75, 60);   // 20 N·m, 75 W (a man's arm), up to 60 rpm
 * }</pre>
 *
 * @param stall   the torque from a standstill, in newton-metres
 * @param power   the most power it gives, in watts
 * @param freeRpm the speed past which it gives nothing, in revolutions a minute
 */
public record TorqueCurve(double stall, double power, double freeRpm) {

    /** A source that gives nothing. */
    public static final TorqueCurve NONE = new TorqueCurve(0.0, 0.0, 0.0);

    public TorqueCurve {
        if (!(stall >= 0.0) || !(power >= 0.0) || !(freeRpm >= 0.0)) {
            throw new IllegalArgumentException("a source gives no less than nothing, up to a speed no less than none");
        }
    }

    /** A source of {@code stall} newton-metres and {@code watts}, up to {@code freeRpm}. */
    public static TorqueCurve of(final double stall, final double watts, final double freeRpm) {
        return new TorqueCurve(stall, watts, freeRpm);
    }

    /**
     * The torque it gives its own way while it turns at {@code rpm}, in newton-metres: its full torque from a
     * standstill, and when something turns it backwards.
     */
    public double torque(final double rpm) {
        if (rpm >= this.freeRpm) {
            return 0.0;
        }
        if (rpm <= 0.0) {
            return this.stall;
        }
        return Math.min(this.stall * (1.0 - rpm / this.freeRpm), RotationMath.torque(this.power, rpm));
    }

    /** The same, its torque and power times {@code share}: a water wheel given less water, a motor run slower. */
    public TorqueCurve times(final double share) {
        final double kept = Math.max(0.0, share);
        return new TorqueCurve(this.stall * kept, this.power * kept, this.freeRpm);
    }
}
