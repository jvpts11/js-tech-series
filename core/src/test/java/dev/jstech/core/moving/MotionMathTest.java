/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotionMathTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void towards_goesNoFasterThanItsSpeedAndArrivesExactly() {
        StructurePose pose = StructurePose.at(0, 0, 0);
        final StructurePose target = StructurePose.at(0, 10, 0);
        pose = MotionMath.towards(pose, target, 3, 1);
        assertEquals(3, pose.y(), CLOSE);
        for (int i = 0; i < 3; i++) {
            pose = MotionMath.towards(pose, target, 3, 1);
        }
        assertEquals(10, pose.y(), CLOSE);
        assertTrue(MotionMath.arrived(pose, target));
    }

    @Test
    void towards_turnsNoFasterThanItsRate() {
        final StructurePose target = StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 2);
        final StructurePose pose = MotionMath.towards(StructurePose.at(0, 0, 0), target, 1, Math.PI / 8);
        assertEquals(Math.PI / 8, pose.angleTo(StructurePose.at(0, 0, 0)), 1.0e-9);
    }

    @Test
    void about_swingsThePivotRoundTheHinge() {
        final StructurePose bridge = StructurePose.at(5, 64, 0);
        final StructurePose up = MotionMath.about(bridge, 0, 64, 0, 0, 0, 1, Math.PI / 2);
        assertEquals(0, up.x(), CLOSE);
        assertEquals(69, up.y(), CLOSE);
        assertEquals(Math.PI / 2, up.angleTo(bridge), 1.0e-9);
    }

    @Test
    void facing_pointsTheStructuresForwardAlongTheDirection() {
        final StructurePose aimed = MotionMath.facing(StructurePose.at(0, 0, 0), 1, 1, 0);
        final StructureFrame frame = new StructureFrame(aimed, 0, 0, 0);
        final double s = Math.sqrt(0.5);
        assertEquals(s, frame.turnX(0, 0, 1), 1.0e-9);
        assertEquals(s, frame.turnY(0, 0, 1), 1.0e-9);
        assertEquals(0, frame.turnZ(0, 0, 1), 1.0e-9);
        assertTrue(frame.turnY(0, 1, 0) > 0, "its up stays up as far as it can");
    }
}
