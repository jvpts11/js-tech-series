/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MachineScreenLayoutTest {

    private static final int OX = 100;
    private static final int OY = 50;
    private static final int S = 24;

    @Test
    void layout_keepsItsPartsApartAndInside() {
        assertTrue(MachineScreenLayout.layout().isClean(), MachineScreenLayout.layout().overlaps().toString());
    }

    @Test
    void tabAt_findsEachTabAndNothingBetween() {
        assertEquals(0, MachineScreenLayout.tabAt(MachineScreenLayout.tabX(0) + 2, 5));
        assertEquals(4, MachineScreenLayout.tabAt(MachineScreenLayout.tabX(4) + 27, 25));
        assertEquals(-1, MachineScreenLayout.tabAt(MachineScreenLayout.tabX(1) - 1, 5));
        assertEquals(-1, MachineScreenLayout.tabAt(MachineScreenLayout.tabX(0) + 2, 40));
    }

    @Test
    void cubeFaceAt_findsTheFaceUnderThePoint() {
        assertEquals(0, MachineScreenLayout.cubeFaceAt(OX, OY + S / 2.0, OX, OY, S));
        assertEquals(1, MachineScreenLayout.cubeFaceAt(OX - S / 2.0, OY + S + 6, OX, OY, S));
        assertEquals(2, MachineScreenLayout.cubeFaceAt(OX + S / 2.0, OY + S + 6, OX, OY, S));
        assertEquals(-1, MachineScreenLayout.cubeFaceAt(OX + S + 2, OY + S, OX, OY, S));
        assertEquals(-1, MachineScreenLayout.cubeFaceAt(OX, OY - 2, OX, OY, S));
    }

    @Test
    void fits_saysWhatTheStandardFaceHasRoomFor() {
        assertTrue(MachineScreenLayout.fits(3, 1, 1));
        assertTrue(MachineScreenLayout.fits(1, 2, 2));
        assertFalse(MachineScreenLayout.fits(4, 1, 1));
        assertFalse(MachineScreenLayout.fits(1, 4, 3));
    }

    @Test
    void meterMark_putsThePeakOnTheScaleWithANiceMark() {
        assertEquals(1, MachineScreenLayout.meterMark(0));
        assertEquals(1, MachineScreenLayout.meterMark(4));
        assertEquals(2, MachineScreenLayout.meterMark(5));
        assertEquals(5, MachineScreenLayout.meterMark(15));
        assertEquals(10, MachineScreenLayout.meterMark(40));
        assertEquals(20, MachineScreenLayout.meterMark(41));
        assertEquals(500, MachineScreenLayout.meterMark(1_999));
    }

    @Test
    void knobAngles_turnClockwiseFromUpAndLeft() {
        assertEquals(Math.PI * 1.25, MachineScreenLayout.knobAngle(0), 1e-9);
        assertEquals(Math.PI * 1.75, MachineScreenLayout.knobAngle(1), 1e-9);
        assertEquals(Math.PI * 1.0, MachineScreenLayout.keyAngle(0), 1e-9);
        assertEquals(Math.PI * 2.0, MachineScreenLayout.keyAngle(2), 1e-9);
    }

    @Test
    void wheelDrumAt_findsEachDrumAndNothingBetweenThem() {
        final int x = 50;
        final int y = 20;
        assertEquals(0, MachineScreenLayout.wheelDrumAt(x + 1, y + 3, x, y));
        assertEquals(2, MachineScreenLayout.wheelDrumAt(x + 1 + 2 * MachineScreenLayout.WHEEL_DIGIT, y + 9, x, y));
        assertEquals(-1, MachineScreenLayout.wheelDrumAt(x + MachineScreenLayout.WHEEL_DIGIT, y + 3, x, y));
        assertEquals(-1, MachineScreenLayout.wheelDrumAt(x + 1, y + MachineScreenLayout.WHEEL_HEIGHT, x, y));
    }

    @Test
    void resourceY_closesUpForFiveButtons() {
        assertEquals(MachineScreenLayout.RESOURCE_Y + MachineScreenLayout.RESOURCE_PITCH,
                MachineScreenLayout.resourceY(1, 4));
        assertEquals(MachineScreenLayout.RESOURCE_Y + 4 * MachineScreenLayout.RESOURCE_PITCH_TIGHT,
                MachineScreenLayout.resourceY(4, 5));
        assertTrue(MachineScreenLayout.resourceY(4, 5) + MachineScreenLayout.BUTTON_HEIGHT
                <= MachineScreenLayout.FACE_HEIGHT);
    }

    @Test
    void laneSlots_ofEveryLaneStayInsideTheProcessWindow() {
        for (int lanes = 1; lanes <= 3; lanes++) {
            for (int lane = 0; lane < lanes; lane++) {
                final var in = MachineScreenLayout.laneSlot(lane, lanes, 0, false, 2);
                final var out = MachineScreenLayout.laneSlot(lane, lanes, 1, true, 2);
                assertTrue(in.y() - 1 >= MachineScreenLayout.faceY(MachineScreenLayout.PROCESS_Y));
                assertTrue(out.y() + MachineScreenLayout.SLOT - 1
                        <= MachineScreenLayout.faceY(MachineScreenLayout.PROCESS_BOTTOM));
                assertTrue(out.x() + MachineScreenLayout.SLOT - 1
                        <= MachineScreenLayout.faceX(MachineScreenLayout.PROCESS_RIGHT));
            }
        }
    }
}
