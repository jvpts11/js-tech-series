/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SweptBoxesTest {

    private static final double[] SQUARE = {1, 0, 0, 0, 1, 0, 0, 0, 1};

    @Test
    void sweep_aFallingBoxLandsOnTheFloorItFallsOnto() {
        final SweptBoxes.Turned box = box(0.5, 3, 0.5, SQUARE);
        final SweptBoxes.Contact hit = SweptBoxes.sweep(box, 0, -4, 0, 0, 0, 0, 1, 1, 1);
        assertNotNull(hit);
        assertFalse(hit.overlapping());
        assertEquals(1, hit.ny(), 1.0e-9);
        assertEquals(1.5 / 4, hit.time(), 1.0e-6);
    }

    @Test
    void sweep_missesABoxItPassesBeside() {
        final SweptBoxes.Turned box = box(3, 3, 0.5, SQUARE);
        assertNull(SweptBoxes.sweep(box, 0, -4, 0, 0, 0, 0, 1, 1, 1));
    }

    @Test
    void sweep_slidingOverAFloorDoesNotCatchOnTheSeamsBetweenItsBlocks() {
        final SweptBoxes.Turned box = box(0.5, 1.0 + 0.5 + 1.0e-6, 0.5, SQUARE);
        assertNull(SweptBoxes.sweep(box, 2, 0, 0, 1, 0, 0, 2, 1, 1));
    }

    @Test
    void sweep_aTurnedBoxTouchesWhereItsCornerReachesTheWall() {
        final double c = Math.cos(Math.PI / 4);
        final double[] turned = {c, 0, -c, 0, 1, 0, c, 0, c};
        final SweptBoxes.Turned box = box(-2, 0.5, 0.5, turned);
        final SweptBoxes.Contact hit = SweptBoxes.sweep(box, 2, 0, 0, 0, 0, 0, 1, 1, 1);
        assertNotNull(hit);
        final double reach = 0.5 * Math.sqrt(2);
        assertEquals((2 - reach) / 2, hit.time(), 1.0e-6);
        assertEquals(-1, hit.nx(), 1.0e-9);
    }

    @Test
    void sweep_aBoxAlreadyInsideIsPushedOutTheShortestWay() {
        final SweptBoxes.Turned box = box(0.5, 1.2, 0.5, SQUARE);
        final SweptBoxes.Contact hit = SweptBoxes.sweep(box, 0, 0, 0, 0, 0, 0, 1, 1, 1);
        assertNotNull(hit);
        assertTrue(hit.overlapping());
        assertEquals(1, hit.ny(), 1.0e-9);
        assertEquals(0.3, hit.depth(), 1.0e-9);
    }

    private static SweptBoxes.Turned box(final double x, final double y, final double z, final double[] sides) {
        final SweptBoxes.Turned box = new SweptBoxes.Turned();
        box.set(x, y, z, 0.5, 0.5, 0.5, sides);
        return box;
    }
}
