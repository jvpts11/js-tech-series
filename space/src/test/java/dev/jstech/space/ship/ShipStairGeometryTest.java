/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ShipStairGeometryTest {

    private static final int AXES = 3;
    private static final double CLOSE = 1.0e-9;
    /* A player's width, how far off its middle the outer doorway, a block wide, lets them through, and a hand. */
    private static final double PLAYER = 0.6;
    private static final double THROUGH_THE_DOOR = 0.2;
    private static final double A_HAND = 0.5;

    @Test
    public void rails_keepAClimberWithinTheDoorwaysReachOfTheMiddle() {
        final List<double[]> steps = ShipStairGeometry.steps();
        final List<double[]> rails = ShipStairGeometry.rails();

        assertEquals(2 * steps.size(), rails.size());
        for (int k = 0; k < steps.size(); k++) {
            final double[] step = steps.get(k);
            final double[] near = rails.get(2 * k);
            final double[] far = rails.get(2 * k + 1);
            assertEquals(step[2], near[AXES + 2], CLOSE, "the near rail stands at the tread's end " + k);
            assertEquals(step[AXES + 2], far[2], CLOSE, "the far rail stands at the tread's other end " + k);
            assertTrue((far[2] - near[AXES + 2] - PLAYER) / 2 <= THROUGH_THE_DOOR + CLOSE, "between them a player"
                    + " stands within the doorway's reach of the middle, tread " + k);
            assertTrue(near[AXES + 1] - step[AXES + 1] >= A_HAND && far[AXES + 1] - step[AXES + 1] >= A_HAND,
                    "the rails stand a hand over the tread " + k);
        }
    }

    @Test
    public void rails_eachBlockHoldsItsPartsOfTheRailsWhole() {
        double volume = 0;
        for (final int[] cell : ShipStairGeometry.CELLS) {
            for (final double[] rail : ShipStairGeometry.rails(cell[0], cell[1], cell[2])) {
                volume += (rail[AXES] - rail[0]) * (rail[AXES + 1] - rail[1]) * (rail[AXES + 2] - rail[2]);
            }
        }
        final double whole = ShipStairGeometry.rails().stream().mapToDouble(rail -> (rail[AXES] - rail[0])
                * (rail[AXES + 1] - rail[1]) * (rail[AXES + 2] - rail[2])).sum();
        assertEquals(whole, volume, 1.0e-6);
    }

    @Test
    public void mirroredSteps_areTheAirlocksTurnedAcrossTheMiddleLine() {
        final List<double[]> airlock = ShipStairGeometry.steps();
        final List<double[]> mirrored = ShipStairGeometry.mirroredSteps();

        assertEquals(airlock.size(), mirrored.size());
        for (int k = 0; k < airlock.size(); k++) {
            final double[] step = airlock.get(k);
            final double[] turned = {-step[AXES], step[1], step[2], -step[0], step[AXES + 1], step[AXES + 2]};
            for (int axis = 0; axis < 2 * AXES; axis++) {
                assertEquals(turned[axis], mirrored.get(k)[axis], CLOSE, "step " + k + " " + Arrays.toString(step));
            }
        }
    }

    @Test
    public void mirroredCells_areTheAirlocksTurnedAcrossTheMiddleLine() {
        assertEquals(ShipStairGeometry.CELLS.size(), ShipStairGeometry.MIRRORED_CELLS.size());
        for (final int[] cell : ShipStairGeometry.CELLS) {
            assertTrue(ShipStairGeometry.MIRRORED_CELLS.stream().anyMatch(other -> other[0] == -1 - cell[0]
                    && other[1] == cell[1] && other[2] == cell[2]), Arrays.toString(cell));
        }
    }

    @Test
    public void mirroredTreads_eachBlockHoldsItsPartsOfTheStepsWhole() {
        double volume = 0;
        for (final int[] cell : ShipStairGeometry.MIRRORED_CELLS) {
            for (final double[] tread : ShipStairGeometry.mirroredTreads(cell[0], cell[1], cell[2])) {
                volume += (tread[AXES] - tread[0]) * (tread[AXES + 1] - tread[1]) * (tread[AXES + 2] - tread[2]);
            }
        }
        final double whole = ShipStairGeometry.mirroredSteps().stream().mapToDouble(step -> (step[AXES] - step[0])
                * (step[AXES + 1] - step[1]) * (step[AXES + 2] - step[2])).sum();
        assertEquals(whole, volume, 1.0e-6);
    }
}
