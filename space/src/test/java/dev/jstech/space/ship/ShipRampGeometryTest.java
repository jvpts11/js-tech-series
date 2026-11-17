/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShipRampGeometryTest {

    private static final double EPSILON = 1.0e-9;
    /* How high a player steps up of themselves, in blocks; the deck and the land in the hold's model's space. */
    private static final double STEP_UP = 0.6;
    private static final double DECK = 0.35;
    private static final double LAND = -2.0;
    private static final double NEAR = 0.3;
    /* The hold's tail, along the ship, and the ramp's half width. */
    private static final double TAIL = 5.0;
    private static final double HALF = 0.97;

    @Test
    void steps_goDownFromTheDeckToTheLandAStepAtATime() {
        final List<double[]> steps = ShipRampGeometry.steps();
        assertFalse(steps.isEmpty());
        double above = DECK;
        double along = TAIL;
        for (final double[] step : steps) {
            assertTrue(above - step[4] <= STEP_UP + EPSILON && above - step[4] >= -EPSILON,
                    "each step a step down from the one before: " + step[4] + " under " + above);
            assertEquals(along, step[2], EPSILON, "each step from where the one before ends, out from the tail");
            assertTrue(step[0] <= -HALF + EPSILON && step[3] >= HALF - EPSILON, "as wide as the ramp");
            above = step[4];
            along = step[5];
        }
        assertTrue(above - LAND <= NEAR, "down to the land: the last step's top at " + above);
    }

    @Test
    void treads_ofABlockAreTheStepsWithinIt() {
        int found = 0;
        for (final int[] cell : ShipRampGeometry.CELLS) {
            for (final double[] tread : ShipRampGeometry.treads(cell[0], cell[1], cell[2])) {
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(tread[axis] >= -EPSILON && tread[axis + 3] <= 1 + EPSILON
                            && tread[axis] < tread[axis + 3], "within its block, from 0 to 1");
                }
                found++;
            }
        }
        assertTrue(found >= ShipRampGeometry.steps().size(), "every step stands in a block of the ramp");
        assertTrue(ShipRampGeometry.treads(0, 1, 2).isEmpty(), "and none in the hold");
    }
}
