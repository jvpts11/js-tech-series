/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3d;
import org.joml.Vector3d;

/**
 * The mass of a body made of several parts held as one, a ship and the turret and the shuttle docked on it: the whole
 * mass, its centre, and its inertia about that centre, each part's own inertia about its own centre moved there by the
 * parallel axis rule. The places and axes are of one frame, the body's own; the sums are kept about the first part's
 * centre, so they stay exact however far out the frame puts them.
 */
public final class CompoundMass {

    private final List<Part> parts = new ArrayList<>();
    private final Vector3d scratch = new Vector3d();
    private final Matrix3d moved = new Matrix3d();

    /* Less than this is no mass at all. */
    private static final double NONE = 1.0e-9;

    /** Forgets every part. */
    public void clear() {
        parts.clear();
    }

    /** Adds a part of {@code kilograms}, its centre at {@code centre}, its {@code inertia} about that centre. */
    public void add(final double kilograms, final Vector3d centre, final Matrix3d inertia) {
        if (kilograms > NONE) {
            parts.add(new Part(kilograms, new Vector3d(centre), new Matrix3d(inertia)));
        }
    }

    /** The whole mass, in kilograms. */
    public double mass() {
        double sum = 0;
        for (final Part part : parts) {
            sum += part.kilograms;
        }
        return sum;
    }

    /** The centre of mass, into {@code into}; the origin for a body of nothing. */
    public Vector3d centre(final Vector3d into) {
        if (parts.isEmpty()) {
            return into.zero();
        }
        final Vector3d origin = parts.get(0).centre;
        double mass = 0;
        double x = 0;
        double y = 0;
        double z = 0;
        for (final Part part : parts) {
            mass += part.kilograms;
            x += part.kilograms * (part.centre.x - origin.x);
            y += part.kilograms * (part.centre.y - origin.y);
            z += part.kilograms * (part.centre.z - origin.z);
        }
        return into.set(origin.x + x / mass, origin.y + y / mass, origin.z + z / mass);
    }

    /** The inertia about the centre of mass, into {@code into}: each part's own, and that of its place about it. */
    public Matrix3d inertia(final Matrix3d into) {
        into.zero();
        if (parts.isEmpty()) {
            return into;
        }
        final Vector3d centre = centre(new Vector3d());
        for (final Part part : parts) {
            scratch.set(part.centre).sub(centre);
            final double m = part.kilograms;
            final double squared = scratch.lengthSquared();
            // m (|d|^2 E - d d^T), column-major.
            moved.set(m * (squared - scratch.x * scratch.x), -m * scratch.x * scratch.y, -m * scratch.x * scratch.z,
                    -m * scratch.y * scratch.x, m * (squared - scratch.y * scratch.y), -m * scratch.y * scratch.z,
                    -m * scratch.z * scratch.x, -m * scratch.z * scratch.y, m * (squared - scratch.z * scratch.z));
            into.add(moved).add(part.inertia);
        }
        return into;
    }

    /* One part: its mass, centre and inertia about that centre. */
    private record Part(double kilograms, Vector3d centre, Matrix3d inertia) {
    }
}
