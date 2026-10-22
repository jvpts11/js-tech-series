/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ShellOrientationTest {

    @Test
    void flips_turnsTheOneFaceThatFacesIn() {
        final List<List<Vector3d[]>> cube = cube();
        cube.set(2, turned(cube.get(2)));
        assertArrayEquals(new boolean[] {false, false, true, false, false, false},
                ShellOrientation.flips(cube, true));
    }

    @Test
    void flips_turnsAClosedShellFacingInWhole() {
        final List<List<Vector3d[]>> cube = cube();
        cube.replaceAll(ShellOrientationTest::turned);
        assertArrayEquals(new boolean[] {true, true, true, true, true, true}, ShellOrientation.flips(cube, true));
    }

    @Test
    void flips_leavesAShellFacingOutAsItIs() {
        assertArrayEquals(new boolean[6], ShellOrientation.flips(cube(), true));
    }

    /* A cube of side 1, each face two triangles, counter-clockwise seen from outside. */
    private static List<List<Vector3d[]>> cube() {
        final double[][] corners = {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1},
            {0, 1, 1}};
        final int[][] faces = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 1, 5, 4}, {2, 3, 7, 6}, {1, 2, 6, 5}, {3, 0, 4, 7}};
        final List<List<Vector3d[]>> out = new ArrayList<>();
        for (final int[] face : faces) {
            final Vector3d a = point(corners[face[0]]);
            final Vector3d b = point(corners[face[1]]);
            final Vector3d c = point(corners[face[2]]);
            final Vector3d d = point(corners[face[3]]);
            out.add(new ArrayList<>(List.of(new Vector3d[] {a, b, c}, new Vector3d[] {a, c, d})));
        }
        return out;
    }

    private static List<Vector3d[]> turned(final List<Vector3d[]> face) {
        final List<Vector3d[]> out = new ArrayList<>();
        for (final Vector3d[] triangle : face) {
            out.add(new Vector3d[] {triangle[0], triangle[2], triangle[1]});
        }
        return out;
    }

    private static Vector3d point(final double[] xyz) {
        return new Vector3d(xyz[0], xyz[1], xyz[2]);
    }
}
