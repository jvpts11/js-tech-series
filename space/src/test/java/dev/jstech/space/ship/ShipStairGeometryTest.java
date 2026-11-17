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
