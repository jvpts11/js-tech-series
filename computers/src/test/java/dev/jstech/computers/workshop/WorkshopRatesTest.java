/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class WorkshopRatesTest {

    @Test
    void furnaceSpeed_risesWithTheComputersEraFromTheLegacy() {
        assertEquals(0, WorkshopRates.furnaceSpeed(HardwareEra.VINTAGE));
        assertEquals(2, WorkshopRates.furnaceSpeed(HardwareEra.LEGACY));
        assertEquals(3, WorkshopRates.furnaceSpeed(HardwareEra.TRANSITION));
        assertEquals(4, WorkshopRates.furnaceSpeed(HardwareEra.STANDARD));
        assertEquals(6, WorkshopRates.furnaceSpeed(HardwareEra.ADVANCED));
    }

    @Test
    void ticksPerItem_dividesTheRecipesTimeAndNeverDropsUnderATick() {
        assertEquals(100, WorkshopRates.ticksPerItem(200, 2));
        assertEquals(67, WorkshopRates.ticksPerItem(200, 3));
        assertEquals(1, WorkshopRates.ticksPerItem(1, 6));
        assertEquals(Integer.MAX_VALUE, WorkshopRates.ticksPerItem(200, 0));
    }

    @Test
    void enchantLevels_areOneOneAndTwo() {
        assertEquals(1, WorkshopRates.enchantLevels(0));
        assertEquals(1, WorkshopRates.enchantLevels(1));
        assertEquals(2, WorkshopRates.enchantLevels(2));
        assertEquals(2, WorkshopRates.enchantLevels(7));
    }

    @Test
    void anvilLevels_areTwoThirdsRoundedUp() {
        assertEquals(0, WorkshopRates.anvilLevels(0));
        assertEquals(1, WorkshopRates.anvilLevels(1));
        assertEquals(4, WorkshopRates.anvilLevels(6));
        assertEquals(5, WorkshopRates.anvilLevels(7));
        assertEquals(26, WorkshopRates.anvilLevels(39));
    }
}
