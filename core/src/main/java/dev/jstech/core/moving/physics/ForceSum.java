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
 * centre, each force where it acts; and what each kind gave, for whoever reads them. In newtons, the world's axes.
 */
public final class ForceSum {

    private final Vector3d force = new Vector3d();
    private final Vector3d torque = new Vector3d();
    private final Vector3d centre = new Vector3d();
    private final Vector3d arm = new Vector3d();
    private final Map<ForceKind, Vector3d> byKind = new EnumMap<>(ForceKind.class);

    public ForceSum() {
        for (final ForceKind kind : ForceKind.values()) {
            byKind.put(kind, new Vector3d());
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
    }

    /** Adds {@code push}, of {@code kind}, acting at the world's place {@code at}: it pushes and turns. */
    public void add(final ForceKind kind, final Vector3d push, final Vector3d at) {
        force.add(push);
        arm.set(at).sub(centre);
        torque.add(arm.cross(push));
        byKind.get(kind).add(push);
    }

    /** Adds {@code push}, of {@code kind}, acting at the centre: it only pushes. */
    public void addAtCentre(final ForceKind kind, final Vector3d push) {
        force.add(push);
        byKind.get(kind).add(push);
    }

    /** Notes {@code push} as given by {@code kind}, for whoever reads them, without pushing: one worked as blows. */
    public void note(final ForceKind kind, final Vector3d push) {
        byKind.get(kind).add(push);
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
        BALLOONS
    }
}
