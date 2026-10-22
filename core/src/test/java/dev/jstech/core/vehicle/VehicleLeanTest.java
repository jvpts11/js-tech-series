/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class VehicleLeanTest {

    private static final double EPSILON = 1.0e-6;

    @Test
    void seat_standingLevelIsTurnedByTheHeadingAlone() {
        // Heading south (yaw 0) ahead is +Z and left is +X, as the game's entities face.
        assertClose(new Vector3d(0.5, 1.0, 2.0), VehicleLean.seat(0.5, 1.0, 2.0, 0.0F, 0.0F, 0.0F));
        // Heading west (yaw 90) ahead is -X and left is +Z.
        assertClose(new Vector3d(-2.0, 1.0, 0.5), VehicleLean.seat(0.5, 1.0, 2.0, 90.0F, 0.0F, 0.0F));
    }

    @Test
    void seat_aheadDropsAsTheNoseLeansDown() {
        final Vector3d nose = VehicleLean.seat(0.0, 0.0, 2.0, 0.0F, -30.0F, 0.0F);
        assertEquals(-1.0, nose.y, EPSILON, "two blocks ahead, the nose down a third of a right angle, a block down");
        assertEquals(Math.sqrt(3.0), nose.z, EPSILON);
    }

    @Test
    void seat_aboveSwingsToTheSideTheVehicleLeans() {
        final Vector3d left = VehicleLean.seat(0.0, 1.0, 0.0, 0.0F, 0.0F, 30.0F);
        final Vector3d right = VehicleLean.seat(0.0, 1.0, 0.0, 0.0F, 0.0F, -30.0F);
        assertEquals(-left.x, right.x, EPSILON, "a lean either way swings a seat above as far either way");
        assertEquals(Math.cos(Math.toRadians(30.0)), left.y, EPSILON);
        assertEquals(0.5, Math.abs(left.x), EPSILON);
    }

    @Test
    void tilt_turnsWhatSitsAsTheSeatTurns() {
        // A rider's head a block above where they sit goes where a seat a block up would go from the vehicle's floor.
        final float yaw = 37.0F;
        final float pitch = 12.0F;
        final float roll = -25.0F;
        final Vector3d head = VehicleLean.tilt(yaw, pitch, roll).transform(new Vector3d(0, 1, 0));
        final Vector3d seat = VehicleLean.seat(0, 1, 0, yaw, pitch, roll);
        assertClose(seat, head);
        assertClose(new Vector3d(0, 1, 0), VehicleLean.tilt(yaw, 0.0F, 0.0F).transform(new Vector3d(0, 1, 0)));
    }

    @Test
    void leans_onlyWhenPitchedOrRolled() {
        assertFalse(VehicleLean.leans(0.0F, 0.0F));
        assertTrue(VehicleLean.leans(0.0F, 4.0F));
        assertTrue(VehicleLean.leans(-2.0F, 0.0F));
    }

    private static void assertClose(final Vector3d expected, final Vector3d actual) {
        assertEquals(expected.x, actual.x, EPSILON, () -> "x of " + actual);
        assertEquals(expected.y, actual.y, EPSILON, () -> "y of " + actual);
        assertEquals(expected.z, actual.z, EPSILON, () -> "z of " + actual);
    }
}
