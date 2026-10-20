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

import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class RigidBodyTest {

    private static final double STEP = 1.0 / 20;
    private static final double GRAVITY = 9.81;

    @Test
    void advance_fallsAsHalfGravityTimesTimeSquared() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d().scale(1000);
        final Vector3d weight = new Vector3d(0, -GRAVITY * 1000, 0);
        for (int i = 0; i < 20; i++) {
            body.advance(weight, new Vector3d(), 1000, inertia, STEP);
        }
        // Stepped, it falls a little more than the exact half g t^2 after a second, by a step's worth.
        assertEquals(-GRAVITY, body.velocity().y, 1.0e-9);
        assertEquals(-GRAVITY / 2, body.position().y, GRAVITY * STEP);
    }

    @Test
    void advance_turnsByATorqueAgainstItsInertia() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d().scale(2);
        body.advance(new Vector3d(), new Vector3d(0, 4, 0), 1, inertia, 0.5);
        // A torque of 4 against an inertia of 2 for half a second: a spin of 1 rad/s.
        assertEquals(1, body.spin().y, 1.0e-9);
    }

    @Test
    void advance_keepsTheSpinOfAFreeBodyAndTurnsItAQuarterInTime() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d().scale(5);
        body.spin().set(0, Math.PI / 2, 0);
        for (int i = 0; i < 2000; i++) {
            body.advance(new Vector3d(), new Vector3d(), 1, inertia, 1.0 / 2000);
        }
        final Vector3d east = body.orientation().transform(new Vector3d(1, 0, 0));
        assertEquals(Math.PI / 2, body.spin().y, 1.0e-9);
        assertEquals(0, east.x, 1.0e-3);
        assertEquals(-1, east.z, 1.0e-3);
    }

    @Test
    void advance_keepsTheOrientationOfUnitLength() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d(1, 0, 0, 0, 3, 0, 0, 0, 7);
        body.spin().set(1.3, -0.7, 2.1);
        for (int i = 0; i < 400; i++) {
            body.advance(new Vector3d(), new Vector3d(), 1, inertia, STEP);
        }
        final Quaterniond turn = body.orientation();
        assertEquals(1, Math.sqrt(turn.x * turn.x + turn.y * turn.y + turn.z * turn.z + turn.w * turn.w), 1.0e-9);
    }

    @Test
    void advance_keepsTheAngularMomentumOfALopsidedFreeBody() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d(1, 0, 0, 0, 3, 0, 0, 0, 7);
        body.spin().set(0.4, 2.0, 0.1);
        final Vector3d before = body.worldInertia(inertia, new Matrix3d()).transform(body.spin(), new Vector3d());
        for (int i = 0; i < 200; i++) {
            body.advance(new Vector3d(), new Vector3d(), 1, inertia, STEP);
        }
        final Vector3d after = body.worldInertia(inertia, new Matrix3d()).transform(body.spin(), new Vector3d());
        assertEquals(0, after.distance(before), 1.0e-6);
    }

    @Test
    void advance_movesNothingOfNoMass() {
        final RigidBody body = new RigidBody();
        body.advance(new Vector3d(0, -100, 0), new Vector3d(1, 1, 1), 0, new Matrix3d(), STEP);
        assertEquals(0, body.position().length(), 0);
        assertEquals(0, body.spin().length(), 0);
    }

    @Test
    void push_atItsCentreOnlySpeedsItAndOffItTurnsItToo() {
        final RigidBody body = new RigidBody();
        final Matrix3d inertia = new Matrix3d().scale(10);
        body.push(new Vector3d(0, 0, 20), new Vector3d(), 10, inertia);
        assertEquals(2, body.velocity().z, 1.0e-9);
        assertEquals(0, body.spin().length(), 1.0e-9);
        body.push(new Vector3d(0, 0, 10), new Vector3d(1, 0, 0), 10, inertia);
        assertTrue(body.spin().y < 0, "a push east of the centre, southwards, turns it clockwise seen from above");
    }

    @Test
    void velocityAt_addsTheSpinAboutTheCentre() {
        final RigidBody body = new RigidBody();
        body.velocity().set(1, 0, 0);
        body.spin().set(0, 1, 0);
        final Vector3d at = body.velocityAt(new Vector3d(0, 0, 2), new Vector3d());
        assertEquals(1 + 2, at.x, 1.0e-9);
    }
}
