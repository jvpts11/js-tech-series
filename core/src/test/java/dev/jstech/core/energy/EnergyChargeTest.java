/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EnergyChargeTest {

    @Test
    void level_ofAnEmptyStoreIsNone() {
        assertEquals(0, EnergyCharge.level(0L, 1_000L, 4));
    }

    @Test
    void level_ofAnyChargeIsAtLeastOneStep() {
        assertEquals(1, EnergyCharge.level(1L, 1_000_000L, 4));
    }

    @Test
    void level_ofAFullStoreIsEveryStepAndOnlyThen() {
        assertEquals(4, EnergyCharge.level(1_000L, 1_000L, 4));
        assertEquals(3, EnergyCharge.level(999L, 1_000L, 4));
    }

    @Test
    void level_followsTheCharge() {
        assertEquals(2, EnergyCharge.level(500L, 1_000L, 4));
        assertEquals(3, EnergyCharge.level(600L, 1_000L, 4));
    }

    @Test
    void level_refusesNoSteps() {
        assertThrows(IllegalArgumentException.class, () -> EnergyCharge.level(1L, 1L, 0));
    }
}
