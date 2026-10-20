/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class CompoundMassTest {

    private static final double CLOSE = 1.0e-6;
    /* Far out, as the yard's places are. */
    private static final double FAR = 27_000_000;

    @Test
    void mass_sumsItsParts() {
        final CompoundMass compound = new CompoundMass();
        compound.add(100, new Vector3d(FAR, 0, 0), new Matrix3d().zero());
        compound.add(300, new Vector3d(FAR + 4, 0, 0), new Matrix3d().zero());
        assertEquals(400, compound.mass(), CLOSE);
    }

    @Test
    void centre_isWeightedByMassEvenFarOut() {
        final CompoundMass compound = new CompoundMass();
        compound.add(100, new Vector3d(FAR, 10, 0), new Matrix3d().zero());
        compound.add(300, new Vector3d(FAR + 4, 10, 0), new Matrix3d().zero());
        final Vector3d centre = compound.centre(new Vector3d());
        assertEquals(FAR + 3, centre.x, CLOSE);
        assertEquals(10, centre.y, CLOSE);
    }

    @Test
    void inertia_isAsTheBlocksWouldGiveTogether() {
        // Two cubes of a metre side, each holding its own inertia of m/6 on each axis, two metres apart along x.
        final MassSum blocks = new MassSum();
        blocks.add(FAR + 0.5, 0.5, 0.5, 50, 1);
        blocks.add(FAR + 2.5, 0.5, 0.5, 150, 1);
        final CompoundMass compound = new CompoundMass();
        compound.add(50, new Vector3d(FAR + 0.5, 0.5, 0.5), new Matrix3d().scaling(50.0 / 6));
        compound.add(150, new Vector3d(FAR + 2.5, 0.5, 0.5), new Matrix3d().scaling(150.0 / 6));
        final Matrix3d expected = blocks.inertia(new Matrix3d());
        final Matrix3d actual = compound.inertia(new Matrix3d());
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), CLOSE);
            }
        }
    }
}
