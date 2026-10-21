/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.creature.CreaturesBiomeModifier;
import dev.jstech.core.worldgen.CoreFeatures;
import dev.jstech.core.worldgen.EverywhereBiomeModifier;
import dev.jstech.core.worldgen.vein.VeinsBiomeModifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Writes the Core's underground as the datapack files the game reads: the features of the veins and of the reservoirs
 * and their placements; the biome modifier {@code jscore:veins}, which adds the veins to every biome and stands them in
 * for the game's scattered iron, copper and gold and, when the server tries, for other mods' scattered ores of the
 * common ores tag; a {@code jscore:everywhere} that adds the reservoirs to every biome, before the ores; and the biome
 * modifier {@code jscore:creatures}, which adds where the creatures of the data appear, though they live above ground.
 */
public final class UndergroundProvider extends DatapackBuiltinEntriesProvider {

    private static final ResourceLocation VEINS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "veins");
    private static final ResourceLocation RESERVOIRS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "reservoirs");
    private static final ResourceLocation CREATURES = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "creatures");
    /* The game's scattered iron, copper and gold, overworld and Nether. */
    private static final List<ResourceKey<PlacedFeature>> COVERED = List.of(OrePlacements.ORE_IRON_UPPER,
            OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL, OrePlacements.ORE_COPPER,
            OrePlacements.ORE_COPPER_LARGE, OrePlacements.ORE_GOLD, OrePlacements.ORE_GOLD_LOWER,
            OrePlacements.ORE_GOLD_EXTRA, OrePlacements.ORE_GOLD_NETHER, OrePlacements.ORE_GOLD_DELTAS);

    public UndergroundProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, entries(), Set.of(JsCore.MODID));
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":underground";
    }

    private static RegistrySetBuilder entries() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    context.register(configured(VEINS), new ConfiguredFeature<>(CoreFeatures.VEINS.get(),
                            NoneFeatureConfiguration.INSTANCE));
                    context.register(configured(RESERVOIRS), new ConfiguredFeature<>(CoreFeatures.RESERVOIRS.get(),
                            NoneFeatureConfiguration.INSTANCE));
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    final HolderGetter<ConfiguredFeature<?, ?>> configured =
                            context.lookup(Registries.CONFIGURED_FEATURE);
                    context.register(placed(VEINS), new PlacedFeature(configured.getOrThrow(configured(VEINS)),
                            List.of()));
                    context.register(placed(RESERVOIRS), new PlacedFeature(
                            configured.getOrThrow(configured(RESERVOIRS)), List.of()));
                })
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    final HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
                    context.register(modifier(VEINS), (BiomeModifier) new VeinsBiomeModifier(
                            features.getOrThrow(placed(VEINS)),
                            HolderSet.direct(COVERED.stream().map(features::getOrThrow).toList()),
                            Optional.of(Tags.Blocks.ORES)));
                    context.register(modifier(RESERVOIRS), (BiomeModifier) new EverywhereBiomeModifier(
                            HolderSet.direct(features.getOrThrow(placed(RESERVOIRS))),
                            GenerationStep.Decoration.UNDERGROUND_STRUCTURES));
                    context.register(modifier(CREATURES), (BiomeModifier) CreaturesBiomeModifier.INSTANCE);
                });
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(final ResourceLocation id) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id);
    }

    private static ResourceKey<PlacedFeature> placed(final ResourceLocation id) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id);
    }

    private static ResourceKey<BiomeModifier> modifier(final ResourceLocation id) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, id);
    }
}
