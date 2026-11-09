/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wireless;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class WirelessStandardTest {

    @Test
    void of_givesEachEraFromTheLegacyItsWifiAndTheVintageNone() {
        assertNull(WirelessStandard.of(HardwareEra.VINTAGE));
        assertSame(WirelessStandard.LEGACY, WirelessStandard.of(HardwareEra.LEGACY));
        assertSame(WirelessStandard.TRANSITION, WirelessStandard.of(HardwareEra.TRANSITION));
        assertSame(WirelessStandard.STANDARD, WirelessStandard.of(HardwareEra.STANDARD));
        assertSame(WirelessStandard.ADVANCED, WirelessStandard.of(HardwareEra.ADVANCED));
    }

    @Test
    void table_isTheDesignsReachSpeedAndDraw() {
        assertEquals(24, WirelessStandard.LEGACY.reachBlocks());
        assertEquals(4, WirelessStandard.LEGACY.itemsPerTick());
        assertEquals(10, WirelessStandard.LEGACY.watts());
        assertEquals(64, WirelessStandard.ADVANCED.reachBlocks());
        assertEquals(128, WirelessStandard.ADVANCED.itemsPerTick());
        assertEquals(20, WirelessStandard.ADVANCED.watts());
        assertEquals("802.11b/g", WirelessStandard.LEGACY.accessPointName());
        assertEquals("802.11g", WirelessStandard.LEGACY.cardName());
    }

    @Test
    void older_isTheSlowerEndWhicheverSideItIsOn() {
        assertSame(WirelessStandard.TRANSITION, WirelessStandard.older(WirelessStandard.TRANSITION,
                WirelessStandard.STANDARD));
        assertSame(WirelessStandard.TRANSITION, WirelessStandard.older(WirelessStandard.STANDARD,
                WirelessStandard.TRANSITION));
        assertSame(WirelessStandard.ADVANCED, WirelessStandard.older(WirelessStandard.ADVANCED,
                WirelessStandard.ADVANCED));
    }

    @Test
    void speeds_growWithEveryEra() {
        final WirelessStandard[] all = WirelessStandard.values();
        for (int i = 1; i < all.length; i++) {
            assertTrue(all[i].itemsPerTick() > all[i - 1].itemsPerTick(), all[i].name());
            assertTrue(all[i].reachBlocks() > all[i - 1].reachBlocks(), all[i].name());
        }
    }

    @Test
    void byName_readsWhatSerializedNameWrites() {
        for (final WirelessStandard standard : WirelessStandard.values()) {
            assertSame(standard, WirelessStandard.byName(standard.serializedName()));
        }
        assertSame(WirelessStandard.LEGACY, WirelessStandard.byName("nothing"));
    }
}
