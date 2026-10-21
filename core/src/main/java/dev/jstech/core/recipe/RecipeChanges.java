/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.unify.RecipeUnifier;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The recipes data packs change rather than add, read from {@code data/<namespace>/jstech/recipe_changes/*.json} as
 * the recipes load and done to them before the game reads them ({@link RecipeEdits}): taken away, given another item
 * in place of one, or put in another's place. A file may carry NeoForge's conditions and is passed over when they do
 * not hold; the files are read in the order of their ids.
 */
public final class RecipeChanges {

    /** The folder of the files, in a data pack's namespace. */
    public static final String FOLDER = "jstech/recipe_changes";
    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    private static final String JSON = ".json";

    private static volatile Applied applied = Applied.NONE;

    private RecipeChanges() {
    }

    /**
     * Does to {@code recipes}, every recipe's file by its id, what the files {@code resources} holds say, their
     * conditions read with {@code ops}. Called as the recipes load.
     */
    @ApiStatus.Internal
    public static void apply(final Map<ResourceLocation, JsonElement> recipes, final ResourceManager resources,
                             final DynamicOps<JsonElement> ops) {
        final RecipeEdits edits = RecipeEdits.of(files(resources, ops));
        if (edits.isEmpty()) {
            applied = Applied.NONE;
            return;
        }
        final List<String> removed = new ArrayList<>();
        recipes.entrySet().removeIf(recipe -> {
            final boolean taken = edits.removes(recipe.getKey().toString(), recipe.getValue());
            if (taken) {
                removed.add(recipe.getKey().toString());
            }
            return taken;
        });
        edits.replaced().forEach((id, recipe) -> {
            final ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null) {
                recipes.put(key, recipe.deepCopy());
            }
        });
        final List<String> swapped = new ArrayList<>();
        if (!edits.swaps().isEmpty()) {
            for (final Map.Entry<ResourceLocation, JsonElement> recipe : recipes.entrySet()) {
                if (RecipeUnifier.rewrite(recipe.getValue(), edits.swaps())) {
                    swapped.add(recipe.getKey().toString());
                }
            }
        }
        applied = new Applied(removed, List.copyOf(edits.replaced().keySet()), swapped);
        LOGGER.info("Recipe changes: {} recipes taken away, {} put in place, {} given other items", removed.size(),
                edits.replaced().size(), swapped.size());
    }

    /** What the files changed as the recipes last loaded, for the studio's recipe bench. */
    public static Applied applied() {
        return applied;
    }

    /**
     * What the recipe changes did as the recipes last loaded.
     *
     * @param removed  the recipes taken away, by id
     * @param replaced the recipes put in place, new or over one there was, by id
     * @param swapped  the recipes given other items, by id
     */
    public record Applied(List<String> removed, List<String> replaced, List<String> swapped) {

        /** Nothing changed. */
        public static final Applied NONE = new Applied(List.of(), List.of(), List.of());

        public Applied {
            removed = List.copyOf(removed);
            replaced = List.copyOf(replaced);
            swapped = List.copyOf(swapped);
        }
    }

    /* The files' changes whose conditions hold, in the order of their ids. */
    private static List<JsonObject> files(final ResourceManager resources, final DynamicOps<JsonElement> ops) {
        final Map<ResourceLocation, Resource> found = new TreeMap<>(resources.listResources(FOLDER,
                id -> id.getPath().endsWith(JSON)));
        final List<JsonObject> out = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, Resource> file : found.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                if (!(JsonParser.parseReader(reader) instanceof JsonObject object)) {
                    LOGGER.warn("The recipe changes {} are not an object and are passed over", file.getKey());
                    continue;
                }
                if (ICondition.conditionsMatched(ops, object)) {
                    out.add(object);
                }
            } catch (final IOException | JsonParseException unreadable) {
                LOGGER.warn("The recipe changes {} could not be read and are passed over", file.getKey(), unreadable);
            }
        }
        return out;
    }
}
