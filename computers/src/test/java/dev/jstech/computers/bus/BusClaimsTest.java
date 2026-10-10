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

import dev.jstech.core.uuid.NetworkUuid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusClaimsTest {

    private BusClaims claims;
    private NetworkUuid network;

    @BeforeEach
    void setUp() {
        claims = new BusClaims();
        network = NetworkUuid.random();
    }

    @Test
    void mayGo_letsTheHigherPriorityGoFirst() {
        assertTrue(claims.mayGo(network, "minecraft:iron_ore", 5, 10L));
        assertFalse(claims.mayGo(network, "minecraft:iron_ore", 1, 10L), "the lower one waits");
        assertTrue(claims.mayGo(network, "minecraft:iron_ore", 5, 11L), "the higher one goes on");
    }

    @Test
    void mayGo_letsTheLowerGoOnceTheHigherStopsWanting() {
        claims.mayGo(network, "minecraft:iron_ore", 5, 10L);

        assertTrue(claims.mayGo(network, "minecraft:iron_ore", 1, 10L + BusClaims.LASTS + 1L));
    }

    @Test
    void mayGo_sweepsLapsedWantsOfNetworksThatAreGone() {
        claims.mayGo(NetworkUuid.random(), "minecraft:iron_ore", 5, 10L);
        claims.mayGo(NetworkUuid.random(), "minecraft:coal", 5, 20L);

        claims.mayGo(network, "minecraft:dirt", 1, 10L + BusClaims.SWEEP_EVERY + 5L);

        assertEquals(1, claims.size());
    }

    @Test
    void mayGo_keepsEachThingApart() {
        claims.mayGo(network, "minecraft:iron_ore", 5, 10L);

        assertTrue(claims.mayGo(network, "minecraft:coal", 1, 10L));
    }

    @Test
    void mayGo_keepsEachNetworkApart() {
        claims.mayGo(network, "minecraft:iron_ore", 5, 10L);

        assertTrue(claims.mayGo(NetworkUuid.random(), "minecraft:iron_ore", 1, 10L));
    }

    @Test
    void mayGo_letsEqualPrioritiesBothGo() {
        claims.mayGo(network, "minecraft:iron_ore", 3, 10L);

        assertTrue(claims.mayGo(network, "minecraft:iron_ore", 3, 10L));
    }
}
