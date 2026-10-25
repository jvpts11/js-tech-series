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
    /* How far into each other two bodies rest, as the solver lets them be, and how fast they settle that deep. */
    private static final double LETS_BE = 0.005;
    private static final double SETTLES = 0.05;

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
        assertEquals(-(0.4 / STEP + SETTLES), body.velocity().y, 1.0e-3,
                "it closes the gap to touching, settling no faster into the depth a contact lets be, not bouncing yet");
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

    @Test
    void solve_settlesAStackDroppedOnItselfSquarely() {
        final Stack stack = new Stack(2);
        double thrown = 0;
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
            thrown = Math.max(thrown, stack.topSideways());
        }
        // Each lands on the one below just landed and bounces a little, as fast as it came: never thrown aside.
        assertTrue(thrown < 0.3, "the top is never thrown aside: " + thrown);
        stack.assertSquare();
    }

    @Test
    void solve_settlesAStackSetDownTouchingWithoutItsTopFalling() {
        // Each cube touching the one below, as blocks set down from the grid are: no deeper than none.
        final Stack stack = new Stack(1);
        double fell = 0;
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
            fell = Math.max(fell, -stack.topUp());
        }
        // Each joint settles into the depth it lets be no faster than that: the top falls no faster than all of them.
        assertTrue(fell <= (Stack.CUBES + 1) * SETTLES + 0.05, "set down touching, its top barely falls: " + fell
                + " m/s");
        stack.assertSquare();
    }

    @Test
    void solve_pushesAStackSunkIntoItselfApartWithoutThrowingItsTop() {
        final Stack stack = new Stack(1);
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
        }
        // Each sunk five centimetres into the one below, as a step of them closing with nothing holding them would.
        stack.sink(0.05);
        double thrown = 0;
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
            thrown = Math.max(thrown, stack.topUp());
        }
        assertTrue(thrown < 0.5, "pushed out of one another, the top is not thrown up: " + thrown + " m/s");
        stack.assertSquare();
    }

    @Test
    void solve_startsTwoPointsKnownAlikeFromWhatTheyGaveOnce() {
        final RigidBody body = new RigidBody();
        body.position().set(0, 0.5 - LETS_BE, 0);
        final RigidBody ground = new RigidBody();
        ground.fixed();
        final ContactSolver solver = new ContactSolver();
        final Vector3d up = new Vector3d(0, 1, 0);
        double started = 0;
        for (int i = 0; i < 20; i++) {
            body.prepare(MASS, CUBE);
            body.accelerate(new Vector3d(0, -9.81 * MASS, 0), new Vector3d(), MASS, CUBE, STEP);
            solver.clear();
            // The same point twice, known alike, as two corners of a face turned a hair on another may be.
            for (int twice = 0; twice < 2; twice++) {
                solver.add(body, ground, new Vector3d(0, -LETS_BE, 0), up, -LETS_BE, GRIP, 1, 1);
            }
            solver.ready(STEP, ContactSolver.ITERATIONS, 1.0);
            solver.warm();
            started = body.velocity().y;
            solver.iterate(ContactSolver.ITERATIONS);
            solver.end();
            body.drift(CUBE, STEP);
        }
        assertEquals(0, started, 1.0e-6, "started from what both gave, once, it is held as it rests");
    }

    @Test
    void solve_rightsAStackLeaningWithinTheDepthItLetsBe() {
        final Stack stack = new Stack(1);
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
        }
        /*
         * Each cube leaning a quarter of a degree more than the one below, every joint touching at both its edges and
         * no deeper than a contact lets be: as a stack shaken by its landing comes to rest.
         */
        stack.lean(0.25);
        for (int i = 0; i < Stack.SETTLED; i++) {
            stack.step();
        }
        assertTrue(stack.mostLean() < 0.1, "the stack rights itself: its top leans " + stack.mostLean() + " degrees");
        stack.assertSquare();
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

    /*
     * Five cubes one on another on the ground, {@code gap} apart to begin with, worked as an island of structures
     * resting on one another is: a step a quarter of a tick, each pair's points one group, passes more for each cube
     * above.
     */
    private static final class Stack {

        /* Long enough for a stack dropped or pushed to come to rest: three seconds of quarter-tick steps. */
        private static final int SETTLED = 240;
        private static final int CUBES = 5;
        private static final double STEP = 0.0125;
        private static final int MOST_PASSES = 40;
        private static final int PASSES_PER_CUBE = 4;
        private static final long FLOOR_GROUP = 100;
        private static final double LEANING_INTO = 0.0025;

        private final RigidBody[] cubes = new RigidBody[CUBES];
        private final RigidBody ground = new RigidBody();
        private final OrientedBox floor = new OrientedBox().square(-50, -1, -50, 50, 0, 50);
        private final ContactSolver solver = new ContactSolver();
        private final BoxContacts contacts = new BoxContacts();

        private Stack(final double gap) {
            ground.fixed();
            for (int i = 0; i < CUBES; i++) {
                cubes[i] = new RigidBody();
                // A hair aside of one another, as structures landing never quite square do.
                cubes[i].position().set(i % 2 == 0 ? 0.01 : -0.01, 0.5 + gap * i, i % 3 == 0 ? 0.01 : 0);
            }
        }

        private void step() {
            for (final RigidBody cube : cubes) {
                cube.prepare(MASS, CUBE);
                cube.accelerate(new Vector3d(0, -9.81 * MASS, 0), new Vector3d(), MASS, CUBE, STEP);
            }
            solver.clear();
            final RigidBody lowest = cubes[0];
            contacts.collide(box(lowest), floor, lowest.velocity().length() * STEP + 0.05, (at, way, apart,
                    feature) -> solver.add(lowest, ground, at, way, apart, GRIP, key(FLOOR_GROUP, lowest, at, way,
                    apart), FLOOR_GROUP));
            for (int i = 0; i < CUBES - 1; i++) {
                final RigidBody under = cubes[i];
                final RigidBody over = cubes[i + 1];
                final long pair = i;
                contacts.collide(box(over), box(under), (over.velocity().length() + under.velocity().length())
                        * STEP + 0.05, (at, way, apart, feature) -> solver.add(over, under, at, way, apart, GRIP,
                        key(pair, over, at, way, apart), pair));
            }
            final int times = solver.begin(STEP, ContactSolver.ITERATIONS, 1.0);
            solver.iterate(Math.max(times, Math.min(MOST_PASSES, times + PASSES_PER_CUBE * (CUBES - 1))));
            solver.end();
            for (final RigidBody cube : cubes) {
                cube.drift(CUBE, STEP);
            }
        }

        /* Sinks each cube {@code depth} into the one below. */
        private void sink(final double depth) {
            for (int i = 1; i < CUBES; i++) {
                cubes[i].position().y -= depth * i;
            }
        }

        /*
         * Leans each cube, still, {@code degrees} more than the one below, each turned about its own centre and two and
         * a half millimetres into the one below: both edges of each joint within the depth a contact lets be.
         */
        private void lean(final double degrees) {
            for (int i = 0; i < CUBES; i++) {
                cubes[i].orientation().identity().rotateZ(Math.toRadians(degrees * (i + 1)));
                cubes[i].position().set(0, 0.5 - LEANING_INTO + i * (1 - LEANING_INTO), 0);
                cubes[i].velocity().zero();
                cubes[i].spin().zero();
            }
        }

        /* The most any cube leans from upright, in degrees. */
        private double mostLean() {
            double most = 0;
            for (final RigidBody cube : cubes) {
                final Vector3d up = cube.orientation().transform(new Vector3d(0, 1, 0));
                most = Math.max(most, Math.toDegrees(Math.acos(Math.min(1, up.y))));
            }
            return most;
        }

        private double topUp() {
            return cubes[CUBES - 1].velocity().y;
        }

        private double topSideways() {
            return Math.hypot(cubes[CUBES - 1].velocity().x, cubes[CUBES - 1].velocity().z);
        }

        /* Each cube rests on the one below, as deep into it as a contact lets be and a hair aside of it at most. */
        private void assertSquare() {
            for (int i = 0; i < CUBES; i++) {
                final Vector3d at = cubes[i].position();
                final double below = i == 0 ? -0.5 : cubes[i - 1].position().y;
                assertEquals(below + 1, at.y, 0.01, "cube " + i + " rests on the one below: " + at);
                assertTrue(Math.abs(at.x) < 0.1 && Math.abs(at.z) < 0.1, "cube " + i + " squarely: " + at);
            }
        }

        /* A point known by where it is on {@code body}'s own face, as the structures' contacts are. */
        private static long key(final long pair, final RigidBody body, final Vector3d at, final Vector3d way,
                                final double apart) {
            final Vector3d local = new Vector3d(way).mul(apart / 2).add(at).sub(body.position());
            body.orientation().transformInverse(local);
            return ContactSolver.key(pair, local);
        }
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
