/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.worldgen.CoreFeatures;
import dev.jstech.core.worldgen.WorldGen;
import dev.jstech.core.worldgen.biome.WorldBiomes;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.terrain.Terrains;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Writes what a mod declared for the generation of the world as the datapack files the game reads: for each vein of
 * ore its configured feature, its placed feature and the biome modifier that adds it; for each structure its template
 * pool, the structure and its set.
 */
public final class WorldGenProvider extends DatapackBuiltinEntriesProvider {

    private final String modid;

    public WorldGenProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries,
                            final String modid) {
        super(output, registries, entries(modid), Set.of(modid));
        this.modid = modid;
    }

    @Override
    public String getName() {
        return modid + ":world_generation";
    }

    /** The registry entries of the mod's declarations. */
    public static RegistrySetBuilder entries(final String modid) {
        final List<WorldGen.OreVein> ores = WorldGen.oresOf(modid);
        final List<WorldGen.DimensionSpec> dimensions = WorldGen.dimensionsOf(modid);
        final List<WorldBiomes.BiomeSpec> declaredBiomes = WorldBiomes.biomesOf(modid);
        final List<Terrains.TerrainSpec> terrains = Terrains.terrainsOf(modid);
        final List<Shaped> shapes = shapes(modid);
        return new RegistrySetBuilder()
                .add(Registries.NOISE, context -> terrains.forEach(terrain -> terrain.noises().forEach(noise ->
                        context.register(terrain.noise(noise.name()), noise.parameters()))))
                .add(Registries.NOISE_SETTINGS, context -> terrains.forEach(terrain -> context.register(
                        terrain.key(), terrain.settings(context.lookup(Registries.NOISE)))))
                .add(Registries.BIOME, context -> {
                    final HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
                    final HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
                    declaredBiomes.forEach(biome -> context.register(biome.key(), biome.biome(placed, carvers)));
                })
                .add(Registries.DIMENSION_TYPE, context -> dimensions.forEach(dimension -> context.register(
                        key(Registries.DIMENSION_TYPE, dimension.id()), dimension.dimensionType())))
                .add(Registries.LEVEL_STEM, context -> {
                    final HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
                    final HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
                    final HolderGetter<NoiseGeneratorSettings> noise = context.lookup(Registries.NOISE_SETTINGS);
                    // A dimension only ever copied while the game runs has its type and no stem the world loads.
                    dimensions.stream().filter(dimension -> !dimension.runtimeOnly()).forEach(dimension ->
                            context.register(key(Registries.LEVEL_STEM, dimension.id()),
                                    new LevelStem(types.getOrThrow(key(Registries.DIMENSION_TYPE, dimension.id())),
                                            dimension.generator(biomes, noise))));
                })
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    ores.forEach(ore -> context.register(key(Registries.CONFIGURED_FEATURE, ore.id()),
                            new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets(ore), ore.veinSize()))));
                    final HolderGetter<Biome> all = context.lookup(Registries.BIOME);
                    shapes.forEach(shape -> context.register(key(Registries.CONFIGURED_FEATURE, shape.id()),
                            shape.feature().apply(all)));
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    final HolderGetter<ConfiguredFeature<?, ?>> configured =
                            context.lookup(Registries.CONFIGURED_FEATURE);
                    ores.forEach(ore -> context.register(key(Registries.PLACED_FEATURE, ore.id()), new PlacedFeature(
                            configured.getOrThrow(key(Registries.CONFIGURED_FEATURE, ore.id())), placement(ore))));
                    // A shape of the land is placed once in each chunk, at its corner, and works out the rest itself.
                    shapes.forEach(shape -> context.register(key(Registries.PLACED_FEATURE, shape.id()),
                            new PlacedFeature(configured.getOrThrow(key(Registries.CONFIGURED_FEATURE, shape.id())),
                                    List.of())));
                })
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    final HolderGetter<Biome> all = context.lookup(Registries.BIOME);
                    final HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
                    ores.forEach(ore -> context.register(key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ore.id()),
                            (BiomeModifier) new BiomeModifiers.AddFeaturesBiomeModifier(all.getOrThrow(ore.biomes()),
                                    HolderSet.direct(placed.getOrThrow(key(Registries.PLACED_FEATURE, ore.id()))),
                                    GenerationStep.Decoration.UNDERGROUND_ORES)));
                    shapes.forEach(shape -> context.register(key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, shape.id()),
                            (BiomeModifier) new BiomeModifiers.AddFeaturesBiomeModifier(all.getOrThrow(shape.biomes()),
                                    HolderSet.direct(placed.getOrThrow(key(Registries.PLACED_FEATURE, shape.id()))),
                                    shape.step())));
                })
                .add(Registries.PROCESSOR_LIST, context -> StructureEntries.processors(context, modid))
                .add(Registries.TEMPLATE_POOL, context -> StructureEntries.pools(context, modid))
                .add(Registries.STRUCTURE, context -> StructureEntries.structures(context, modid))
                .add(Registries.STRUCTURE_SET, context -> StructureEntries.sets(context, modid));
    }

    private static <T> ResourceKey<T> key(final ResourceKey<? extends Registry<T>> registry,
                                          final ResourceLocation id) {
        return ResourceKey.create(registry, id);
    }

    /*
     * The shapes of the land the mod declared, each a feature of the Core's: landforms first, laid on the raw ground,
     * then the rock in layers through what they raised, before the ores; the seas last, once everything else is laid.
     */
    private static List<Shaped> shapes(final String modid) {
        final List<Shaped> shapes = new ArrayList<>();
        Landforms.landformsOf(modid).forEach(one -> shapes.add(new Shaped(one.id(), one.biomes(), one.step(),
                all -> new ConfiguredFeature<>(CoreFeatures.LANDFORM.get(), one.config(all)))));
        Landforms.strataOf(modid).forEach(one -> shapes.add(new Shaped(one.id(), one.biomes(),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                all -> new ConfiguredFeature<>(CoreFeatures.STRATA.get(), one.config()))));
        Landforms.seasOf(modid).forEach(one -> shapes.add(new Shaped(one.id(), one.biomes(),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
                all -> new ConfiguredFeature<>(CoreFeatures.SEA.get(), one.config()))));
        return shapes;
    }

    /* A shape of the land, as it is written: its feature, the biomes it is added to, and its step. */
    private record Shaped(ResourceLocation id, TagKey<Biome> biomes, GenerationStep.Decoration step,
                          Function<HolderGetter<Biome>, ConfiguredFeature<?, ?>> feature) {
    }


    /* Stone and its kin get the stone ore, deepslate and its kin the deepslate one when the vein has it. */
    private static List<OreConfiguration.TargetBlockState> targets(final WorldGen.OreVein ore) {
        final List<OreConfiguration.TargetBlockState> targets = new ArrayList<>();
        targets.add(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                ore.stone().get().defaultBlockState()));
        if (ore.deepslate() != null) {
            targets.add(OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                    ore.deepslate().get().defaultBlockState()));
        }
        return targets;
    }

    private static List<PlacementModifier> placement(final WorldGen.OreVein ore) {
        final VerticalAnchor bottom = VerticalAnchor.absolute(ore.minY());
        final VerticalAnchor top = VerticalAnchor.absolute(ore.maxY());
        return List.of(CountPlacement.of(ore.perChunk()), InSquarePlacement.spread(),
                ore.triangle() ? HeightRangePlacement.triangle(bottom, top) : HeightRangePlacement.uniform(bottom, top),
                BiomeFilter.biome());
    }
}
