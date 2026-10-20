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

class MassSumTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void centre_isTheMassWeightedMiddleOfTheBlocks() {
        final MassSum sum = new MassSum();
        sum.add(0, 0, 0, 1000, 1);
        sum.add(3, 0, 0, 3000, 1);
        final Vector3d centre = sum.centre(new Vector3d());
        assertEquals(4000, sum.mass(), CLOSE);
        assertEquals(2.25, centre.x, CLOSE);
        assertEquals(0, centre.y, CLOSE);
    }

    @Test
    void inertia_ofOneCubeIsASixthOfItsMassOnEachAxis() {
        final MassSum sum = new MassSum();
        sum.add(5, 5, 5, 600, 1);
        final Matrix3d inertia = sum.inertia(new Matrix3d());
        assertEquals(100, inertia.m00, CLOSE);
        assertEquals(100, inertia.m11, CLOSE);
        assertEquals(100, inertia.m22, CLOSE);
        assertEquals(0, inertia.m01, CLOSE);
    }

    @Test
    void inertia_ofARowAddsTheirPlacesAboutTheCentreByTheParallelAxisRule() {
        final MassSum sum = new MassSum();
        sum.add(-1, 0, 0, 6, 1);
        sum.add(1, 0, 0, 6, 1);
        final Matrix3d inertia = sum.inertia(new Matrix3d());
        // Along the row, only the cubes' own; across it, each also its mass times its distance squared.
        assertEquals(2, inertia.m00, CLOSE);
        assertEquals(2 + 12, inertia.m11, CLOSE);
        assertEquals(2 + 12, inertia.m22, CLOSE);
    }

    @Test
    void inertia_ofABlockAwayDiagonallyHasProducts() {
        final MassSum sum = new MassSum();
        sum.add(1, 1, 0, 1, 1);
        sum.add(-1, -1, 0, 1, 1);
        final Matrix3d inertia = sum.inertia(new Matrix3d());
        assertEquals(-2, inertia.m01, CLOSE);
        assertEquals(inertia.m01, inertia.m10, CLOSE);
    }

    @Test
    void remove_takesABlockBackOutAsIfNeverAdded() {
        final MassSum sum = new MassSum();
        sum.add(0, 0, 0, 500, 1);
        sum.add(4, 2, 1, 900, 1);
        sum.remove(4, 2, 1, 900, 1);
        final Vector3d centre = sum.centre(new Vector3d());
        assertEquals(500, sum.mass(), CLOSE);
        assertEquals(0, centre.length(), 1.0e-6);
        assertEquals(500.0 / 6, sum.inertia(new Matrix3d()).m00, 1.0e-6);
    }

    @Test
    void inertia_staysExactFarFromNought() {
        final MassSum sum = new MassSum();
        // A yard's places are tens of millions of blocks out.
        sum.add(25_600_000.5, 64.5, -1_873_920.5, 1000, 1);
        final Vector3d centre = sum.centre(new Vector3d());
        assertEquals(25_600_000.5, centre.x, 1.0e-6);
        assertEquals(1000.0 / 6, sum.inertia(new Matrix3d()).m11, 1.0e-6);
        sum.add(25_600_001.5, 64.5, -1_873_920.5, 1000, 1);
        assertEquals(2000.0 / 6, sum.inertia(new Matrix3d()).m00, 1.0e-6);
        assertEquals(2000.0 / 6 + 2 * 1000 * 0.25, sum.inertia(new Matrix3d()).m11, 1.0e-6);
    }

    @Test
    void empty_isTrueForABodyOfNothing() {
        final MassSum sum = new MassSum();
        assertTrue(sum.empty());
        sum.add(1, 1, 1, 10, 1);
        sum.remove(1, 1, 1, 10, 1);
        assertTrue(sum.empty());
        assertEquals(0, sum.mass(), CLOSE);
    }
}
