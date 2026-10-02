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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusActivityTest {

    private BusActivity activity;

    @BeforeEach
    void setUp() {
        activity = new BusActivity();
    }

    @Test
    void moved_listsTheNewestFirst() {
        activity.moved(10L, "minecraft:iron_ore", 64L, false);
        activity.moved(20L, "minecraft:coal", 12L, true);

        final List<BusActivity.Entry> entries = activity.entries();
        assertEquals("minecraft:coal", entries.get(0).what());
        assertEquals(BusActivity.PARTIAL, entries.get(0).status());
        assertEquals(BusActivity.COMPLETED, entries.get(1).status());
    }

    @Test
    void held_keepsOneLineForAHoldThatGoesOn() {
        activity.held(10L, "minecraft:iron_ore", BusActivity.KEEPS, 16L);
        activity.held(30L, "minecraft:iron_ore", BusActivity.KEEPS, 16L);

        assertEquals(1, activity.entries().size());
        assertEquals(30L, activity.entries().getFirst().time());
    }

    @Test
    void held_marksANetworkWithNoRoomLocked() {
        activity.held(10L, "minecraft:iron_ore", BusActivity.FULL, 0L);

        assertEquals(BusActivity.LOCKED, activity.entries().getFirst().status());
    }

    @Test
    void entries_keepNoMoreThanTheirCount() {
        for (int i = 0; i < BusActivity.KEPT + 5; i++) {
            activity.moved(i, "minecraft:dirt", 1L, false);
        }

        assertEquals(BusActivity.KEPT, activity.entries().size());
        assertEquals(BusActivity.KEPT + 4, activity.entries().getFirst().time());
    }

    @Test
    void idleFor_countsFromTheLastMove() {
        assertTrue(activity.idleFor(100L, 20L), "a bus that never moved is idle");
        activity.moved(90L, "minecraft:dirt", 1L, false);
        activity.held(99L, "minecraft:dirt", BusActivity.KEEPS, 4L);

        assertFalse(activity.idleFor(100L, 20L), "it moved ten ticks ago");
        assertTrue(activity.idleFor(120L, 20L), "and thirty ticks after, it has been idle long enough");
    }
}
