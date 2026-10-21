/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.core.variant.Variants;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The last step of a mod's data generation: every block and item the mod registered is looked for in what the steps
 * before wrote and in the mod's own resources, and the build fails, saying what is missing and how to put it right,
 * when one has no English name, no block state, no model, a model whose texture is nowhere, or no loot table while
 * it drops something. What would otherwise show in the game as a magenta cube or a raw key is found here.
 *
 * <p>Models and textures are followed within the mod's own namespace and the game's; another mod's are its own run's
 * to check.
 */
final class ContentCheckProvider implements DataProvider {

    private final PackOutput output;
    private final ExistingFileHelper existing;
    private final String modid;

    private static final String MINECRAFT = "minecraft";

    ContentCheckProvider(final PackOutput output, final ExistingFileHelper existing, final String modid) {
        this.output = output;
        this.existing = existing;
        this.modid = modid;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<DeclarationError> missing = new ArrayList<>();
        final Set<String> english = english();
        // The things of the materials' uses are named in code and drawn, tagged and given drops as the game loads.
        final Set<ResourceLocation> madeAsTheGameLoads = new HashSet<>();
        MaterialUses.registered().forEach(thing -> madeAsTheGameLoads.add(thing.id()));
        MaterialLiquids.registered().forEach(liquid -> {
            madeAsTheGameLoads.add(liquid.id());
            madeAsTheGameLoads.add(liquid.bucketId());
        });
        CatalogueItems.registered().forEach(entry -> madeAsTheGameLoads.add(entry.id()));
        Variants.registered().forEach(variant -> madeAsTheGameLoads.add(variant.id()));
        for (final Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            if (id.getNamespace().equals(modid) && !madeAsTheGameLoads.contains(id)) {
                checkBlock(id, entry.getValue(), english, missing);
            }
        }
        for (final Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            if (id.getNamespace().equals(modid) && !madeAsTheGameLoads.contains(id)) {
                checkItem(id, entry.getValue(), english, missing);
            }
        }
        if (missing.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("The data generation of " + modid
                + " found " + missing.size() + " thing(s) the game would show broken:" + missing.stream()
                .map(DeclarationError::getMessage).collect(Collectors.joining("\n  ", "\n  ", ""))));
    }

    @Override
    public String getName() {
        return modid + ":content_check";
    }

    private void checkBlock(final ResourceLocation id, final Block block, final Set<String> english,
                            final List<DeclarationError> missing) {
        if (!english.contains(block.getDescriptionId())) {
            missing.add(DeclarationError.of(modid, "the block " + id, "has no English name",
                    "name it with named(...) where it is declared, or add " + block.getDescriptionId()
                    + " to the mod's en_us.json"));
        }
        final JsonObject states = json(file(id, "blockstates/", ".json"), PackType.CLIENT_RESOURCES);
        if (states == null) {
            missing.add(DeclarationError.of(modid, "the block " + id, "has no block state",
                    "give it a look where it is declared, or add assets/" + id.getNamespace() + "/blockstates/"
                    + id.getPath() + ".json"));
        } else {
            for (final String model : modelsNamedBy(states)) {
                checkModel(id, true, ResourceLocation.parse(model), missing, new HashSet<>());
            }
        }
        final ResourceKey<LootTable> table = block.getLootTable();
        if (table != BuiltInLootTables.EMPTY && !exists(file(table.location(), "loot_table/", ".json"),
                PackType.SERVER_DATA)) {
            missing.add(DeclarationError.of(modid, "the block " + id, "drops something but has no loot table",
                    "say what it drops with drops(...) where it is declared, add data/" + id.getNamespace()
                    + "/loot_table/" + table.location().getPath() + ".json, or give it noLootTable() if it drops"
                    + " nothing"));
        }
    }

    private void checkItem(final ResourceLocation id, final Item item, final Set<String> english,
                           final List<DeclarationError> missing) {
        if (!english.contains(item.getDescriptionId())) {
            missing.add(DeclarationError.of(modid, "the item " + id, "has no English name",
                    "name it with named(...) where it is declared, or add " + item.getDescriptionId()
                    + " to the mod's en_us.json"));
        }
        checkModel(id, false, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath()),
                missing, new HashSet<>());
    }

    /**
     * Follows a model of the block or item {@code owner} and its parents within the namespaces this run can see,
     * checking each texture it names.
     */
    private void checkModel(final ResourceLocation owner, final boolean block, final ResourceLocation model,
                            final List<DeclarationError> missing, final Set<ResourceLocation> seen) {
        if (!seen.add(model) || !checkable(model)) {
            return;
        }
        final JsonObject json = json(file(model, "models/", ".json"), PackType.CLIENT_RESOURCES);
        if (json == null) {
            missing.add(DeclarationError.of(modid, (block ? "the block " : "the item ") + owner,
                    "is drawn with the model " + model
                    + ", which is neither written by the data generation nor among the resources",
                    "give it a look where it is declared, or add assets/" + model.getNamespace() + "/models/"
                    + model.getPath() + ".json"));
            return;
        }
        if (json.has("textures") && json.get("textures").isJsonObject()) {
            for (final Map.Entry<String, JsonElement> texture : json.getAsJsonObject("textures").entrySet()) {
                final String named = texture.getValue().isJsonPrimitive() ? texture.getValue().getAsString() : "";
                if (named.isEmpty() || named.startsWith("#")) {
                    continue;
                }
                final ResourceLocation location = ResourceLocation.parse(named);
                // A texture a pack makes as the player's game loads is found there, not in the mod's files.
                if (checkable(location) && !RuntimePacks.promised(location)
                        && !exists(file(location, "textures/", ".png"), PackType.CLIENT_RESOURCES)) {
                    missing.add(DeclarationError.of(modid, (block ? "the block " : "the item ") + owner,
                            "is drawn with the model " + model + ", which names the texture " + location
                            + ", and there is no such picture",
                            "add assets/" + location.getNamespace() + "/textures/" + location.getPath()
                            + ".png, or name a texture that exists"));
                }
            }
        }
        if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
            final ResourceLocation parent = ResourceLocation.parse(json.get("parent").getAsString());
            // The game's built-in models (generated, entity) are drawn in code and have no file.
            if (!parent.getPath().startsWith("builtin/")) {
                checkModel(owner, block, parent, missing, seen);
            }
        }
    }

    /** Every model a block state names, in its variants or its multipart cases. */
    private static Set<String> modelsNamedBy(final JsonObject states) {
        final Set<String> models = new LinkedHashSet<>();
        if (states.has("variants")) {
            for (final Map.Entry<String, JsonElement> variant : states.getAsJsonObject("variants").entrySet()) {
                collectModels(variant.getValue(), models);
            }
        }
        if (states.has("multipart")) {
            for (final JsonElement part : states.getAsJsonArray("multipart")) {
                if (part.isJsonObject() && part.getAsJsonObject().has("apply")) {
                    collectModels(part.getAsJsonObject().get("apply"), models);
                }
            }
        }
        return models;
    }

    private static void collectModels(final JsonElement applied, final Set<String> models) {
        if (applied.isJsonArray()) {
            applied.getAsJsonArray().forEach(each -> collectModels(each, models));
        } else if (applied.isJsonObject() && applied.getAsJsonObject().has("model")) {
            models.add(applied.getAsJsonObject().get("model").getAsString());
        }
    }

    /** Every key the mod's English names: what the data generation wrote and what the mod keeps by hand. */
    private Set<String> english() {
        final Set<String> keys = new HashSet<>();
        final JsonObject lang = json(ResourceLocation.fromNamespaceAndPath(modid, "lang/en_us.json"),
                PackType.CLIENT_RESOURCES);
        if (lang != null) {
            keys.addAll(lang.keySet());
        }
        final JsonObject own = existingJson(ResourceLocation.fromNamespaceAndPath(modid, "lang/en_us.json"),
                PackType.CLIENT_RESOURCES);
        if (own != null) {
            keys.addAll(own.keySet());
        }
        return keys;
    }

    /** A namespace this run can see whole: the mod's own, and the game's. */
    private boolean checkable(final ResourceLocation location) {
        return location.getNamespace().equals(modid) || location.getNamespace().equals(MINECRAFT);
    }

    private static ResourceLocation file(final ResourceLocation id, final String folder, final String suffix) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), folder + id.getPath() + suffix);
    }

    private boolean exists(final ResourceLocation file, final PackType type) {
        return Files.isRegularFile(written(file, type)) || existing.exists(file, type);
    }

    /** The file as the data generation wrote it, or else as the resources hold it; null when neither has it. */
    private @Nullable JsonObject json(final ResourceLocation file, final PackType type) {
        final Path path = written(file, type);
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (final IOException | RuntimeException unreadable) {
                return null;
            }
        }
        return existingJson(file, type);
    }

    private @Nullable JsonObject existingJson(final ResourceLocation file, final PackType type) {
        if (!existing.exists(file, type)) {
            return null;
        }
        try (Reader reader = existing.getResource(file, type).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (final IOException | RuntimeException unreadable) {
            return null;
        }
    }

    private Path written(final ResourceLocation file, final PackType type) {
        final PackOutput.Target target = type == PackType.CLIENT_RESOURCES ? PackOutput.Target.RESOURCE_PACK
                : PackOutput.Target.DATA_PACK;
        return output.getOutputFolder(target).resolve(file.getNamespace()).resolve(file.getPath());
    }
}
