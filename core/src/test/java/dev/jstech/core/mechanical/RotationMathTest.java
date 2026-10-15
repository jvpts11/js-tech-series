/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotationMathTest {

    @Test
    void radians_ofSixtyRpmAreOneTurnASecond() {
        assertEquals(2.0 * Math.PI, RotationMath.radians(60.0), 1e-12);
        assertEquals(60.0, RotationMath.rpm(2.0 * Math.PI), 1e-12);
    }

    @Test
    void power_isTorqueTimesAngularSpeed() {
        assertEquals(1_000.0 * 2.0 * Math.PI, RotationMath.power(1_000.0, 60.0), 1e-9);
        assertEquals(1_000.0, RotationMath.torque(RotationMath.power(1_000.0, 60.0), 60.0), 1e-9);
    }

    @Test
    void torque_ofAStandingShaftIsNone() {
        assertEquals(0.0, RotationMath.torque(500.0, 0.0), 1e-12);
    }

    @Test
    void mesh_turnsTheOtherWaySlowerByTheTeeth() {
        assertEquals(-0.5, RotationMath.mesh(16, 32), 1e-12);
        assertEquals(-2.0, RotationMath.mesh(32, 16), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> RotationMath.mesh(0, 16));
    }

    @Test
    void step_speedsUpByTorqueOverInertia() {
        final double speed = RotationMath.step(0.0, 10.0, any -> 100.0, 0.05);
        assertEquals(0.5, speed, 1e-12);
    }

    @Test
    void step_neverPassesTheBalance() {
        // A net torque of 100 - 10 w balances at 10 rad/s; a light network would leap past it in a step.
        final double speed = RotationMath.step(0.0, 0.01, w -> 100.0 - 10.0 * w, 0.05);
        assertEquals(10.0, speed, 1e-4);
        assertTrue(speed <= 10.0, "it stops at the balance, not past it: " + speed);
    }

    @Test
    void step_withFrictionAloneStopsAndDoesNotTurnBack() {
        final double speed = RotationMath.step(1.0, 0.01, w -> -Math.signum(w) * 50.0, 0.05);
        assertEquals(0.0, speed, 1e-12);
    }

    @Test
    void torqueCurve_fallsEvenlyToNothingAtItsFreeSpeed() {
        final TorqueCurve crank = TorqueCurve.of(20.0, 1_000.0, 60.0);
        assertEquals(20.0, crank.torque(0.0), 1e-12);
        assertEquals(10.0, crank.torque(30.0), 1e-12);
        assertEquals(0.0, crank.torque(60.0), 1e-12);
        assertEquals(20.0, crank.torque(-10.0), 1e-12, "turned backwards, it pushes with its whole torque");
    }

    @Test
    void torqueCurve_neverGivesMoreThanItsPower() {
        final TorqueCurve weak = TorqueCurve.of(1_000.0, 75.0, 600.0);
        assertEquals(RotationMath.torque(75.0, 100.0), weak.torque(100.0), 1e-9);
    }
}
