/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.content.RecipeMachineFiles;
import dev.jstech.core.data.DataRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Which machines run which recipe types: the Recipe Book's way of turning "a smelting recipe" into "a furnace,
 * a blast furnace or a smoker". The map is data: every file under {@code data/<namespace>/recipe_machines/}
 * is a JSON object whose keys are recipe type ids and whose values are a machine block id or a list of them, so a
 * pack (or another mod) can teach the Studio about its machines without code. Types nobody maps fall back to the
 * generic category of their recipe type.
 *
 * <p>The order of a type's machines is what the Studio picks by default when the network declares none of
 * them, so it must not depend on which mod's file was read first: the machines from the recipe type's own
 * namespace come first (a vanilla smelt pairs with the furnace, whatever else can smelt), then the rest in
 * the order of the files' ids.
 */
public final class RecipeMachines {

    /* A machine block id, or a list of them. */
    private static final Codec<List<String>> MACHINES = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(List::of, list -> list), Either::right);
    /* The files, each read whenever the server loads its data. */
    private static final DataRegistry<Map<String, List<String>>> FILES = DataRegistry.builder(
                    ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "recipe_machines"),
                    RecipeMachineFiles.FOLDER, Codec.unboundedMap(Codec.STRING, MACHINES))
            .onReload(RecipeMachines::merge)
            .register();

    private static volatile Map<String, List<String>> machinesByType = Map.of();

    private RecipeMachines() {
    }

    /** Declares the files, before the server first loads its data. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The machine block ids that run {@code recipeTypeId}, in the order the data listed them; empty when unmapped. */
    public static List<String> machinesFor(final String recipeTypeId) {
        return machinesByType.getOrDefault(recipeTypeId, List.of());
    }

    /** Every mapped recipe type id. */
    public static List<String> mappedTypes() {
        return new ArrayList<>(machinesByType.keySet());
    }

    /** Replaces the map wholesale; what a test can do without a datapack. */
    public static void replace(final Map<String, List<String>> map) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> e : map.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        machinesByType = Collections.unmodifiableMap(copy);
    }

    /**
     * Reads every {@code recipe_machines} file the resource manager can see and replaces the map with them:
     * what a data reload does, callable directly to check what a datapack declares.
     */
    public static void reload(final ResourceManager manager, final HolderLookup.Provider registries) {
        FILES.reload(manager, registries);
    }

    private static void merge(final Map<ResourceLocation, Map<String, List<String>>> files) {
        final Map<String, List<String>> merged = new LinkedHashMap<>();
        final List<Map.Entry<ResourceLocation, Map<String, List<String>>>> ordered = new ArrayList<>(files.entrySet());
        ordered.sort(Map.Entry.comparingByKey((a, b) -> a.toString().compareTo(b.toString())));
        for (final Map.Entry<ResourceLocation, Map<String, List<String>>> file : ordered) {
            for (final Map.Entry<String, List<String>> entry : file.getValue().entrySet()) {
                final List<String> machines = merged.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                for (final String machine : entry.getValue()) {
                    if (!machines.contains(machine)) {
                        machines.add(machine);
                    }
                }
            }
        }
        for (final Map.Entry<String, List<String>> entry : merged.entrySet()) {
            final String namespace = namespaceOf(entry.getKey());
            entry.getValue().sort((a, b) -> Boolean.compare(!namespaceOf(a).equals(namespace),
                    !namespaceOf(b).equals(namespace)));
        }
        replace(merged);
        JsComputers.LOGGER.info("Loaded {} recipe type to machine mappings", merged.size());
    }

    private static String namespaceOf(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }
}
