/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormedPieceTest {

    @Test
    void holds_aPointWithinOrJustOffItsSolids() {
        final FormedPiece seat = new FormedPiece("seat", List.of(box(0, 0, 0, 1, 0.5, 1), box(0, 0.5, 0.8, 1, 2, 1)));

        assertTrue(seat.holds(new double[] {0.5, 0.25, 0.5}), "in its pan");
        assertTrue(seat.holds(new double[] {0.5, 1.5, 0.9}), "in its back");
        assertTrue(seat.holds(new double[] {0.5, 0.51, 0.5}), "on its pan, a hair over");
        assertFalse(seat.holds(new double[] {0.5, 1.5, 0.5}), "in front of its back, over its pan");
        assertFalse(seat.holds(new double[] {1.5, 0.25, 0.5}), "beside it");
    }

    @Test
    void lines_ofABoxAreItsTwelveEdges() {
        final FormedPiece locker = new FormedPiece("locker", List.of(box(0, 0, 0, 1, 2, 1)));

        final float[] lines = locker.lines();

        assertEquals(12, lines.length / MeshOutline.LINE);
    }

    @Test
    void lines_ofTwoSolidsAreEachOnesEdges() {
        final FormedPiece seat = new FormedPiece("seat", List.of(box(0, 0, 0, 1, 0.5, 1), box(0, 0.5, 0.8, 1, 2, 1)));

        assertEquals(24, seat.lines().length / MeshOutline.LINE);
    }

    @Test
    void at_findsThePieceHoldingThePoint() {
        final FormedPiece seat = new FormedPiece("seat", List.of(box(0, 0, 0, 1, 1, 1)));
        final FormedPiece console = new FormedPiece("console", List.of(box(2, 0, 0, 3, 1, 1)));

        assertSame(console, FormedPiece.at(List.of(seat, console), new double[] {2.5, 0.5, 0.5}));
        assertNull(FormedPiece.at(List.of(seat, console), new double[] {1.5, 0.5, 0.5}));
    }

    @Test
    void constructor_refusesAPieceOfNoSolid() {
        assertThrows(IllegalArgumentException.class, () -> new FormedPiece("nothing", List.of()));
    }

    @Test
    void leafHolds_onlyWithinItsLeafShutNotOffIt() {
        final FormedPiece door = new FormedPiece("door", List.of(box(-0.6, 0, -0.1, 0.6, 2.1, 0.1)), List.of(0, 0, 0),
                List.of(box(-0.5, 0, 0, 0.5, 2, 0.0625)));

        assertTrue(door.door());
        assertTrue(door.leafHolds(new double[] {0, 1, 0.03}));
        assertFalse(door.leafHolds(new double[] {0, 1, 0.07}), "the aiming margin is no part of the leaf");
        assertFalse(new FormedPiece("seat", List.of(box(0, 0, 0, 1, 1, 1))).door());
    }

    @Test
    void leafBounds_areItsLeafsCorners() {
        final FormedPiece door = new FormedPiece("door", List.of(box(-0.6, 0, -0.1, 0.6, 2.1, 0.1)), List.of(0, 0, 0),
                List.of(box(-0.5, 0, 0, 0.5, 2, 0.0625)));

        final double[] bounds = door.leafBounds();

        assertEquals(-0.5, bounds[0], 1e-6);
        assertEquals(2, bounds[4], 1e-6);
        assertEquals(0.0625, bounds[5], 1e-6);
    }

    @Test
    void constructor_refusesADoorWorkingByNoBlock() {
        assertThrows(IllegalArgumentException.class, () -> new FormedPiece("door", List.of(box(0, 0, 0, 1, 1, 1)),
                List.of(), List.of(box(0, 0, 0, 1, 1, 0.1))));
    }

    /* A box's twelve triangles from ({@code x0}, {@code y0}, {@code z0}) to ({@code x1}, {@code y1}, {@code z1}). */
    private static float[] box(final double x0, final double y0, final double z0, final double x1, final double y1,
                               final double z1) {
        final double[] lo = {x0, y0, z0};
        final double[] hi = {x1, y1, z1};
        final List<float[]> corners = new ArrayList<>();
        for (int axis = 0; axis < 3; axis++) {
            for (final boolean high : new boolean[] {false, true}) {
                final int u = (axis + 1) % 3;
                final int v = (axis + 2) % 3;
                final double[][] square = {{lo[u], lo[v]}, {hi[u], lo[v]}, {hi[u], hi[v]}, {lo[u], hi[v]}};
                final float[][] points = new float[4][3];
                for (int k = 0; k < 4; k++) {
                    points[k][axis] = (float) (high ? hi[axis] : lo[axis]);
                    points[k][u] = (float) square[k][0];
                    points[k][v] = (float) square[k][1];
                }
                final int[] order = high ? new int[] {0, 1, 2, 0, 2, 3} : new int[] {0, 2, 1, 0, 3, 2};
                for (final int k : order) {
                    corners.add(points[k]);
                }
            }
        }
        final float[] out = new float[corners.size() * 3];
        for (int i = 0; i < corners.size(); i++) {
            System.arraycopy(corners.get(i), 0, out, i * 3, 3);
        }
        return out;
    }
}
