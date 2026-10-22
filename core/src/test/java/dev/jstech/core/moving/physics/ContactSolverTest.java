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
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ContactSolverTest {

    private static final double STEP = 0.05;
    private static final double MASS = 100;
    private static final Matrix3d CUBE = new Matrix3d().scaling(MASS / 6);
    private static final double GRIP = 0.6;

    @Test
    void solve_stopsABodyFallingOntoTheGround() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 3, 0);
        run(body, new Vector3d(0, -9.81, 0), 0.6, 80);
        assertEquals(0.5, body.position().y, 0.02);
        assertTrue(body.velocity().length() < 0.05, "it rests: " + body.velocity());
    }

    @Test
    void solve_neverLetsAFastBodyThroughTheGround() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 6, 0);
        body.velocity().set(0, -90, 0);
        run(body, new Vector3d(0, -9.81, 0), 0.6, 40);
        assertTrue(body.position().y > 0.4, "it stopped on top: " + body.position());
    }

    @Test
    void solve_holdsABodyOnASlopeItsGripHolds() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.5, 0);
        final double slope = Math.toRadians(20);
        run(body, new Vector3d(9.81 * Math.sin(slope), -9.81 * Math.cos(slope), 0), 0.6, 60);
        assertEquals(0, body.position().x, 0.05);
    }

    @Test
    void solve_letsABodySlideDownASlopeTooSteepForItsGrip() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.5, 0);
        final double slope = Math.toRadians(20);
        run(body, new Vector3d(9.81 * Math.sin(slope), -9.81 * Math.cos(slope), 0), 0.1, 40);
        assertTrue(body.position().x > 1, "it slid: " + body.position());
    }

    @Test
    void solve_pushesTwoFreeBodiesApartKeepingTheirMomentum() {
        final RigidBody first = new RigidBody();
        final RigidBody second = new RigidBody();
        first.position().set(0, 0.9, 0);
        first.velocity().set(0, -2, 0);
        second.position().set(0, 0, 0);
        second.velocity().set(0, 1, 0);
        first.prepare(MASS, CUBE);
        second.prepare(MASS * 3, new Matrix3d().scaling(MASS * 3 / 6));
        final Vector3d before = new Vector3d(first.velocity()).mul(MASS).fma(MASS * 3, second.velocity());
        final ContactSolver solver = new ContactSolver();
        new BoxContacts().collide(box(first), box(second), 0.1,
                (at, way, apart, feature) -> solver.add(first, second, at, way, apart, 0.5, key(first, at)));
        solver.solve(STEP, ContactSolver.ITERATIONS);
        final Vector3d after = new Vector3d(first.velocity()).mul(MASS).fma(MASS * 3, second.velocity());
        assertEquals(before.y, after.y, 1.0e-6);
        assertTrue(first.velocity().y >= second.velocity().y - 1.0e-6, "they no longer close");
    }

    @Test
    void solve_bouncesAFastBodyOffWhatItMeetsNotBeforeIt() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.9, 0);
        body.velocity().set(0, -20, 0);
        final Landing landing = new Landing();
        landing.step(body);
        assertEquals(-0.4 / STEP, body.velocity().y, 1.0e-3, "it closes the gap and no more, not bouncing yet");
        body.drift(CUBE, STEP);
        landing.step(body);
        assertTrue(body.velocity().y > 0.15 * 20, "touching, it bounces as fast as it came: " + body.velocity());
    }

    @Test
    void solve_landsAFaceALittleTiltedWithoutSettingItTurning() {
        final RigidBody body = new RigidBody();
        body.orientation().rotateZ(0.012);
        body.position().set(0, 0.8, 0);
        body.velocity().set(0, -6, 0);
        final Landing landing = new Landing();
        for (int i = 0; i < 2; i++) {
            landing.step(body);
            body.drift(CUBE, STEP);
        }
        assertTrue(body.velocity().y > 0.5, "it bounced: " + body.velocity());
        assertTrue(body.spin().length() < 1, "and barely turns: " + body.spin());
    }

    @Test
    void solve_bouncesNoContactHeldFromStepToStep() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.5, 0);
        final Landing landing = new Landing();
        for (int i = 0; i < 20; i++) {
            landing.fall(body);
        }
        body.velocity().y = -3;
        landing.step(body);
        assertTrue(body.velocity().y < 0.05, "a body resting there and pushed down does not bounce: "
                + body.velocity());
    }

    @Test
    void solve_keepsItsGripTheSameWayWhileItsWayLeansByAHair() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.5, 0);
        final RigidBody ground = new RigidBody();
        ground.fixed();
        final ContactSolver solver = new ContactSolver();
        final Vector3d push = new Vector3d(0.3 * 9.81 * MASS, -9.81 * MASS, 0);
        for (int i = 0; i < 20; i++) {
            body.prepare(MASS, CUBE);
            body.accelerate(push, new Vector3d(), MASS, CUBE, STEP);
            solver.clear();
            // The way up leans a hair east, then a hair south, by turns: as a body's face meets the ground's.
            final Vector3d up = i % 2 == 0 ? new Vector3d(1.0e-4, 1, 0) : new Vector3d(0, 1, 1.0e-4);
            for (int corner = 0; corner < 4; corner++) {
                final Vector3d at = new Vector3d(corner % 2 == 0 ? -0.5 : 0.5, 0, corner < 2 ? -0.5 : 0.5);
                solver.add(body, ground, at, up, 0, 0.6, corner);
            }
            solver.solve(STEP, ContactSolver.ITERATIONS);
            body.drift(CUBE, STEP);
        }
        assertEquals(0, body.velocity().z, 0.01, "its grip, carried over, never pushes it aside");
        assertEquals(0, body.velocity().x, 0.01, "and holds it against the push");
    }

    /* Steps a falling cube against the ground, a wide fixed box whose top is at nought, as the world's is held. */
    private static void run(final RigidBody body, final Vector3d gravity, final double grip, final int steps) {
        final RigidBody ground = new RigidBody();
        ground.fixed();
        final OrientedBox floor = new OrientedBox().square(-50, -1, -50, 50, 0, 50);
        final ContactSolver solver = new ContactSolver();
        final BoxContacts contacts = new BoxContacts();
        final Vector3d force = new Vector3d();
        for (int i = 0; i < steps; i++) {
            body.prepare(MASS, CUBE);
            body.accelerate(force.set(gravity).mul(MASS), new Vector3d(), MASS, CUBE, STEP);
            solver.clear();
            final double margin = body.velocity().length() * STEP + 0.05;
            contacts.collide(box(body), floor, margin,
                    (at, way, apart, feature) -> solver.add(body, ground, at, way, apart, grip, key(body, at)));
            solver.solve(STEP, ContactSolver.ITERATIONS);
            body.drift(CUBE, STEP);
        }
    }

    private static long key(final RigidBody body, final Vector3d at) {
        final Vector3d local = new Vector3d(at).sub(body.position());
        body.orientation().transformInverse(local);
        return ContactSolver.key(1, local);
    }

    private static OrientedBox box(final RigidBody body) {
        final Vector3d x = body.orientation().transform(new Vector3d(1, 0, 0));
        final Vector3d y = body.orientation().transform(new Vector3d(0, 1, 0));
        final Vector3d z = body.orientation().transform(new Vector3d(0, 0, 1));
        return new OrientedBox().set(body.position(), x, y, z, 0.5, 0.5, 0.5);
    }

    /* A cube's meetings with the ground, a wide fixed box whose top is at nought, worked step after step. */
    private static final class Landing {

        private final RigidBody ground = new RigidBody();
        private final OrientedBox floor = new OrientedBox().square(-50, -1, -50, 50, 0, 50);
        private final ContactSolver solver = new ContactSolver();
        private final BoxContacts contacts = new BoxContacts();
        private long group;

        private Landing() {
            ground.fixed();
        }

        /* Works the cube's meetings with the ground over a step, the points of each step's face as one group. */
        private void step(final RigidBody body) {
            body.prepare(MASS, CUBE);
            solver.clear();
            final double margin = body.velocity().length() * STEP + 0.05;
            final long face = group++;
            // Each point is known by where it is on the cube's own face, as the structures' contacts are.
            contacts.collide(box(body), floor, margin, (at, way, apart, feature) -> solver.add(body, ground, at,
                    way, apart, GRIP, key(body, new Vector3d(way).mul(apart / 2).add(at)), face));
            solver.solve(STEP, ContactSolver.ITERATIONS);
        }

        /* A step of the cube falling onto the ground, and drifting. */
        private void fall(final RigidBody body) {
            body.prepare(MASS, CUBE);
            body.accelerate(new Vector3d(0, -9.81 * MASS, 0), new Vector3d(), MASS, CUBE, STEP);
            step(body);
            body.drift(CUBE, STEP);
        }
    }
}
