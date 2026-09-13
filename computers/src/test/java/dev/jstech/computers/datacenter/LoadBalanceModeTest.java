/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadBalanceModeTest {

    @Test
    void byId_readsBackEveryMode() {
        for (final LoadBalanceMode mode : LoadBalanceMode.values()) {
            assertEquals(mode, LoadBalanceMode.byId(mode.id()));
        }
    }

    @Test
    void byId_readsANewSectionsModeForAnIdNoModeDeclares() {
        assertEquals(LoadBalanceMode.ROUND_ROBIN, LoadBalanceMode.byId(-1));
        assertEquals(LoadBalanceMode.ROUND_ROBIN, LoadBalanceMode.byId(3));
    }

    @Test
    void label_readBackFromTheSyncedId_namesTheModeTheRouterRuns() {
        // The Cluster Manager used to turn the synced number into a name of its own and showed MANUAL for round-robin.
        assertEquals("ROUND-ROBIN", LoadBalanceMode.byId(LoadBalanceMode.ROUND_ROBIN.id()).label());
        assertEquals("LEAST-LOADED", LoadBalanceMode.byId(LoadBalanceMode.LEAST_LOADED.id()).label());
        assertEquals("MANUAL", LoadBalanceMode.byId(LoadBalanceMode.MANUAL.id()).label());
    }

    @Test
    void next_goesThroughEveryModeAndBack() {
        assertEquals(LoadBalanceMode.LEAST_LOADED, LoadBalanceMode.ROUND_ROBIN.next());
        assertEquals(LoadBalanceMode.MANUAL, LoadBalanceMode.LEAST_LOADED.next());
        assertEquals(LoadBalanceMode.ROUND_ROBIN, LoadBalanceMode.MANUAL.next());
    }
}
