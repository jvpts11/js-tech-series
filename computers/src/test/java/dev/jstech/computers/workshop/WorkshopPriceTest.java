/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkshopPriceTest {

    @Test
    void enchant_energyTakesTenKilojoulesForEveryLevelTheTableWouldTake() {
        assertEquals(new WorkshopPrice(0, 10_000L), WorkshopPrice.enchant(UpdateCost.ENERGY, 0));
        assertEquals(new WorkshopPrice(0, 20_000L), WorkshopPrice.enchant(UpdateCost.ENERGY, 1));
        assertEquals(new WorkshopPrice(0, 30_000L), WorkshopPrice.enchant(UpdateCost.ENERGY, 2));
    }

    @Test
    void enchant_xpTakesHalfTheTablesLevelsRoundedUp() {
        assertEquals(new WorkshopPrice(1, 0L), WorkshopPrice.enchant(UpdateCost.XP, 0));
        assertEquals(new WorkshopPrice(1, 0L), WorkshopPrice.enchant(UpdateCost.XP, 1));
        assertEquals(new WorkshopPrice(2, 0L), WorkshopPrice.enchant(UpdateCost.XP, 2));
    }

    @Test
    void enchant_bothTakesTheEnergyAndTheLevelsAndNoneTakesNothing() {
        assertEquals(new WorkshopPrice(2, 30_000L), WorkshopPrice.enchant(UpdateCost.BOTH, 2));
        assertTrue(WorkshopPrice.enchant(UpdateCost.NONE, 2).free());
    }

    @Test
    void enchant_clampsAnOfferOutOfRange() {
        assertEquals(WorkshopPrice.enchant(UpdateCost.BOTH, 2), WorkshopPrice.enchant(UpdateCost.BOTH, 7));
        assertEquals(WorkshopPrice.enchant(UpdateCost.BOTH, 0), WorkshopPrice.enchant(UpdateCost.BOTH, -1));
    }

    @Test
    void smelt_takesTwoKilojoulesAndNoLevels() {
        assertEquals(new WorkshopPrice(0, 2_000L), WorkshopPrice.smelt(UpdateCost.ENERGY));
        assertEquals(new WorkshopPrice(0, 2_000L), WorkshopPrice.smelt(UpdateCost.BOTH));
        assertTrue(WorkshopPrice.smelt(UpdateCost.XP).free());
    }

    @Test
    void anvil_repairTakesTwoKilojoulesForEveryPercentGivenBack() {
        assertEquals(new WorkshopPrice(0, 100_000L), WorkshopPrice.anvil(UpdateCost.ENERGY, UpdateAction.REPAIR, 3,
                50, 100));
        assertEquals(new WorkshopPrice(0, 129L), WorkshopPrice.anvil(UpdateCost.ENERGY, UpdateAction.REPAIR, 3, 1,
                1_561));
        assertEquals(new WorkshopPrice(2, 0L), WorkshopPrice.anvil(UpdateCost.BOTH, UpdateAction.REPAIR, 3, 0, 100));
    }

    @Test
    void anvil_combineTakesTenKilojoulesForEveryLevelTheAnvilWouldAsk() {
        assertEquals(new WorkshopPrice(0, 70_000L), WorkshopPrice.anvil(UpdateCost.ENERGY, UpdateAction.COMBINE, 7,
                0, 0));
        assertEquals(new WorkshopPrice(4, 0L), WorkshopPrice.anvil(UpdateCost.XP, UpdateAction.COMBINE, 7, 0, 0));
    }

    @Test
    void anvil_nameTakesFiveKilojoules() {
        assertEquals(new WorkshopPrice(1, 5_000L), WorkshopPrice.anvil(UpdateCost.BOTH, UpdateAction.NAME, 1, 0,
                0));
    }

    @Test
    void halfLevels_roundsUpAndNeverFallsUnderOne() {
        assertEquals(0, WorkshopPrice.halfLevels(0));
        assertEquals(1, WorkshopPrice.halfLevels(1));
        assertEquals(1, WorkshopPrice.halfLevels(2));
        assertEquals(2, WorkshopPrice.halfLevels(3));
        assertEquals(20, WorkshopPrice.halfLevels(39));
    }

    @Test
    void constructor_neverHoldsANegativePrice() {
        assertTrue(new WorkshopPrice(-3, -10L).free());
    }
}
