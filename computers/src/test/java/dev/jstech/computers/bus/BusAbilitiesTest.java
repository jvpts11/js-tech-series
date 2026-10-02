/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class BusAbilitiesTest {

    @Test
    void of_givesEachEraItsSpeed() {
        assertEquals(1, BusAbilities.of(HardwareEra.VINTAGE).itemsPerTick());
        assertEquals(8, BusAbilities.of(HardwareEra.LEGACY).itemsPerTick());
        assertEquals(16, BusAbilities.of(HardwareEra.TRANSITION).itemsPerTick());
        assertEquals(32, BusAbilities.of(HardwareEra.STANDARD).itemsPerTick());
        assertEquals(64, BusAbilities.of(HardwareEra.ADVANCED).itemsPerTick());
    }

    @Test
    void of_givesTheVintageBusNoFilter() {
        assertTrue(BusAbilities.of(HardwareEra.VINTAGE).features().isEmpty());
    }

    @Test
    void of_givesEveryEraAfterTheVintageAFilter() {
        for (final HardwareEra era : HardwareEra.values()) {
            assertEquals(era != HardwareEra.VINTAGE, BusAbilities.of(era).can(BusFeature.FILTER), era.name());
        }
    }

    @Test
    void of_givesTheTransitionItsQuantitiesItemByItem() {
        final BusAbilities transition = BusAbilities.of(HardwareEra.TRANSITION);

        assertTrue(transition.can(BusFeature.ITEM_QUANTITIES));
        assertFalse(transition.can(BusFeature.QUANTITIES));
    }

    @Test
    void of_givesTheStandardAndTheAdvancedQuantitiesForTheWholeBus() {
        for (final HardwareEra era : new HardwareEra[] {HardwareEra.LEGACY, HardwareEra.STANDARD,
                HardwareEra.ADVANCED}) {
            assertTrue(BusAbilities.of(era).can(BusFeature.QUANTITIES), era.name());
            assertFalse(BusAbilities.of(era).can(BusFeature.ITEM_QUANTITIES), era.name());
        }
    }

    @Test
    void of_givesTheAdvancedAllTheStandardCould() {
        assertTrue(BusAbilities.of(HardwareEra.ADVANCED).features()
                .containsAll(BusAbilities.of(HardwareEra.STANDARD).features()));
    }

    @Test
    void of_givesTagsAndFuzzyOnlyToTheAdvancedBus() {
        assertTrue(BusAbilities.of(HardwareEra.ADVANCED).can(BusFeature.TAGS));
        assertTrue(BusAbilities.of(HardwareEra.ADVANCED).can(BusFeature.FUZZY));
        assertFalse(BusAbilities.of(HardwareEra.STANDARD).can(BusFeature.TAGS));
    }

    @Test
    void of_givesConditionsFromTheStandardBusOn() {
        assertFalse(BusAbilities.of(HardwareEra.TRANSITION).can(BusFeature.CONDITIONS));
        assertTrue(BusAbilities.of(HardwareEra.STANDARD).can(BusFeature.CONDITIONS));
        assertTrue(BusAbilities.of(HardwareEra.STANDARD).can(BusFeature.PRIORITY));
    }

    @Test
    void speedOn_neverPassesTheCable() {
        final BusAbilities advanced = BusAbilities.of(HardwareEra.ADVANCED);

        assertEquals(16, advanced.speedOn(16));
        assertEquals(64, advanced.speedOn(512));
        assertEquals(0, advanced.speedOn(0));
    }
}
