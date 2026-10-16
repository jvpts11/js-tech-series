/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.CoreFeatures;
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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Writes the Core's veins as the datapack files the game reads: the veins' feature and its placement, and the biome
 * modifier {@code jscore:veins} that adds them to every biome, and stands them in for the game's scattered iron, copper
 * and gold and, when the server tries, for other mods' scattered ores of the common ores tag.
 */
public final class VeinsProvider extends DatapackBuiltinEntriesProvider {

    private static final ResourceLocation VEINS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "veins");
    /* The game's scattered iron, copper and gold, overworld and Nether. */
    private static final List<ResourceKey<PlacedFeature>> COVERED = List.of(OrePlacements.ORE_IRON_UPPER,
            OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL, OrePlacements.ORE_COPPER,
            OrePlacements.ORE_COPPER_LARGE, OrePlacements.ORE_GOLD, OrePlacements.ORE_GOLD_LOWER,
            OrePlacements.ORE_GOLD_EXTRA, OrePlacements.ORE_GOLD_NETHER, OrePlacements.ORE_GOLD_DELTAS);

    public VeinsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, entries(), Set.of(JsCore.MODID));
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":veins";
    }

    private static RegistrySetBuilder entries() {
        final ResourceKey<ConfiguredFeature<?, ?>> configured = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                VEINS);
        final ResourceKey<PlacedFeature> placed = ResourceKey.create(Registries.PLACED_FEATURE, VEINS);
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, context -> context.register(configured,
                        new ConfiguredFeature<>(CoreFeatures.VEINS.get(), NoneFeatureConfiguration.INSTANCE)))
                .add(Registries.PLACED_FEATURE, context -> context.register(placed, new PlacedFeature(
                        context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(configured), List.of())))
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    final HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
                    context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, VEINS),
                            (BiomeModifier) new VeinsBiomeModifier(features.getOrThrow(placed),
                                    HolderSet.direct(COVERED.stream().map(features::getOrThrow).toList()),
                                    Optional.of(Tags.Blocks.ORES)));
                });
    }
}
