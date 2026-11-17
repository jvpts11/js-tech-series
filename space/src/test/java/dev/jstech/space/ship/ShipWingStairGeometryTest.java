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

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ShipWingStairGeometryTest {

    /* How high a player steps up walking; the land under the hull's foot; how near the land the stair comes. */
    private static final double STEP_UP = 0.6;
    private static final double LAND = -2.0;
    private static final double NEAR_THE_LAND = 0.6;
    /* Half a player's width. */
    private static final double HALF_A_PLAYER = 0.3;
    private static final int AXES = 3;
    private static final double CLOSE = 1.0e-9;

    @Test
    public void of_findsEachWingByItsModelsName() {
        assertEquals(ShipWingStairGeometry.SWEPT, ShipWingStairGeometry.of("small_swept_wings").orElseThrow());
        assertEquals(ShipWingStairGeometry.STUB, ShipWingStairGeometry.of("small_stub_wings").orElseThrow());
        assertEquals(ShipWingStairGeometry.DELTA, ShipWingStairGeometry.of("small_blended_delta").orElseThrow());
        assertFalse(ShipWingStairGeometry.of("small_side_door").isPresent());
    }

    @Test
    public void steps_fromTheWingsTopAStepDownEachToNearTheLand() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            final List<double[]> steps = wing.steps().stream()
                    .sorted(Comparator.comparingDouble(step -> -step[AXES + 1])).toList();
            double above = steps.getFirst()[AXES + 1];
            for (final double[] step : steps) {
                assertTrue(above - step[AXES + 1] <= STEP_UP, wing.model() + ": a step down each");
                above = step[AXES + 1];
            }
            assertTrue(above - LAND <= NEAR_THE_LAND && above > LAND, wing.model() + ": down to near the land: "
                    + above);
        }
    }

    @Test
    public void steps_aPlayerClearOfTheWingStandsAStepUnderItsTop() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            // Standing just past the trailing edge, their whole width clear of it, on the highest tread under them.
            final double from = wing.hingeZ();
            final double to = from + 2 * HALF_A_PLAYER;
            final double highest = wing.steps().stream().filter(step -> step[AXES + 2] > from && step[2] < to)
                    .mapToDouble(step -> step[AXES + 1]).max().orElseThrow();
            assertTrue(wing.top() - highest <= STEP_UP, wing.model() + ": a step up onto the wing: " + highest
                    + " under " + wing.top());
        }
    }

    @Test
    public void steps_hangUnderTheWingAndStandWithinTheStairsWidth() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            assertTrue(wing.hingeY() < wing.top(), wing.model() + ": hinged under the wing");
            for (final double[] step : wing.steps()) {
                assertTrue(step[0] >= ShipWingStairGeometry.ACROSS_FROM && step[AXES] <= ShipWingStairGeometry
                        .ACROSS_TO, wing.model() + ": within the stair's width");
                assertTrue(step[2] >= wing.hingeZ() - 1.0, wing.model() + ": behind the trailing edge");
            }
        }
    }

    @Test
    public void cells_ofTheLeftWingAreTheRightsTurnedAcrossTheMiddleLine() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            final List<int[]> right = wing.cells(false);
            final List<int[]> left = wing.cells(true);
            assertEquals(right.size(), left.size(), wing.model());
            for (final int[] cell : right) {
                assertTrue(left.stream().anyMatch(other -> other[0] == -1 - cell[0] && other[1] == cell[1]
                        && other[2] == cell[2]), wing.model());
                final List<double[]> mirrored = wing.treads(-1 - cell[0], cell[1], cell[2]);
                final List<double[]> own = wing.treads(cell[0], cell[1], cell[2]);
                assertEquals(own.size(), mirrored.size(), wing.model());
                for (int k = 0; k < own.size(); k++) {
                    assertEquals(1 - own.get(k)[AXES], mirrored.get(k)[0], CLOSE, wing.model());
                    assertEquals(1 - own.get(k)[0], mirrored.get(k)[AXES], CLOSE, wing.model());
                    assertEquals(own.get(k)[AXES + 1], mirrored.get(k)[AXES + 1], CLOSE, wing.model());
                }
            }
        }
    }

    @Test
    public void stateCell_isABlockOfTheWingByItsRootOnEachSide() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            final int[] right = wing.stateCell(false);
            final int[] left = wing.stateCell(true);
            assertEquals(2, right[0], wing.model());
            assertEquals(-1 - right[0], left[0], wing.model());
            assertEquals(right[1], left[1], wing.model());
            assertEquals(right[2], left[2], wing.model());
            assertTrue(right[2] > wing.lead() && right[2] + 1 < wing.trail(), wing.model() + ": within its chord");
        }
    }

    @Test
    public void lead_andTrailAreTheRootsChordAlongTheHullsSide() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            assertTrue(wing.lead() < wing.trail(), wing.model());
            assertTrue(Math.abs(wing.hingeZ() - wing.trail()) < 0.1, wing.model() + ": hinged at its trailing edge");
        }
    }

    @Test
    public void treads_eachBlockHoldsItsPartsOfTheSteps() {
        for (final ShipWingStairGeometry wing : ShipWingStairGeometry.ALL) {
            assertFalse(wing.cells(false).isEmpty(), wing.model());
            double volume = 0;
            for (final int[] cell : wing.cells(false)) {
                for (final double[] tread : wing.treads(cell[0], cell[1], cell[2])) {
                    for (int axis = 0; axis < AXES; axis++) {
                        assertTrue(tread[axis] >= -CLOSE && tread[axis + AXES] <= 1 + CLOSE, wing.model());
                    }
                    volume += (tread[AXES] - tread[0]) * (tread[AXES + 1] - tread[1]) * (tread[AXES + 2] - tread[2]);
                }
            }
            final double whole = wing.steps().stream().mapToDouble(step -> (step[AXES] - step[0])
                    * (step[AXES + 1] - step[1]) * (step[AXES + 2] - step[2])).sum();
            assertEquals(whole, volume, 1.0e-6, wing.model() + ": every tread shared among its blocks, whole");
        }
    }
}
