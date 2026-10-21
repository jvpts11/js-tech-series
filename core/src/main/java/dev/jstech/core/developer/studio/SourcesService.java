/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.recipe.RecipeChanges;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Where the server's data came from: each pack's file of a tag, with what it put in the tag and whether it replaced
 * what came before, the mod's or a data pack's; and the pack each recipe's file came from, with what the recipe
 * changes of a modpack did as the recipes loaded.
 */
final class SourcesService implements IStudioService {

    static final String NAME = "sources";
    /** The registries whose tags the studio reads, by the name a question gives them. */
    private static final Map<String, ResourceKey<? extends Registry<?>>> REGISTRIES = Map.of(
            "item", Registries.ITEM, "block", Registries.BLOCK, "fluid", Registries.FLUID,
            "entity_type", Registries.ENTITY_TYPE);
    private static final String VALUES = "values";
    private static final String REQUIRED_ID = "id";
    private static final String JSON = ".json";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ResourceManager data = player.server.getResourceManager();
        final CompoundTag out = new CompoundTag();
        if (args.contains(StudioKeys.TAG)) {
            out.put(StudioKeys.PACKS, tagSources(data, args.getString(StudioKeys.REGISTRY),
                    ResourceLocation.parse(args.getString(StudioKeys.TAG))));
        }
        if (args.contains(StudioKeys.RECIPES)) {
            final ListTag recipes = new ListTag();
            final ListTag asked = args.getList(StudioKeys.RECIPES, Tag.TAG_STRING);
            for (int i = 0; i < asked.size(); i++) {
                final ResourceLocation id = ResourceLocation.parse(asked.getString(i));
                final List<Resource> stack = data.getResourceStack(ResourceLocation.fromNamespaceAndPath(
                        id.getNamespace(), Registries.elementsDirPath(Registries.RECIPE) + "/" + id.getPath() + JSON));
                final CompoundTag recipe = new CompoundTag();
                recipe.putString(StudioKeys.ID, id.toString());
                recipe.putString(StudioKeys.PACK, stack.isEmpty() ? "" : stack.getLast().sourcePackId());
                recipes.add(recipe);
            }
            out.put(StudioKeys.RECIPES, recipes);
            final RecipeChanges.Applied applied = RecipeChanges.applied();
            out.put(StudioKeys.REMOVED, strings(applied.removed()));
            out.put(StudioKeys.REPLACED, strings(applied.replaced()));
            out.put(StudioKeys.SWAPPED, strings(applied.swapped()));
        }
        reply.accept(out);
    }

    /* Each pack's file of the tag, the lowest first, as the game reads them one over another. */
    private static ListTag tagSources(final ResourceManager data, final String registry, final ResourceLocation tag) {
        final ListTag packs = new ListTag();
        final ResourceKey<? extends Registry<?>> key = REGISTRIES.get(registry);
        if (key == null) {
            return packs;
        }
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(tag.getNamespace(),
                Registries.tagsDirPath(key) + "/" + tag.getPath() + JSON);
        for (final Resource resource : data.getResourceStack(file)) {
            final CompoundTag pack = new CompoundTag();
            pack.putString(StudioKeys.PACK, resource.sourcePackId());
            final ListTag entries = new ListTag();
            try (Reader reader = resource.openAsReader()) {
                if (JsonParser.parseReader(reader) instanceof JsonObject json) {
                    pack.putBoolean(StudioKeys.REPLACE, json.has(StudioKeys.REPLACE)
                            && json.get(StudioKeys.REPLACE).getAsBoolean());
                    if (json.has(VALUES)) {
                        for (final JsonElement value : json.getAsJsonArray(VALUES)) {
                            entries.add(StringTag.valueOf(value.isJsonObject()
                                    ? value.getAsJsonObject().get(REQUIRED_ID).getAsString() : value.getAsString()));
                        }
                    }
                }
            } catch (final IOException | RuntimeException unread) {
                pack.putString(StudioKeys.PROBLEM, String.valueOf(unread.getMessage()));
            }
            pack.put(StudioKeys.ENTRIES, entries);
            packs.add(pack);
        }
        return packs;
    }

    private static ListTag strings(final List<String> values) {
        final ListTag out = new ListTag();
        values.forEach(value -> out.add(StringTag.valueOf(value)));
        return out;
    }
}
