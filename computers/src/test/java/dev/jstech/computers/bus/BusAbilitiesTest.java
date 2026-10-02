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
    void of_givesEachEraWhatTheEarlierOnesCould() {
        final HardwareEra[] eras = HardwareEra.values();
        for (int i = 1; i < eras.length; i++) {
            final BusAbilities earlier = BusAbilities.of(eras[i - 1]);
            final BusAbilities later = BusAbilities.of(eras[i]);
            assertTrue(later.features().containsAll(earlier.features()), eras[i] + " can do all " + eras[i - 1]
                    + " could");
        }
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
