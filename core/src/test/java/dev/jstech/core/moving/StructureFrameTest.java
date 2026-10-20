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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StructureFrameTest {

    private static final double CLOSE = 1.0e-9;
    /* A pivot far out in the yard, where the numbers are as large as they ever are. */
    private static final double PIVOT_X = 27_000_008.5;
    private static final double PIVOT_Y = 70.5;
    private static final double PIVOT_Z = -4_000_000.5;

    @Test
    void worldX_putsThePivotWhereThePoseStands() {
        final StructureFrame frame = frame(StructurePose.at(10, 64, -3));
        assertEquals(10, frame.worldX(PIVOT_X, PIVOT_Y, PIVOT_Z), CLOSE);
        assertEquals(64, frame.worldY(PIVOT_X, PIVOT_Y, PIVOT_Z), CLOSE);
        assertEquals(-3, frame.worldZ(PIVOT_X, PIVOT_Y, PIVOT_Z), CLOSE);
    }

    @Test
    void yardX_takesAWorldPlaceBackToWhereItWasInTheYard() {
        final StructureFrame frame = frame(StructurePose.turned(5, 80, 7, 0.3, 1, -0.4, 1.234));
        final double yx = PIVOT_X + 3.25;
        final double yy = PIVOT_Y - 1.5;
        final double yz = PIVOT_Z + 7.75;
        final double wx = frame.worldX(yx, yy, yz);
        final double wy = frame.worldY(yx, yy, yz);
        final double wz = frame.worldZ(yx, yy, yz);
        assertEquals(yx, frame.yardX(wx, wy, wz), 1.0e-6);
        assertEquals(yy, frame.yardY(wx, wy, wz), 1.0e-6);
        assertEquals(yz, frame.yardZ(wx, wy, wz), 1.0e-6);
    }

    @Test
    void worldX_turnsTheBlocksRoundThePivot() {
        final StructureFrame frame = frame(StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 2));
        assertEquals(0, frame.worldX(PIVOT_X + 1, PIVOT_Y, PIVOT_Z), CLOSE);
        assertEquals(-1, frame.worldZ(PIVOT_X + 1, PIVOT_Y, PIVOT_Z), CLOSE);
    }

    @Test
    void unturnX_undoesTurnX() {
        final StructureFrame frame = frame(StructurePose.turned(0, 0, 0, 1, 2, 3, 0.7));
        final double x = frame.turnX(0.2, -0.5, 0.9);
        final double y = frame.turnY(0.2, -0.5, 0.9);
        final double z = frame.turnZ(0.2, -0.5, 0.9);
        assertEquals(0.2, frame.unturnX(x, y, z), CLOSE);
        assertEquals(-0.5, frame.unturnY(x, y, z), CLOSE);
        assertEquals(0.9, frame.unturnZ(x, y, z), CLOSE);
    }

    @Test
    void worldBox_holdsEveryCornerOfATurnedBox() {
        final StructureFrame frame = frame(StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 4));
        final double[] box = frame.worldBox(PIVOT_X - 1, PIVOT_Y, PIVOT_Z - 1, PIVOT_X + 1, PIVOT_Y + 1, PIVOT_Z + 1);
        assertEquals(-Math.sqrt(2), box[0], 1.0e-9);
        assertEquals(Math.sqrt(2), box[3], 1.0e-9);
        assertEquals(0, box[1], CLOSE);
        assertEquals(1, box[4], CLOSE);
    }

    @Test
    void aligned_isTrueOnlyForQuarterTurns() {
        assertTrue(frame(StructurePose.at(0, 0, 0)).aligned());
        assertTrue(frame(StructurePose.turned(0, 0, 0, 0, 1, 0, Math.PI / 2)).aligned());
        assertTrue(frame(StructurePose.turned(0, 0, 0, 1, 0, 0, Math.PI)).aligned());
        assertFalse(frame(StructurePose.turned(0, 0, 0, 0, 1, 0, 0.3)).aligned());
    }

    private static StructureFrame frame(final StructurePose pose) {
        return new StructureFrame(pose, PIVOT_X, PIVOT_Y, PIVOT_Z);
    }
}
