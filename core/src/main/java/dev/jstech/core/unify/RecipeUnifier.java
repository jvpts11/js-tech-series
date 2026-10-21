/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.Set;

/**
 * Puts the one item that stands for each of a material's copies in a recipe's file, whatever the recipe's kind: every
 * item named by an {@code "item"} or an {@code "id"}, which is how recipes of every mod name an item they take and an
 * item they make, and every item named bare where a recipe says what it makes ({@code "result": "mymod:tin_ingot"}).
 * What a recipe makes comes out as the item that stands for its material, and what it takes is asked for as that
 * item, the only one players now come by. The recipe's conditions are left as they are, since they ask about the
 * items as the game has them. It works on the file before the game reads it, so it reaches recipes of kinds it does
 * not know.
 */
public final class RecipeUnifier {

    /** The keys a recipe names an item under, whether it takes it or makes it. */
    private static final Set<String> ITEM_KEYS = Set.of("item", "id");
    /** The keys a recipe says what it makes under, where an item may be named bare. */
    private static final Set<String> OUTPUT_KEYS = Set.of("result", "results", "output", "outputs");
    /** The key NeoForge's conditions are kept under. */
    private static final String CONDITIONS = "neoforge:conditions";

    private RecipeUnifier() {
    }

    /**
     * Puts in {@code recipe} the item {@code replace} maps each item it names to, by their ids; the file is changed
     * where it stands. Whether anything was put.
     */
    public static boolean rewrite(final JsonElement recipe, final Map<String, String> replace) {
        return !replace.isEmpty() && walk(recipe, replace, false);
    }

    /* Puts the items in {@code element}; {@code output} says whether it is a list of what a recipe makes. */
    private static boolean walk(final JsonElement element, final Map<String, String> replace, final boolean output) {
        if (element instanceof JsonObject object) {
            boolean changed = false;
            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                final String key = entry.getKey();
                if (key.equals(CONDITIONS)) {
                    continue;
                }
                final JsonElement value = entry.getValue();
                final boolean names = ITEM_KEYS.contains(key) || OUTPUT_KEYS.contains(key);
                if (names && value instanceof JsonPrimitive primitive && primitive.isString()) {
                    final String to = replace.get(primitive.getAsString());
                    if (to != null) {
                        entry.setValue(new JsonPrimitive(to));
                        changed = true;
                    }
                } else {
                    changed |= walk(value, replace, OUTPUT_KEYS.contains(key));
                }
            }
            return changed;
        }
        if (element instanceof JsonArray array) {
            boolean changed = false;
            for (int i = 0; i < array.size(); i++) {
                final JsonElement value = array.get(i);
                if (output && value instanceof JsonPrimitive primitive && primitive.isString()) {
                    final String to = replace.get(primitive.getAsString());
                    if (to != null) {
                        array.set(i, new JsonPrimitive(to));
                        changed = true;
                    }
                } else {
                    changed |= walk(value, replace, output);
                }
            }
            return changed;
        }
        return false;
    }
}
