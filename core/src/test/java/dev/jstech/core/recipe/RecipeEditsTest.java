/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeEditsTest {

    private static final JsonElement BRICKS = json("""
            {"type": "minecraft:crafting_shaped", "result": {"id": "minecraft:stone_bricks", "count": 4}}""");
    private static final JsonElement GEAR = json("""
            {"type": "othermod:pressing", "results": [{"item": {"id": "othermod:copper_gear"}}]}""");

    @Test
    void removes_byIdByModByWhatItMakesAndByKind() {
        final RecipeEdits edits = RecipeEdits.of(List.of(object("""
                {"remove": ["minecraft:stone_bricks", "thirdmod:*", {"output": "othermod:copper_gear"},
                            {"type": "minecraft:smelting"}]}""")));

        assertTrue(edits.removes("minecraft:stone_bricks", BRICKS), "by its id");
        assertTrue(edits.removes("thirdmod:anything", BRICKS), "all of a mod's");
        assertTrue(edits.removes("othermod:gear_from_plate", GEAR), "every recipe making an item");
        assertTrue(edits.removes("minecraft:glass", json("{\"type\": \"minecraft:smelting\"}")), "every one of a kind");
        assertFalse(edits.removes("minecraft:torch", json("{\"type\": \"minecraft:crafting_shaped\", "
                + "\"result\": {\"id\": \"minecraft:torch\"}}")), "and nothing else");
    }

    @Test
    void of_joinsTheFilesALaterSwapOrRecipeWinning() {
        final RecipeEdits edits = RecipeEdits.of(List.of(
                object("{\"swap\": {\"othermod:tin_ingot\": \"jscore:tin_ingot\"}, \"replace\": {\"a:b\": 1}}"),
                object("{\"swap\": {\"othermod:tin_ingot\": \"minecraft:iron_ingot\"}, \"replace\": {\"a:b\": 2}}")));

        assertEquals(Map.of("othermod:tin_ingot", "minecraft:iron_ingot"), edits.swaps());
        assertEquals(2, edits.replaced().get("a:b").getAsInt());
        assertFalse(edits.isEmpty());
        assertTrue(RecipeEdits.of(List.of(object("{}"))).isEmpty());
    }

    @Test
    void made_findsTheItemsUnderEveryKeyOfWhatIsMade() {
        assertEquals(Set.of("minecraft:stone_bricks"), RecipeEdits.made(BRICKS));
        assertEquals(Set.of("othermod:copper_gear"), RecipeEdits.made(GEAR));
        assertEquals(Set.of("othermod:a", "othermod:b"), RecipeEdits.made(json("""
                {"output": "othermod:a", "outputs": [{"id": "othermod:b", "count": 2}]}""")));
    }

    private static JsonElement json(final String text) {
        return JsonParser.parseString(text);
    }

    private static JsonObject object(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
