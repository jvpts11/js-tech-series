/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StructurePoseTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void constructor_keepsTheRotationAUnitQuaternion() {
        final StructurePose pose = new StructurePose(1, 2, 3, 0, 0, 0, 5);
        assertEquals(1, pose.qw(), CLOSE);
        assertTrue(pose.unturned());
    }

    @Test
    void constructor_refusesAPoseThatIsNoPlace() {
        assertThrows(IllegalArgumentException.class, () -> new StructurePose(Double.NaN, 0, 0, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new StructurePose(0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void turned_aQuarterTurnUpIsNinetyDegreesOfYaw() {
        final StructurePose west = StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 2);
        assertEquals(Math.PI / 2, west.angleTo(StructurePose.at(0, 0, 0)), CLOSE);
        assertEquals(-90, west.yawDegrees(), 1.0e-6);
    }

    @Test
    void between_blendsThePlaceStraightAndTheRotationAlongTheArc() {
        final StructurePose from = StructurePose.at(0, 0, 0);
        final StructurePose to = StructurePose.turned(10, 0, 0, 0, 1, 0, Math.PI / 2);
        final StructurePose half = StructurePose.between(from, to, 0.5);
        assertEquals(5, half.x(), CLOSE);
        assertEquals(Math.PI / 4, half.angleTo(from), 1.0e-9);
        assertEquals(Math.PI / 4, half.angleTo(to), 1.0e-9);
    }

    @Test
    void between_takesTheShortWayRoundFromAQuaternionOfTheOtherSign() {
        final StructurePose from = StructurePose.turned(0, 0, 0, 0, 1, 0, 0.1);
        final StructurePose flipped = new StructurePose(0, 0, 0, -from.qx(), -from.qy(), -from.qz(), -from.qw());
        assertEquals(0, StructurePose.between(StructurePose.at(0, 0, 0), flipped, 1).angleTo(from), 1.0e-9);
    }

    @Test
    void then_addsRotationsInTheWorldsAxes() {
        final StructurePose quarter = StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 2);
        final StructurePose half = quarter.then(quarter);
        assertEquals(Math.PI, half.angleTo(StructurePose.at(0, 0, 0)), 1.0e-9);
        assertEquals(half.angleTo(StructurePose.at(0, 0, 0)),
                StructurePose.at(0, 0, 0).turnedBy(0, 1, 0, Math.PI).angleTo(StructurePose.at(0, 0, 0)), 1.0e-9);
    }
}
