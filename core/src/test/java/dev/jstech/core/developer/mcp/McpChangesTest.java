/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class McpChangesTest {

    @Test
    void told_givesTheTickAfterAndTheTicksThatWentBy() {
        final JsonObject told = McpChanges.told(glimpse("{game_tick: 100, paused: false}"),
                glimpse("{game_tick: 140, paused: false}"));
        assertEquals(140, told.get(McpChanges.TICK).getAsLong());
        assertEquals(40, told.get(McpChanges.TICKS_TAKEN).getAsLong());
        assertFalse(told.has(McpChanges.CHANGED), "nothing changed but the time: " + told);
    }

    @Test
    void told_givesEveryReadingThatChangedWithWhatItWasAndIs() {
        final JsonObject told = McpChanges.told(glimpse("{game_tick: 1, screen: null, slot: 2, place: {x: 1, y: 2}}"),
                glimpse("{game_tick: 2, screen: {title: 'Chest'}, slot: 2, place: {y: 2, x: 1}}"));
        final JsonObject changed = told.getAsJsonObject(McpChanges.CHANGED);
        assertEquals(1, changed.size(), "only the screen changed, the place in another order is the same: " + told);
        assertEquals(JsonNull.INSTANCE, changed.getAsJsonObject("screen").get(McpChanges.FROM));
        assertEquals("Chest", changed.getAsJsonObject("screen").getAsJsonObject(McpChanges.TO).get("title")
                .getAsString());
    }

    @Test
    void told_writesAReadingMissingOnOneSideAsNull() {
        final JsonObject told = McpChanges.told(glimpse("{game_tick: 1, riding: 'jsspace:seat'}"),
                glimpse("{game_tick: 2, aim: 'stone'}"));
        final JsonObject changed = told.getAsJsonObject(McpChanges.CHANGED);
        assertEquals(JsonNull.INSTANCE, changed.getAsJsonObject("riding").get(McpChanges.TO));
        assertEquals(JsonNull.INSTANCE, changed.getAsJsonObject("aim").get(McpChanges.FROM));
    }

    @Test
    void told_isOnlyTheTimeWithNothingBeforeAndNothingWithNothingAfter() {
        final JsonObject first = McpChanges.told(new JsonObject(), glimpse("{game_tick: 7, slot: 1}"));
        assertEquals(7, first.get(McpChanges.TICK).getAsLong());
        assertFalse(first.has(McpChanges.TICKS_TAKEN) || first.has(McpChanges.CHANGED), first.toString());
        assertTrue(McpChanges.told(glimpse("{game_tick: 7}"), new JsonObject()).isEmpty());
    }

    @Test
    void told_tellsNoTimeWhenTheTickIsNoNumber() {
        final JsonObject told = McpChanges.told(glimpse("{game_tick: 'soon', slot: 1}"),
                glimpse("{game_tick: 'later', slot: 1}"));
        assertTrue(told.isEmpty(), "a tick that is no number is neither the time nor a change: " + told);
    }

    private static JsonObject glimpse(final String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
