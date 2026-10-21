/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecipeUnifierTest {

    private static final Map<String, String> TIN = Map.of("othermod:tin_ingot", "jscore:tin_ingot",
            "othermod:tin_dust", "jscore:tin_dust");

    @Test
    void rewrite_putsTheStandingItemInWhatARecipeMakes() {
        final JsonElement recipe = json("""
                {"type": "minecraft:smelting", "ingredient": {"tag": "c:dusts/tin"},
                 "result": {"id": "othermod:tin_ingot", "count": 2}}
                """);

        assertTrue(RecipeUnifier.rewrite(recipe, TIN));
        assertEquals(json("""
                {"type": "minecraft:smelting", "ingredient": {"tag": "c:dusts/tin"},
                 "result": {"id": "jscore:tin_ingot", "count": 2}}
                """), recipe);
    }

    @Test
    void rewrite_asksForTheStandingItemWhereARecipeTakesACopy() {
        final JsonElement recipe = json("""
                {"type": "minecraft:crafting_shaped", "pattern": ["T", "S"],
                 "key": {"T": {"item": "othermod:tin_ingot"}, "S": [{"item": "minecraft:stick"}]},
                 "result": {"id": "minecraft:lantern"}}
                """);

        assertTrue(RecipeUnifier.rewrite(recipe, TIN));
        assertEquals("jscore:tin_ingot", recipe.getAsJsonObject().getAsJsonObject("key").getAsJsonObject("T")
                .get("item").getAsString());
        assertEquals("minecraft:stick", recipe.getAsJsonObject().getAsJsonObject("key").getAsJsonArray("S").get(0)
                .getAsJsonObject().get("item").getAsString());
    }

    @Test
    void rewrite_reachesItemsNamedBareAndInListsOfWhatIsMade() {
        final JsonElement recipe = json("""
                {"type": "othermod:grinding", "input": {"item": "othermod:tin_ingot", "count": 1},
                 "results": ["othermod:tin_dust", {"item": {"id": "othermod:tin_dust"}, "chance": 0.5}],
                 "output": "othermod:tin_ingot"}
                """);

        assertTrue(RecipeUnifier.rewrite(recipe, TIN));
        assertEquals(json("""
                {"type": "othermod:grinding", "input": {"item": "jscore:tin_ingot", "count": 1},
                 "results": ["jscore:tin_dust", {"item": {"id": "jscore:tin_dust"}, "chance": 0.5}],
                 "output": "jscore:tin_ingot"}
                """), recipe);
    }

    @Test
    void rewrite_leavesTheConditionsAsTheyAre() {
        final JsonElement recipe = json("""
                {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": "othermod:tin_ingot"}],
                 "type": "minecraft:stonecutting", "ingredient": {"item": "minecraft:stone"},
                 "result": {"id": "othermod:tin_ingot"}}
                """);

        RecipeUnifier.rewrite(recipe, TIN);

        assertEquals("othermod:tin_ingot", recipe.getAsJsonObject().getAsJsonArray("neoforge:conditions").get(0)
                .getAsJsonObject().get("item").getAsString());
        assertEquals("jscore:tin_ingot", recipe.getAsJsonObject().getAsJsonObject("result").get("id")
                .getAsString());
    }

    @Test
    void rewrite_leavesBareWordsThatAreNotWhatARecipeMakes() {
        final JsonElement recipe = json("""
                {"type": "othermod:tin_ingot", "group": "othermod:tin_ingot", "pattern": ["othermod:tin_ingot"],
                 "result": {"id": "minecraft:stone"}}
                """);

        assertFalse(RecipeUnifier.rewrite(recipe, TIN));
        assertEquals("othermod:tin_ingot", recipe.getAsJsonObject().get("type").getAsString());
        assertEquals("othermod:tin_ingot", recipe.getAsJsonObject().getAsJsonArray("pattern").get(0).getAsString());
    }

    @Test
    void rewrite_changesNothingWithAnEmptyPlan() {
        final JsonElement recipe = json("""
                {"type": "minecraft:smelting", "result": {"id": "othermod:tin_ingot"}}
                """);

        assertFalse(RecipeUnifier.rewrite(recipe, Map.of()));
        assertEquals("othermod:tin_ingot", recipe.getAsJsonObject().getAsJsonObject("result").get("id")
                .getAsString());
    }

    private static JsonElement json(final String text) {
        return JsonParser.parseString(text);
    }
}
