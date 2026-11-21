/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class McpResultTest {

    @Test
    void with_addsToWhatWasFoundAndWritesItsWordsAgain() {
        final McpResult result = McpResult.json(object("{slot: 3, game_tick: 'the tool says'}"))
                .with(object("{game_tick: 9, changed: {}}"));
        assertEquals("the tool says", result.structured().get("game_tick").getAsString(),
                "the tool's own fields keep theirs");
        assertEquals(0, result.structured().getAsJsonObject("changed").size(), "a field the tool has not is added");
        assertEquals(1, result.content().size());
        assertEquals(result.structured(), object(result.content().get(0).getAsJsonObject().get("text")
                .getAsString()));
    }

    @Test
    void with_givesAResultOfWordsWhatWasToldAsItsOwnAfterItsWords() {
        final McpResult result = McpResult.text("done").with(object("{game_tick: 4}"));
        assertEquals(4, result.structured().get("game_tick").getAsInt());
        assertEquals(2, result.content().size());
        assertEquals("done", result.content().get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("{\"game_tick\":4}", result.content().get(1).getAsJsonObject().get("text").getAsString());
    }

    @Test
    void with_keepsAPicturesPictureFirst() {
        final McpResult result = McpResult.picture(new byte[]{1}, object("{width: 960}"))
                .with(object("{game_tick: 2}"));
        assertEquals("image", result.content().get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(2, result.content().size());
        assertEquals(960, result.structured().get("width").getAsInt());
    }

    @Test
    void with_nothingToldIsTheSameResult() {
        final McpResult result = McpResult.text("done");
        assertSame(result, result.with(new JsonObject()));
    }

    private static JsonObject object(final String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
