/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class RigGeometryTest {

    private static final double EPSILON = 1e-9;

    @Test
    void of_givesEachEraItsRig() {
        assertSame(RigGeometry.TRANSITION, RigGeometry.of(HardwareEra.TRANSITION));
        assertSame(RigGeometry.STANDARD, RigGeometry.of(HardwareEra.STANDARD));
        assertSame(RigGeometry.ADVANCED, RigGeometry.of(HardwareEra.ADVANCED));
        assertEquals(4, RigGeometry.TRANSITION.risers());
        assertEquals(6, RigGeometry.STANDARD.risers());
        assertEquals(8, RigGeometry.ADVANCED.risers());
    }

    @Test
    void riserX_spreadsTheRisersEvenlyAboutTheMiddle() {
        for (final RigGeometry rig : new RigGeometry[] {RigGeometry.TRANSITION, RigGeometry.STANDARD,
                RigGeometry.ADVANCED}) {
            final double middle = (rig.left() + rig.right()) / 2.0;
            assertEquals(middle, (rig.riserX(0) + rig.riserX(rig.risers() - 1)) / 2.0, EPSILON);
            for (int i = 1; i < rig.risers(); i++) {
                assertEquals(rig.pitch(), rig.riserX(i) - rig.riserX(i - 1), EPSILON);
            }
            assertTrue(rig.riserX(0) > 0 && rig.riserX(rig.risers() - 1) < rig.width());
        }
    }

    @Test
    void place_neverTouchesItsNeighbour() {
        final RigGeometry rig = RigGeometry.ADVANCED;
        for (int i = 1; i < rig.risers(); i++) {
            assertTrue(rig.place(i - 1)[3] < rig.place(i)[0]);
        }
    }

    @Test
    void placeTurned_facingNorthIsThePlaceInBlocks() {
        final double[] place = RigGeometry.STANDARD.place(2);
        final double[] turned = RigGeometry.STANDARD.placeTurned(2, 0);
        for (int k = 0; k < place.length; k++) {
            assertEquals(place[k] / 16.0, turned[k], EPSILON);
        }
    }

    @Test
    void placeTurned_turnsAboutTheOriginBlocksMiddle() {
        final RigGeometry rig = RigGeometry.TRANSITION;
        final double[] north = rig.placeTurned(0, 0);
        final double[] south = rig.placeTurned(0, 2);
        // Half a turn mirrors the place through the block's middle, its height kept.
        assertArrayEquals(new double[] {1 - north[3], north[1], 1 - north[5], 1 - north[0], north[4], 1 - north[2]},
                south, EPSILON);
        // A quarter turn east takes the rig's west side to the north, so x becomes z.
        final double[] east = rig.placeTurned(0, 1);
        assertEquals(north[0], east[2], EPSILON);
        assertEquals(north[3], east[5], EPSILON);
        assertEquals(1 - north[5], east[0], EPSILON);
    }

    @Test
    void placeTurned_aWideRigReachesIntoItsSecondBlock() {
        final RigGeometry rig = RigGeometry.STANDARD;
        // Facing north the second block is east of the origin: the last riser stands past x = 1.
        assertTrue(rig.placeTurned(rig.risers() - 1, 0)[3] > 1.0);
        // Facing east it is south of the origin: past z = 1.
        assertTrue(rig.placeTurned(rig.risers() - 1, 1)[5] > 1.0);
    }
}
