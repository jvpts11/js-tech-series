/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.pack.ContentPacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * The generated reference, taken from the game as it runs: every kind of data the Core's registries read, with the
 * folder it lives in and one of the world's own files as the example; every data map of every mod, with a few of its
 * values; every kind of recipe, with one of the world's recipes written out; the conditions the Core gives data files;
 * what a content pack declares; and every id the series' mods and the content packs registered. The examples are
 * written by each thing's own codec, so the reference always says what the game reads.
 */
public final class References {

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** How many of a data map's values its example shows. */
    private static final int MAP_EXAMPLES = 3;
    private static final String MINECRAFT = "minecraft";

    private References() {
    }

    /** The reference's sections, as the game with {@code server} running holds them now. */
    public static List<ReferenceSite.Section> sections(final MinecraftServer server) {
        final DynamicOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return List.of(dataKinds(ops), dataMaps(server, ops), recipes(server, ops), conditions(), contentPacks(),
                ids());
    }

    private static ReferenceSite.Section dataKinds(final DynamicOps<JsonElement> ops) {
        final List<ReferenceSite.Entry> entries = new ArrayList<>();
        for (final DataRegistry<?> registry : DataRegistries.all()) {
            entries.add(new ReferenceSite.Entry(registry.id().toString(), "data/<namespace>/" + registry.folder()
                    + "/<name>.json", firstOf(registry, ops)));
        }
        entries.sort((one, other) -> one.id().compareTo(other.id()));
        return new ReferenceSite.Section("data", "Kinds of data", "What the Core's registries read from data packs, "
                + "where each file goes, and one of the world's own files as an example.", entries);
    }

    private static ReferenceSite.Section dataMaps(final MinecraftServer server, final DynamicOps<JsonElement> ops) {
        final List<ReferenceSite.Entry> entries = new ArrayList<>();
        RegistryManager.getDataMaps().forEach((registry, maps) -> maps.forEach((id, map) -> entries.add(
                new ReferenceSite.Entry(id.toString(), "data/" + id.getNamespace() + "/data_maps/"
                        + folderOf(registry.location()) + "/" + id.getPath() + ".json",
                        valuesOf(server, map, ops)))));
        entries.sort((one, other) -> one.id().compareTo(other.id()));
        return new ReferenceSite.Section("data_maps", "Data maps", "What every mod attaches as data to the game's "
                + "blocks, items, creatures and biomes, where each file goes, and a few of its values.", entries);
    }

    private static ReferenceSite.Section recipes(final MinecraftServer server, final DynamicOps<JsonElement> ops) {
        final Map<String, Recipe<?>> firsts = new TreeMap<>();
        for (final RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            final ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (type != null) {
                firsts.putIfAbsent(type.toString(), holder.value());
            }
        }
        final List<ReferenceSite.Entry> entries = new ArrayList<>();
        for (final RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            final String id = String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(type));
            final Recipe<?> example = firsts.get(id);
            entries.add(new ReferenceSite.Entry(id, "data/<namespace>/recipe/<name>.json", example == null ? ""
                    : write(Recipe.CODEC, example, ops)));
        }
        entries.sort((one, other) -> one.id().compareTo(other.id()));
        return new ReferenceSite.Section("recipes", "Kinds of recipe", "Every kind of recipe, and one of the world's "
                + "recipes of it written out, as its file holds it.", entries);
    }

    private static ReferenceSite.Section conditions() {
        return new ReferenceSite.Section("conditions", "Conditions on data", "What a data file lists under "
                + "neoforge:conditions to load only while each holds, beside NeoForge's own.", List.of(
                new ReferenceSite.Entry("jscore:setting", "neoforge:conditions", """
                        {"type": "jscore:setting", "file": "jstech-balance",
                         "setting": "moving_structures.physics_tool_recipe", "value": true}"""),
                new ReferenceSite.Entry("jscore:simple_path", "neoforge:conditions", """
                        {"type": "jscore:simple_path", "mod": "jsc"}"""),
                new ReferenceSite.Entry("neoforge:mod_loaded", "neoforge:conditions", """
                        {"type": "neoforge:mod_loaded", "modid": "mekanism"}""")));
    }

    private static ReferenceSite.Section contentPacks() {
        final String where = "jstech/content/<name>.json, or jstech_content/<name>.json in a mod";
        return new ReferenceSite.Section("content", "Content packs", "What a content pack declares, registered as the "
                + "game starts with no Java.", List.of(
                new ReferenceSite.Entry("block", where, """
                        {"type": "block", "id": "mypack:copper_lamp", "name": "Copper Lamp", "strength": 3,
                         "sound": "metal", "light": 15, "tool": "pickaxe", "needs": "stone"}"""),
                new ReferenceSite.Entry("item", where, """
                        {"type": "item", "id": "mypack:token", "name": "Token", "stack": 16}"""),
                new ReferenceSite.Entry("material", where, """
                        {"type": "material", "id": "mypack:mythril", "name": "Mythril", "kind": "metal",
                         "colour": "#7AD7FF", "forms": ["ingot", "dust", "block"], "ores": ["stone"]}"""),
                new ReferenceSite.Entry("processing", where, """
                        {"type": "processing", "id": "mypack:smelting", "name": "Smelting",
                         "machine": "mypack:smelter"}"""),
                new ReferenceSite.Entry("machine", where, """
                        {"type": "machine", "id": "mypack:smelter", "name": "Smelter"}"""),
                new ReferenceSite.Entry("tab", where, """
                        {"type": "tab", "id": "mypack:main", "name": "My Pack", "icon": "mypack:token"}""")));
    }

    private static ReferenceSite.Section ids() {
        final Set<String> namespaces = new TreeSet<>(ContentPacks.namespaces());
        for (final ModContent content : ModContent.all()) {
            namespaces.add(content.modid());
        }
        final List<ReferenceSite.Entry> entries = new ArrayList<>();
        for (final String namespace : namespaces) {
            final Map<String, List<String>> byRegistry = new TreeMap<>();
            BuiltInRegistries.REGISTRY.forEach(registry -> registry.keySet().forEach(id -> {
                if (id.getNamespace().equals(namespace)) {
                    byRegistry.computeIfAbsent(registry.key().location().toString(), unused -> new ArrayList<>())
                            .add(id.toString());
                }
            }));
            byRegistry.forEach((registry, ids) -> {
                ids.sort(null);
                entries.add(new ReferenceSite.Entry(namespace + " " + registry, registry, String.join("\n", ids)));
            });
        }
        return new ReferenceSite.Section("ids", "Ids", "Every id the series' mods and the content packs registered, "
                + "by namespace and by registry.", entries);
    }

    private static <T> String firstOf(final DataRegistry<T> registry, final DynamicOps<JsonElement> ops) {
        return new TreeMap<>(registry.entries()).values().stream().findFirst()
                .map(value -> write(registry.codec(), value, ops)).orElse("");
    }

    @SuppressWarnings("unchecked")
    private static <R, V> String valuesOf(final MinecraftServer server, final DataMapType<R, V> map,
                                          final DynamicOps<JsonElement> ops) {
        final Optional<Registry<R>> registry = server.registryAccess().registry(
                (ResourceKey<? extends Registry<R>>) map.registryKey());
        if (registry.isEmpty()) {
            return "";
        }
        final JsonObject values = new JsonObject();
        final Map<String, V> sorted = new TreeMap<>();
        registry.get().getDataMap(map).forEach((key, value) -> sorted.put(key.location().toString(), value));
        sorted.entrySet().stream().limit(MAP_EXAMPLES).forEach(value -> map.codec().encodeStart(ops,
                value.getValue()).result().ifPresent(json -> values.add(value.getKey(), json)));
        if (values.isEmpty()) {
            return "";
        }
        final JsonObject file = new JsonObject();
        file.add("values", values);
        return PRETTY.toJson(file);
    }

    private static <T> String write(final Codec<T> codec, final T value, final DynamicOps<JsonElement> ops) {
        return codec.encodeStart(ops, value).result().map(PRETTY::toJson).orElse("");
    }

    /* The folder a registry's data maps go in: the path of the game's own, and namespace/path for any other. */
    private static String folderOf(final ResourceLocation registry) {
        return registry.getNamespace().equals(MINECRAFT) ? registry.getPath()
                : registry.getNamespace() + "/" + registry.getPath();
    }
}
