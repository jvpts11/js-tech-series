/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PersonnelDoorSwingTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    public void angle_openingSwingsFromShutToAQuarterTurnOverItsTime() {
        assertEquals(0, PersonnelDoorSwing.angle(true, 0), CLOSE);
        assertEquals(PersonnelDoorSwing.OPEN, PersonnelDoorSwing.angle(true, PersonnelDoorSwing.TICKS), CLOSE);
        assertEquals(PersonnelDoorSwing.OPEN, PersonnelDoorSwing.angle(true, PersonnelDoorSwing.TICKS * 3.0), CLOSE);
    }

    @Test
    public void angle_shuttingSwingsBackFromOpenToShut() {
        assertEquals(PersonnelDoorSwing.OPEN, PersonnelDoorSwing.angle(false, 0), CLOSE);
        assertEquals(0, PersonnelDoorSwing.angle(false, PersonnelDoorSwing.TICKS), CLOSE);
    }

    @Test
    public void angle_halfWayIsHalfOpenAndItEasesAtBothEnds() {
        final double half = PersonnelDoorSwing.angle(true, PersonnelDoorSwing.TICKS / 2.0);
        final double early = PersonnelDoorSwing.angle(true, PersonnelDoorSwing.TICKS / 10.0);

        assertEquals(PersonnelDoorSwing.OPEN / 2, half, CLOSE);
        assertTrue(early < PersonnelDoorSwing.OPEN / 10, "it starts slowly: " + early);
    }

    @Test
    public void angle_beforeItEverSwungIsAtRest() {
        assertEquals(0, PersonnelDoorSwing.angle(false, Double.MAX_VALUE), CLOSE);
        assertEquals(PersonnelDoorSwing.OPEN, PersonnelDoorSwing.angle(true, Double.MAX_VALUE), CLOSE);
    }
}
