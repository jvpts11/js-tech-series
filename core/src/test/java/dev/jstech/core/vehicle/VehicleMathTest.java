/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleMathTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void grip_holdsOnStoneAndBarelyOnIce() {
        assertEquals(1.0, VehicleMath.grip(1.0, 0.6), CLOSE);
        assertTrue(VehicleMath.grip(1.0, 0.98) < 0.1, "ice holds a tenth as well");
        assertEquals(1.0, VehicleMath.grip(1.0, 0.4), CLOSE, "never better than a dry road");
    }

    @Test
    void steerAngle_turnsLessAsItGoesFaster() {
        assertEquals(35, VehicleMath.steerAngle(1, 0), CLOSE);
        assertTrue(VehicleMath.steerAngle(1, 30) < 15);
        assertEquals(-35, VehicleMath.steerAngle(-2, 0), CLOSE);
    }

    @Test
    void yawRate_isHeldByTheGrip() {
        final double free = VehicleMath.yawRate(5, 20, 3, 100);
        final double held = VehicleMath.yawRate(30, 20, 3, 0.5);

        assertEquals(Math.toDegrees(5 / 3.0 * Math.tan(Math.toRadians(20))), free, CLOSE);
        assertEquals(Math.toDegrees(0.5 * VehicleMath.G / 30), held, CLOSE, "past its grip it turns no tighter");
    }

    @Test
    void slide_diesAwayByTheGrip() {
        assertEquals(2 - VehicleMath.G * 0.05, VehicleMath.slide(2, 1, 0.05), CLOSE);
        assertEquals(0, VehicleMath.slide(0.1, 1, 0.05), CLOSE);
        assertEquals(-2 + 0.1 * VehicleMath.G * 0.05, VehicleMath.slide(-2, 0.1, 0.05), CLOSE);
    }

    @Test
    void resistance_growsWithTheSquareOfTheSpeed() {
        final double slow = VehicleMath.resistance(10, 0.8, 1.2, 1000, false);
        final double fast = VehicleMath.resistance(20, 0.8, 1.2, 1000, false);

        assertEquals(4 * slow, fast, CLOSE);
        assertEquals(-slow, VehicleMath.resistance(-10, 0.8, 1.2, 1000, false), CLOSE);
        assertEquals(0.015 * 1000 * VehicleMath.G, VehicleMath.resistance(1.0e-3, 0, 1.2, 1000, true), 0.01);
    }

    @Test
    void lean_isTheSlopeBetweenTheWheels() {
        assertEquals(45, VehicleMath.lean(2, 2), CLOSE);
        assertEquals(0, VehicleMath.lean(1, 0), CLOSE);
    }

    @Test
    void follow_keepsTheTrailerItsLengthBehindFacingTheHitch() {
        final double[] at = VehicleMath.follow(0, 10, 0, 0, 4);

        assertArrayEquals(new double[] {0, 6, 0}, at, CLOSE);
        final double[] turned = VehicleMath.follow(10, 0, 0, 0, 4);
        assertEquals(6, turned[0], CLOSE);
        assertEquals(-90, turned[2], CLOSE, "facing east, as the game gives yaw");
    }

    @Test
    void shares_giveTheNearSideMostAndAddToOne() {
        final double[] shares = VehicleMath.shares(new double[] {2, 0, -2});

        assertTrue(shares[0] > shares[1] && shares[1] > shares[2]);
        assertEquals(1, shares[0] + shares[1] + shares[2], CLOSE);
        assertEquals(0.1 * shares[0], shares[2], CLOSE);
    }

    @Test
    void enginePitch_risesFromIdleToTop() {
        assertEquals(0.6F, VehicleMath.enginePitch(800, 800, 4500), 1.0e-6F);
        assertEquals(1.8F, VehicleMath.enginePitch(9000, 800, 4500), 1.0e-6F);
        assertTrue(VehicleMath.engineVolume(1) > VehicleMath.engineVolume(0));
    }
}
