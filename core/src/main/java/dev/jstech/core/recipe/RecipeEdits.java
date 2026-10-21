/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the data packs' recipe changes say to do to the recipes before the game reads them: take some away (by id, all
 * of a mod's, every recipe making an item, every recipe of a kind), put one item for another in every recipe, and put a
 * recipe in another's place. Each file holds any of the three:
 *
 * <pre>{@code
 * {"remove": ["minecraft:stone_bricks", "othermod:*", {"output": "othermod:copper_gear"},
 *             {"type": "othermod:grinding"}],
 *  "swap": {"othermod:tin_ingot": "jscore:tin_ingot"},
 *  "replace": {"minecraft:torch": {"type": "minecraft:crafting_shaped", "...": "..."}}}
 * }</pre>
 *
 * <p>Pure: it reads the files' text as JSON, and is asked of recipes by their id and their file.
 *
 * @param removed  what to take away, each as its file wrote it
 * @param swaps    each item put in another's place, by id
 * @param replaced each recipe given another, by id
 */
public record RecipeEdits(List<JsonElement> removed, Map<String, String> swaps, Map<String, JsonElement> replaced) {

    /** Nothing to change. */
    public static final RecipeEdits NONE = new RecipeEdits(List.of(), Map.of(), Map.of());

    private static final String WHOLE_MOD = ":*";
    private static final Set<String> OUTPUT_KEYS = Set.of("result", "results", "output", "outputs");
    private static final Set<String> ITEM_KEYS = Set.of("item", "id");

    public RecipeEdits {
        removed = List.copyOf(removed);
        swaps = Map.copyOf(swaps);
        replaced = Map.copyOf(replaced);
    }

    /** The changes of every file of {@code files}, joined in their order: a later file's swap or recipe wins. */
    public static RecipeEdits of(final List<JsonObject> files) {
        final List<JsonElement> removed = new ArrayList<>();
        final Map<String, String> swaps = new LinkedHashMap<>();
        final Map<String, JsonElement> replaced = new LinkedHashMap<>();
        for (final JsonObject file : files) {
            if (file.get("remove") instanceof JsonArray remove) {
                remove.forEach(removed::add);
            }
            if (file.get("swap") instanceof JsonObject swap) {
                swap.entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive()) {
                        swaps.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });
            }
            if (file.get("replace") instanceof JsonObject replace) {
                replace.entrySet().forEach(entry -> replaced.put(entry.getKey(), entry.getValue()));
            }
        }
        return new RecipeEdits(removed, swaps, replaced);
    }

    /** Whether it changes nothing. */
    public boolean isEmpty() {
        return this.removed.isEmpty() && this.swaps.isEmpty() && this.replaced.isEmpty();
    }

    /** Whether the recipe {@code id}, written as {@code recipe}, is taken away. */
    public boolean removes(final String id, final JsonElement recipe) {
        for (final JsonElement pattern : this.removed) {
            if (matches(pattern, id, recipe)) {
                return true;
            }
        }
        return false;
    }

    /** The items {@code recipe} makes, by id, under any of the keys recipes say what they make under. */
    public static Set<String> made(final JsonElement recipe) {
        final Set<String> out = new HashSet<>();
        if (recipe instanceof JsonObject object) {
            for (final String key : OUTPUT_KEYS) {
                if (object.get(key) != null) {
                    collect(object.get(key), out);
                }
            }
        }
        return out;
    }

    private static boolean matches(final JsonElement pattern, final String id, final JsonElement recipe) {
        if (pattern instanceof JsonPrimitive written && written.isString()) {
            final String text = written.getAsString();
            return text.endsWith(WHOLE_MOD) ? id.startsWith(text.substring(0, text.length() - 1)) : id.equals(text);
        }
        if (!(pattern instanceof JsonObject object)) {
            return false;
        }
        if (object.get("output") instanceof JsonPrimitive output) {
            return made(recipe).contains(output.getAsString());
        }
        if (object.get("type") instanceof JsonPrimitive type && recipe instanceof JsonObject file
                && file.get("type") instanceof JsonPrimitive kind) {
            return kind.getAsString().equals(type.getAsString());
        }
        return false;
    }

    /* Every item named in what a recipe makes: bare, under an item's key, or deeper inside. */
    private static void collect(final JsonElement element, final Set<String> out) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            out.add(primitive.getAsString());
        } else if (element instanceof JsonArray array) {
            array.forEach(value -> collect(value, out));
        } else if (element instanceof JsonObject object) {
            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (ITEM_KEYS.contains(entry.getKey()) || entry.getValue().isJsonObject()) {
                    collect(entry.getValue(), out);
                }
            }
        }
    }
}
