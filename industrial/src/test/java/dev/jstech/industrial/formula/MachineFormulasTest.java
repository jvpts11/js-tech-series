/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineFormulasTest {

    @Test
    void upgradeSlots_growWithTheTier() {
        assertEquals(0, MachineFormulas.upgradeSlots(0));
        assertEquals(2, MachineFormulas.upgradeSlots(1));
        assertEquals(3, MachineFormulas.upgradeSlots(2));
        assertEquals(3, MachineFormulas.upgradeSlots(3));
        assertEquals(4, MachineFormulas.upgradeSlots(4));
        assertEquals(4, MachineFormulas.upgradeSlots(9));
    }

    @Test
    void fourMotors_makeAMachineThreeTimesAsFastAtAThirdMoreAnItem() {
        assertEquals(3.0, MachineFormulas.speed(4, 0));
        assertEquals(4.0, MachineFormulas.energy(4, 0, 0));
        assertEquals(4.0 / 3.0, MachineFormulas.energyPerItem(4, 0, 0), 1e-12);
    }

    @Test
    void fourDrives_bringTheEnergyDownTo41Percent() {
        assertEquals(0.4096, MachineFormulas.energy(0, 4, 0), 1e-12);
    }

    @Test
    void overdrive_addsOneToTheSpeedAndDoublesTheEnergy() {
        assertEquals(2.0, MachineFormulas.energy(0, 0, 1));
        assertEquals(2.0, MachineFormulas.speed(0, 1));
    }

    @Test
    void recipesAtOnce_addOneALineAndTwoAnAdvancedOne() {
        assertEquals(1, MachineFormulas.recipesAtOnce(0, 0));
        assertEquals(4, MachineFormulas.recipesAtOnce(1, 1));
    }
}
