/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * A rigid body as it moves: where its centre of mass is, how it is turned, and how fast it goes and turns, in metres,
 * radians and seconds, the world's axes. Each step it is pushed by the sum of the forces on it and turned by the sum
 * of their turning about its centre: its centre goes as its mass is pushed, and it turns as its spin is kept against
 * its inertia, turned as the body is, so a body of any shape turns as a real one does, over any axis, upside down.
 */
public final class RigidBody {

    private final Vector3d position = new Vector3d();
    private final Quaterniond orientation = new Quaterniond();
    private final Vector3d velocity = new Vector3d();
    private final Vector3d spin = new Vector3d();
    /* Worked on each step. */
    private final Matrix3d turn = new Matrix3d();
    private final Matrix3d worldInertia = new Matrix3d();
    private final Matrix3d scratch = new Matrix3d();
    private final Vector3d momentum = new Vector3d();
    private final Vector3d arm = new Vector3d();
    /* What a push does to it, as {@link #prepare prepared} for a step: none for a body that does not give. */
    private final Matrix3d inverseInertia = new Matrix3d().zero();
    private double inverseMass;

    /* The rate the orientation turns is half the spin, as a quaternion's rate is. */
    private static final double HALF = 0.5;

    /** Where its centre of mass is, in the world. */
    public Vector3d position() {
        return position;
    }

    /** How it is turned from its own frame to the world's. */
    public Quaterniond orientation() {
        return orientation;
    }

    /** How fast its centre goes, in metres a second. */
    public Vector3d velocity() {
        return velocity;
    }

    /** How fast it turns: an axis of the world, of a length its rate in radians a second. */
    public Vector3d spin() {
        return spin;
    }

    /**
     * One step of {@code seconds}: pushed by {@code force} at its centre and turned by {@code torque} about it, both
     * in newtons and the world's axes, a body of {@code kilograms} with {@code inertia} about its centre in its own
     * frame. A body of no mass does not move.
     */
    public void advance(final Vector3d force, final Vector3d torque, final double kilograms, final Matrix3d inertia,
                        final double seconds) {
        if (kilograms <= 0) {
            return;
        }
        accelerate(force, torque, kilograms, inertia, seconds);
        drift(inertia, seconds);
    }

    /**
     * The first half of a step: its speed and spin change by {@code force} and {@code torque} over {@code seconds},
     * where it stands; what holds it, its contacts, may change them again before it {@link #drift drifts}.
     */
    public void accelerate(final Vector3d force, final Vector3d torque, final double kilograms,
                           final Matrix3d inertia, final double seconds) {
        if (kilograms <= 0) {
            return;
        }
        velocity.fma(seconds / kilograms, force);
        worldInertia(inertia, worldInertia);
        worldInertia.invert(scratch).transform(torque, momentum);
        spin.fma(seconds, momentum);
    }

    /**
     * The second half of a step: it goes and turns over {@code seconds} as fast as it now goes and turns, its spin
     * kept as momentum against its inertia turned as it turns, as a free body's is.
     */
    public void drift(final Matrix3d inertia, final double seconds) {
        position.fma(seconds, velocity);
        worldInertia(inertia, worldInertia);
        worldInertia.transform(spin, momentum);
        turnBy(seconds);
        worldInertia(inertia, worldInertia);
        worldInertia.invert(scratch).transform(momentum, spin);
    }

    /** The velocity of its point at {@code at} in the world, its centre's and its spin's, into {@code into}. */
    public Vector3d velocityAt(final Vector3d at, final Vector3d into) {
        into.set(at).sub(position);
        return spin.cross(into, into).add(velocity);
    }

    /**
     * Readies it for the pushes of a step, as it is turned now: a body of {@code kilograms} with {@code inertia} about
     * its centre in its own frame. A body of no mass is readied as one that does not give.
     */
    public void prepare(final double kilograms, final Matrix3d inertia) {
        if (kilograms <= 0) {
            fixed();
            return;
        }
        inverseMass = 1.0 / kilograms;
        worldInertia(inertia, worldInertia).invert(inverseInertia);
    }

    /** Readies it as a body that does not give, the ground or a structure moved by a rule: pushes leave it be. */
    public void fixed() {
        inverseMass = 0;
        inverseInertia.zero();
    }

    /** One over its mass, as readied: 0 for a body that does not give. */
    public double inverseMass() {
        return inverseMass;
    }

    /** The inverse of its inertia in the world's axes, as readied. */
    public Matrix3d inverseInertia() {
        return inverseInertia;
    }

    /**
     * Gives it a push of {@code impulse}, in newton seconds, at the world's point {@code at}, as readied for the step:
     * cheap, for the many small pushes of its contacts.
     */
    public void kick(final Vector3d impulse, final Vector3d at) {
        if (inverseMass <= 0) {
            return;
        }
        velocity.fma(inverseMass, impulse);
        arm.set(at).sub(position).cross(impulse);
        inverseInertia.transform(arm);
        spin.add(arm);
    }

    /** Gives it a push of {@code size} newton seconds along {@code way} at the world's point {@code at}. */
    public void kick(final Vector3d way, final double size, final Vector3d at) {
        if (inverseMass <= 0) {
            return;
        }
        velocity.fma(inverseMass * size, way);
        arm.set(at).sub(position).cross(way).mul(size);
        inverseInertia.transform(arm);
        spin.add(arm);
    }

    /**
     * How much a push along {@code way} at the world's point {@code at} gives against, as readied: one over the
     * mass a push there meets, its own and its turning's.
     */
    public double give(final Vector3d way, final Vector3d at) {
        if (inverseMass <= 0) {
            return 0;
        }
        arm.set(at).sub(position);
        final Vector3d turning = new Vector3d(arm).cross(way);
        inverseInertia.transform(turning);
        return inverseMass + turning.cross(arm).dot(way);
    }

    /** Its inertia about its centre in the world's axes, as it is turned now, into {@code into}. */
    public Matrix3d worldInertia(final Matrix3d own, final Matrix3d into) {
        orientation.get(turn);
        // R I R^T
        return into.set(turn).mul(own).mul(scratch.set(turn).transpose());
    }

    /**
     * Gives it a push of {@code impulse}, in newton seconds, at the world's point {@code at}: its speed and its spin
     * change at once, as at a blow.
     */
    public void push(final Vector3d impulse, final Vector3d at, final double kilograms, final Matrix3d inertia) {
        if (kilograms <= 0) {
            return;
        }
        velocity.fma(1.0 / kilograms, impulse);
        final Vector3d arm = new Vector3d(at).sub(position);
        final Vector3d angular = arm.cross(impulse, new Vector3d());
        worldInertia(inertia, worldInertia);
        worldInertia.transform(spin, momentum).add(angular);
        worldInertia.invert(scratch).transform(momentum, spin);
    }

    /* Turns the orientation by the spin over {@code seconds}, kept of unit length. */
    private void turnBy(final double seconds) {
        final double half = HALF * seconds;
        final Quaterniond rate = new Quaterniond(spin.x * half, spin.y * half, spin.z * half, 0).mul(orientation);
        orientation.set(orientation.x + rate.x, orientation.y + rate.y, orientation.z + rate.z,
                orientation.w + rate.w).normalize();
    }
}
