/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

/**
 * What a part drives or asks of the network it turns in, at each of its own speeds: a source's torque pushing its own
 * way, positive, and a machine's or a brake's against the way it turns. Pure.
 */
@FunctionalInterface
public interface ITorque {

    /** The torque it puts in while it turns at {@code rpm} its own way, in newton-metres. */
    double torque(double rpm);

    /** A load that asks {@code newtonMetres} against the way it turns, and nothing while it stands. */
    static ITorque load(final double newtonMetres) {
        final double asked = Math.abs(newtonMetres);
        return rpm -> -Math.signum(rpm) * asked;
    }
}
