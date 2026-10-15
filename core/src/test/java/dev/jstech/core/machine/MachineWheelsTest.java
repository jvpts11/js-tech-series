/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineWheelsTest {

    @Test
    void turn_movesOneDrumAlone() {
        assertEquals(74, MachineWheels.turn(64, 1, true));
        assertEquals(164, MachineWheels.turn(64, 0, true));
        assertEquals(63, MachineWheels.turn(64, 2, false));
    }

    @Test
    void turn_goesRoundWithoutCarrying() {
        assertEquals(60, MachineWheels.turn(69, 2, true));
        assertEquals(904, MachineWheels.turn(4, 0, false));
    }

    @Test
    void turn_neverGoesBelowOne() {
        assertEquals(1, MachineWheels.turn(1, 2, false));
        assertEquals(1, MachineWheels.turn(10, 1, false));
    }

    @Test
    void shown_padsToThreeDigits() {
        assertEquals("064", MachineWheels.shown(64));
        assertEquals("999", MachineWheels.shown(5_000));
        assertEquals("000", MachineWheels.shown(-3));
    }

    @Test
    void unit_countsItemsOneByOneAndTheRestInHundreds() {
        assertEquals(1, MachineWheels.unit(MachineResource.ITEMS));
        assertEquals(MachineWheels.HUNDREDS, MachineWheels.unit(MachineResource.FLUIDS));
    }
}
