/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import java.util.EnumMap;
import java.util.Map;
import org.joml.Vector3d;

/**
 * The forces on a body in one step, summed: their sum, pushing its centre, and the sum of their turning about its
 * centre, each force where it acts; and what each kind gave, and where it acted on the whole, for whoever reads them.
 * In newtons, the world's axes.
 */
public final class ForceSum {

    private final Vector3d force = new Vector3d();
    private final Vector3d torque = new Vector3d();
    private final Vector3d centre = new Vector3d();
    private final Vector3d arm = new Vector3d();
    private final Map<ForceKind, Vector3d> byKind = new EnumMap<>(ForceKind.class);
    /* For each kind, where its forces acted, each weighed by how strong it was: x, y, z summed, and the weights. */
    private final Map<ForceKind, double[]> acting = new EnumMap<>(ForceKind.class);

    public ForceSum() {
        for (final ForceKind kind : ForceKind.values()) {
            byKind.put(kind, new Vector3d());
            acting.put(kind, new double[4]);
        }
    }

    /** Starts a new step about the body's centre at {@code at}, the world's place. */
    public void clear(final Vector3d at) {
        force.set(0, 0, 0);
        torque.set(0, 0, 0);
        centre.set(at);
        for (final Vector3d each : byKind.values()) {
            each.set(0, 0, 0);
        }
        for (final double[] each : acting.values()) {
            each[0] = 0;
            each[1] = 0;
            each[2] = 0;
            each[3] = 0;
        }
    }

    /** Adds {@code push}, of {@code kind}, acting at the world's place {@code at}: it pushes and turns. */
    public void add(final ForceKind kind, final Vector3d push, final Vector3d at) {
        force.add(push);
        arm.set(at).sub(centre);
        torque.add(arm.cross(push));
        byKind.get(kind).add(push);
        acts(kind, push.length(), at.x, at.y, at.z);
    }

    /** Adds {@code push}, of {@code kind}, acting at the centre: it only pushes. */
    public void addAtCentre(final ForceKind kind, final Vector3d push) {
        force.add(push);
        byKind.get(kind).add(push);
        acts(kind, push.length(), centre.x, centre.y, centre.z);
    }

    /** Notes {@code push} as given by {@code kind}, for whoever reads them, without pushing: one worked as blows. */
    public void note(final ForceKind kind, final Vector3d push) {
        byKind.get(kind).add(push);
        acts(kind, push.length(), centre.x, centre.y, centre.z);
    }

    /**
     * Where the forces of {@code kind} acted on the whole, the world's place, into {@code into}: each force's place,
     * weighed by how strong it was; the centre for a kind that gave nothing.
     */
    public Vector3d where(final ForceKind kind, final Vector3d into) {
        final double[] sums = acting.get(kind);
        if (sums[3] <= 0) {
            return into.set(centre);
        }
        return into.set(sums[0] / sums[3], sums[1] / sums[3], sums[2] / sums[3]);
    }

    /** The body's centre this step was worked about, the world's place. */
    public Vector3d centre() {
        return centre;
    }

    /** Adds a turning of its own, {@code turning}, in newton metres. */
    public void addTurning(final Vector3d turning) {
        torque.add(turning);
    }

    /** The sum of the forces. */
    public Vector3d force() {
        return force;
    }

    /** The sum of their turning about the centre. */
    public Vector3d torque() {
        return torque;
    }

    /** What the forces of {@code kind} gave, summed. */
    public Vector3d of(final ForceKind kind) {
        return byKind.get(kind);
    }

    private void acts(final ForceKind kind, final double weight, final double x, final double y, final double z) {
        if (weight <= 0) {
            return;
        }
        final double[] sums = acting.get(kind);
        sums[0] += x * weight;
        sums[1] += y * weight;
        sums[2] += z * weight;
        sums[3] += weight;
    }

    /** The kinds of force on a structure. */
    public enum ForceKind {
        GRAVITY,
        THRUST,
        LIFT,
        DRAG,
        BUOYANCY,
        LEVITATION,
        MAGNETISM,
        WHEELS,
        BALLOONS,
        /** Held from outside: the physics tool's grip, or a mod's. */
        HELD
    }
}
