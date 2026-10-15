/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.function.DoubleUnaryOperator;

/**
 * The physics of rotation, in real units: speed in revolutions a minute or radians a second, torque in newton-metres,
 * power in watts, inertia in kilogram square metres. Pure, so its numbers are tested without the game.
 *
 * <ul>
 *   <li>Power is torque times angular speed.</li>
 *   <li>Two gears in mesh turn opposite ways, the driven one slower by as many times as it has more teeth, and with
 *       that much more torque.</li>
 *   <li>A network of parts turns as one: each part at its ratio to the network's speed. Its inertia, seen from the
 *       network, is each part's times the square of its ratio, and each part's torque counts times its ratio.</li>
 * </ul>
 *
 * <p>A step speeds the network up or slows it down by its net torque over its inertia, and never past the speed where
 * the torque would balance: a light network of shafts settles at once, a heavy flywheel takes its time, and neither
 * swings about the balance.
 */
public final class RotationMath {

    /** Radians a second in one revolution a minute. */
    public static final double RADIANS_PER_RPM = 2.0 * Math.PI / 60.0;
    /* How finely a step's balance is found, in radians a second. */
    private static final double SETTLED = 1e-6;
    private static final int HALVINGS = 60;
    /* The least inertia a network has, in kilogram square metres: a network is never weightless. */
    private static final double LIGHTEST = 1e-6;
    /* Closer to a standstill than this, in radians a second, a balance is a standstill. */
    private static final double STANDSTILL = 1e-4;

    private RotationMath() {
    }

    /** {@code rpm} revolutions a minute in radians a second. */
    public static double radians(final double rpm) {
        return rpm * RADIANS_PER_RPM;
    }

    /** {@code radiansPerSecond} in revolutions a minute. */
    public static double rpm(final double radiansPerSecond) {
        return radiansPerSecond / RADIANS_PER_RPM;
    }

    /** The power of {@code torque} newton-metres turning at {@code rpm}, in watts. */
    public static double power(final double torque, final double rpm) {
        return torque * radians(rpm);
    }

    /** The torque that {@code watts} gives at {@code rpm}, in newton-metres; none when it does not turn. */
    public static double torque(final double watts, final double rpm) {
        final double radians = radians(Math.abs(rpm));
        return radians <= 0.0 ? 0.0 : watts / radians;
    }

    /**
     * How fast a gear of {@code drivenTeeth} turns for each turn of the gear of {@code driverTeeth} it meshes with:
     * slower by their ratio, and the other way.
     */
    public static double mesh(final int driverTeeth, final int drivenTeeth) {
        if (driverTeeth <= 0 || drivenTeeth <= 0) {
            throw new IllegalArgumentException("a gear has teeth: " + driverTeeth + ", " + drivenTeeth);
        }
        return -(double) driverTeeth / drivenTeeth;
    }

    /**
     * The network's speed after {@code seconds}, from {@code speed} in radians a second, with {@code inertia} and the
     * net torque the network feels at each speed, {@code torque}: towards the balance, never past it.
     */
    public static double step(final double speed, final double inertia, final DoubleUnaryOperator torque,
                              final double seconds) {
        final double now = torque.applyAsDouble(speed);
        if (now == 0.0 || seconds <= 0.0) {
            return speed;
        }
        final double towards = speed + now / Math.max(LIGHTEST, inertia) * seconds;
        if (Math.signum(torque.applyAsDouble(towards)) == Math.signum(now)) {
            return towards;
        }
        // A network that friction brings to its balance at a standstill stands still, not a hair off it.
        final double balanced = balance(speed, towards, torque);
        return Math.abs(balanced) < STANDSTILL ? 0.0 : balanced;
    }

    /* The speed between {@code from} and {@code to} where the torque changes its sign, found by halving. */
    private static double balance(final double from, final double to, final DoubleUnaryOperator torque) {
        double low = from;
        double high = to;
        final double sign = Math.signum(torque.applyAsDouble(from));
        for (int i = 0; i < HALVINGS && Math.abs(high - low) > SETTLED; i++) {
            final double middle = (low + high) / 2.0;
            if (Math.signum(torque.applyAsDouble(middle)) == sign) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }
}
