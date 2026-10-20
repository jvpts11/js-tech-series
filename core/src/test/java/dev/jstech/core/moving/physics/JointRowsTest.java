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

import java.util.List;
import java.util.function.Consumer;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class JointRowsTest {

    private static final double STEP = 0.05;
    private static final double MASS = 100;
    private static final Matrix3d CUBE = new Matrix3d().scaling(MASS / 6);
    private static final Vector3d GRAVITY = new Vector3d(0, -9.81, 0);
    private static final int ITERATIONS = 10;
    private static final int ROUNDS = 3;

    @Test
    void distance_holdsARopePendulumAtItsLengthAsItSwings() {
        final RigidBody bob = body(2, 0, 0);
        final RigidBody ground = ground();
        final double[] lowest = {0};
        final double[] longest = {0};
        run(List.of(bob), rows -> new JointRows(rows).distance(bob, bob.position(), ground, new Vector3d(), 2,
                JointRows.Distance.AT_MOST, 0, 0, STEP, 1), 60, () -> {
                    lowest[0] = Math.min(lowest[0], bob.position().y);
                    longest[0] = Math.max(longest[0], bob.position().length());
                });
        assertTrue(longest[0] < 2.01, "the rope holds it at its length: " + longest[0]);
        assertTrue(lowest[0] < -1.95, "it swings down under its pivot: " + lowest[0]);
    }

    @Test
    void distance_swingsARopePendulumBackUpTheOtherSide() {
        final RigidBody bob = body(2, 0, 0);
        final RigidBody ground = ground();
        final double[] furthest = {0};
        run(List.of(bob), rows -> new JointRows(rows).distance(bob, bob.position(), ground, new Vector3d(), 2,
                JointRows.Distance.AT_MOST, 0, 0, STEP, 1), 28, () -> furthest[0] = Math.min(furthest[0],
                bob.position().x));
        assertTrue(furthest[0] < -1.7, "it keeps its swing, losing little: " + furthest[0]);
    }

    @Test
    void distance_letsARopeGoSlackAndHoldsOnlyWhenTaut() {
        final RigidBody bob = body(0, -1, 0);
        final RigidBody ground = ground();
        bob.velocity().set(0, 3, 0);
        final Vector3d pivot = new Vector3d();
        run(List.of(bob), rows -> new JointRows(rows).distance(bob, bob.position(), ground, pivot, 2,
                JointRows.Distance.AT_MOST, 0, 0, STEP, 1), 3, () -> { });
        assertTrue(bob.position().y > -0.9, "thrown up, the slack rope does not hold it back: " + bob.position());
    }

    @Test
    void distance_hangsASpringStretchedByTheWeightOnIt() {
        final RigidBody bob = body(0, -2, 0);
        final RigidBody ground = ground();
        final double stiffness = 20_000;
        run(List.of(bob), rows -> new JointRows(rows).distance(bob, bob.position(), ground, new Vector3d(), 2,
                JointRows.Distance.SPRING, stiffness, 4_000, STEP, 1), 200, () -> { });
        assertEquals(-(2 + MASS * 9.81 / stiffness), bob.position().y, 0.01);
    }

    @Test
    void point_keepsARigidPendulumOnItsPivot() {
        final RigidBody bar = body(1, 0, 0);
        final RigidBody ground = ground();
        final double[] furthest = {0};
        run(List.of(bar), rows -> new JointRows(rows).point(bar, end(bar, -1, 0, 0), ground, new Vector3d(), 1),
                60, () -> furthest[0] = Math.max(furthest[0], end(bar, -1, 0, 0).length()));
        assertTrue(furthest[0] < 0.01, "its end stays on the pivot: " + furthest[0]);
        assertTrue(bar.position().y < -0.5, "it swung down: " + bar.position());
    }

    @Test
    void align_letsAHingedBodyTurnAboutItsAxisOnly() {
        final RigidBody door = body(1, 0, 0);
        final RigidBody ground = ground();
        door.spin().set(1.5, 0, 0);
        final double[] leaning = {1};
        run(List.of(door), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.point(door, end(door, -1, 0, 0), ground, new Vector3d(), 1);
            joint.align(door, door.orientation().transform(new Vector3d(0, 0, 1)), ground, new Vector3d(0, 0, 1),
                    2);
        }, 80, () -> leaning[0] = Math.min(leaning[0], door.orientation().transform(new Vector3d(0, 0, 1)).z));
        assertTrue(leaning[0] > 0.99, "its axis stays the hinge's: " + leaning[0]);
        assertTrue(door.position().y < -0.5, "it swings down about the hinge: " + door.position());
    }

    @Test
    void lock_holdsTwoWeldedBodiesTurnedAsTheyWere() {
        final RigidBody first = body(0, 0, 0);
        final RigidBody second = body(1, 0, 0);
        first.spin().set(0, 2, 0);
        runFree(List.of(first, second), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.point(first, end(first, 0.5, 0, 0), second, end(second, -0.5, 0, 0), 1);
            joint.lock(first, new Quaterniond(first.orientation()), second, new Quaterniond(second.orientation()),
                    2);
        }, 40);
        final Quaterniond between = new Quaterniond(second.orientation()).mul(
                new Quaterniond(first.orientation()).conjugate());
        assertTrue(Math.abs(between.w) > 0.999, "turned as one: " + between);
        assertTrue(Math.abs(first.orientation().w) < 0.99, "and they turned: " + first.orientation());
        assertTrue(end(first, 0.5, 0, 0).distance(end(second, -0.5, 0, 0)) < 0.01, "held together");
    }

    @Test
    void point_keepsTheMomentumOfTwoFreeBodies() {
        final RigidBody first = body(0, 0, 0);
        final RigidBody second = body(1.2, 0, 0);
        first.velocity().set(0, 3, 0);
        second.velocity().set(1, 0, 0);
        final Vector3d before = new Vector3d(first.velocity()).add(second.velocity()).mul(MASS);
        runFree(List.of(first, second), rows -> new JointRows(rows).point(first, first.position(), second,
                second.position(), 1), 1);
        final Vector3d after = new Vector3d(first.velocity()).add(second.velocity()).mul(MASS);
        assertEquals(before.x, after.x, 1.0e-6);
        assertEquals(before.y, after.y, 1.0e-6);
    }

    @Test
    void slide_drivesABodyAlongItsLineAtTheMotorsSpeed() {
        final RigidBody carriage = body(0, 0, 0);
        final RigidBody ground = ground();
        final Vector3d axis = new Vector3d(1, 0, 0);
        run(List.of(carriage), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.onLine(ground, new Vector3d(), axis, carriage, carriage.position(), 1);
            joint.lock(ground, new Quaterniond(), carriage, new Quaterniond(carriage.orientation()), 2);
            joint.slide(ground, new Vector3d(), axis, carriage, carriage.position(), Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, 1.5, 100_000, STEP, 3);
        }, 20, () -> { });
        assertEquals(1.5, carriage.velocity().x, 0.01);
        assertEquals(0, carriage.position().y, 0.01);
    }

    @Test
    void slide_stopsABodyAtTheLimitOfItsLine() {
        final RigidBody carriage = body(0, 0, 0);
        final RigidBody ground = ground();
        final Vector3d axis = new Vector3d(0, 1, 0);
        run(List.of(carriage), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.onLine(ground, new Vector3d(), axis, carriage, carriage.position(), 1);
            joint.slide(ground, new Vector3d(), axis, carriage, carriage.position(), -1.5, 3, 0, 0, STEP, 3);
        }, 60, () -> { });
        assertEquals(-1.5, carriage.position().y, 0.01);
    }

    @Test
    void turn_drivesAHingedWheelAtTheMotorsSpeed() {
        final RigidBody wheel = body(0, 0, 0);
        final RigidBody ground = ground();
        final Vector3d axis = new Vector3d(0, 1, 0);
        run(List.of(wheel), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.point(wheel, wheel.position(), ground, new Vector3d(), 1);
            joint.align(wheel, wheel.orientation().transform(new Vector3d(0, 1, 0)), ground, axis, 2);
            joint.turn(ground, wheel, axis, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 2, 50_000,
                    STEP, 3);
        }, 20, () -> { });
        assertEquals(2, wheel.spin().y, 0.01);
    }

    @Test
    void turn_stopsAHingedDoorAtItsLimit() {
        final RigidBody door = body(1, 0, 0);
        final RigidBody ground = ground();
        final Vector3d axis = new Vector3d(0, 0, 1);
        run(List.of(door), rows -> {
            final JointRows joint = new JointRows(rows);
            joint.point(door, end(door, -1, 0, 0), ground, new Vector3d(), 1);
            joint.align(door, door.orientation().transform(new Vector3d(0, 0, 1)), ground, axis, 2);
            final double angle = JointRows.angle(axis, new Vector3d(1, 0, 0),
                    door.orientation().transform(new Vector3d(1, 0, 0)));
            joint.turn(ground, door, axis, angle, Math.toRadians(-30), Math.toRadians(30), 0, 0, STEP, 3);
        }, 80, () -> { });
        final Vector3d ref = door.orientation().transform(new Vector3d(1, 0, 0));
        assertEquals(-30, Math.toDegrees(JointRows.angle(axis, new Vector3d(1, 0, 0), ref)), 1.0);
    }

    @Test
    void angle_isMeasuredAboutTheAxisFromTheFirstWayToTheSecond() {
        final Vector3d axis = new Vector3d(0, 0, 1);
        assertEquals(Math.PI / 2, JointRows.angle(axis, new Vector3d(1, 0, 0), new Vector3d(0, 1, 0)), 1.0e-9);
        assertEquals(-Math.PI / 2, JointRows.angle(axis, new Vector3d(0, 1, 0), new Vector3d(1, 0, 0)), 1.0e-9);
    }

    /* A cube of MASS at {@code (x, y, z)}. */
    private static RigidBody body(final double x, final double y, final double z) {
        final RigidBody body = new RigidBody();
        body.position().set(x, y, z);
        return body;
    }

    private static RigidBody ground() {
        final RigidBody ground = new RigidBody();
        ground.fixed();
        return ground;
    }

    /* The world's place of the body's own place {@code (x, y, z)} from its centre, as it is turned now. */
    private static Vector3d end(final RigidBody body, final double x, final double y, final double z) {
        return body.orientation().transform(new Vector3d(x, y, z)).add(body.position());
    }

    /* Steps the bodies under gravity, their joints made each step by {@code joints}, {@code after} each step. */
    private static void run(final List<RigidBody> bodies, final Consumer<ConstraintRows> joints, final int steps,
                            final Runnable after) {
        step(bodies, joints, steps, after, GRAVITY);
    }

    /* Steps the bodies with no gravity. */
    private static void runFree(final List<RigidBody> bodies, final Consumer<ConstraintRows> joints,
                                final int steps) {
        step(bodies, joints, steps, () -> { }, new Vector3d());
    }

    /* As a structure's step: pushed, held by the rows, moved, then drawn back together where they stand. */
    private static void step(final List<RigidBody> bodies, final Consumer<ConstraintRows> joints, final int steps,
                             final Runnable after, final Vector3d gravity) {
        final ConstraintRows rows = new ConstraintRows();
        for (int i = 0; i < steps; i++) {
            for (final RigidBody body : bodies) {
                body.prepare(MASS, CUBE);
                body.accelerate(new Vector3d(gravity).mul(MASS), new Vector3d(), MASS, CUBE, STEP);
            }
            rows.clear();
            joints.accept(rows);
            rows.begin();
            rows.iterate(ITERATIONS);
            rows.end();
            for (final RigidBody body : bodies) {
                body.drift(CUBE, STEP);
                body.prepare(MASS, CUBE);
            }
            rows.clear();
            joints.accept(rows);
            rows.project(ROUNDS);
            after.run();
        }
    }
}
