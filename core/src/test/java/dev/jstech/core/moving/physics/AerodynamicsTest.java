/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class AerodynamicsTest {

    private static final double STALL = Math.toRadians(15);
    private static final double MOST = 1.2;

    @Test
    void liftCoefficient_risesStraightToItsMostAtTheStall() {
        assertEquals(0, Aerodynamics.liftCoefficient(0, STALL, MOST), 1.0e-12);
        assertEquals(MOST / 2, Aerodynamics.liftCoefficient(STALL / 2, STALL, MOST), 1.0e-12);
        assertEquals(MOST, Aerodynamics.liftCoefficient(STALL, STALL, MOST), 1.0e-12);
        assertEquals(-MOST, Aerodynamics.liftCoefficient(-STALL, STALL, MOST), 1.0e-12);
    }

    @Test
    void liftCoefficient_dropsPastTheStall() {
        assertTrue(Aerodynamics.liftCoefficient(STALL + 0.01, STALL, MOST) < MOST * 0.6);
    }

    @Test
    void dragCoefficient_growsWithTheLiftAndBroadsideMost() {
        final double bare = Aerodynamics.dragCoefficient(0, STALL, MOST);
        assertTrue(Aerodynamics.dragCoefficient(STALL, STALL, MOST) > bare);
        assertTrue(Aerodynamics.dragCoefficient(Math.PI / 2, STALL, MOST) > 1.5);
    }

    @Test
    void wing_liftsUpWhenFlyingLevelNosePitchedUp() {
        final double pitch = Math.toRadians(5);
        final Vector3d normal = new Vector3d(-Math.sin(pitch), Math.cos(pitch), 0);
        // Flying east at 50 m/s: the air goes west past the wing.
        final Vector3d force = Aerodynamics.wing(new Vector3d(-50, 0, 0), normal, 10, Aerodynamics.SEA_AIR, STALL,
                MOST, new Vector3d());
        final double expected = 0.5 * Aerodynamics.SEA_AIR * 50 * 50 * 10
                * Aerodynamics.liftCoefficient(pitch, STALL, MOST);
        assertEquals(expected, force.y, expected * 1.0e-6);
        assertTrue(force.x < 0, "it drags back, westwards");
    }

    @Test
    void wing_givesNothingInAVacuumOrStillAir() {
        final Vector3d up = new Vector3d(0, 1, 0);
        assertEquals(0, Aerodynamics.wing(new Vector3d(-50, 0, 0), up, 10, 0, STALL, MOST, new Vector3d()).length());
        assertEquals(0, Aerodynamics.wing(new Vector3d(), up, 10, 1.2, STALL, MOST, new Vector3d()).length());
    }

    @Test
    void drag_growsWithTheSquareOfTheSpeed() {
        final double slow = Aerodynamics.drag(10, 1.2, 2, 1);
        assertEquals(4 * slow, Aerodynamics.drag(20, 1.2, 2, 1), 1.0e-9);
        assertEquals(0.5 * 1.2 * 100 * 2, slow, 1.0e-9);
    }

    @Test
    void thrustShare_ofARocketIsAllAnywhereAndOfAJetNoneInAVacuum() {
        assertEquals(1, Aerodynamics.thrustShare(Aerodynamics.ThrustKind.ROCKET, 0, 300, 0), 0);
        assertEquals(0, Aerodynamics.thrustShare(Aerodynamics.ThrustKind.JET, 0, 0, 0), 0);
        assertEquals(0.5, Aerodynamics.thrustShare(Aerodynamics.ThrustKind.JET, Aerodynamics.SEA_AIR / 2, 0, 0),
                1.0e-12);
    }

    @Test
    void thrustShare_ofAPropellerFallsAsTheCraftNearsItsAirsSpeed() {
        final double still = Aerodynamics.thrustShare(Aerodynamics.ThrustKind.PROPELLER, Aerodynamics.SEA_AIR, 0, 40);
        final double half = Aerodynamics.thrustShare(Aerodynamics.ThrustKind.PROPELLER, Aerodynamics.SEA_AIR, 20,
                40);
        assertEquals(1, still, 1.0e-12);
        assertEquals(0.5, half, 1.0e-12);
        assertEquals(0, Aerodynamics.thrustShare(Aerodynamics.ThrustKind.PROPELLER, Aerodynamics.SEA_AIR, 60, 40),
                0);
    }

    @Test
    void heating_ofAReEntryIsByTheSpeedCubedAndTheRootOfTheDensity() {
        final double slow = Aerodynamics.heating(1.0e-4, 1000);
        assertEquals(8 * slow, Aerodynamics.heating(1.0e-4, 2000), slow * 1.0e-9);
        assertEquals(2 * slow, Aerodynamics.heating(4.0e-4, 1000), slow * 1.0e-9);
        assertEquals(0, Aerodynamics.heating(0, 7000), 0);
    }
}
