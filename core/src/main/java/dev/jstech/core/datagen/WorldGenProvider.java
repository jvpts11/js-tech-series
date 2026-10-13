/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.worldgen.WorldGen;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
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
        final List<WorldGen.TemplateStructure> structures = WorldGen.structuresOf(modid);
        final List<WorldGen.DimensionSpec> dimensions = WorldGen.dimensionsOf(modid);
        return new RegistrySetBuilder()
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
                .add(Registries.CONFIGURED_FEATURE, context -> ores.forEach(ore -> context.register(
                        key(Registries.CONFIGURED_FEATURE, ore.id()),
                        new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets(ore), ore.veinSize())))))
                .add(Registries.PLACED_FEATURE, context -> {
                    final HolderGetter<ConfiguredFeature<?, ?>> configured =
                            context.lookup(Registries.CONFIGURED_FEATURE);
                    ores.forEach(ore -> context.register(key(Registries.PLACED_FEATURE, ore.id()), new PlacedFeature(
                            configured.getOrThrow(key(Registries.CONFIGURED_FEATURE, ore.id())), placement(ore))));
                })
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                    final HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
                    final HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
                    ores.forEach(ore -> context.register(key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ore.id()),
                            (BiomeModifier) new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(ore.biomes()),
                                    HolderSet.direct(placed.getOrThrow(key(Registries.PLACED_FEATURE, ore.id()))),
                                    GenerationStep.Decoration.UNDERGROUND_ORES)));
                })
                .add(Registries.TEMPLATE_POOL, context -> {
                    final HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
                    structures.forEach(structure -> context.register(key(Registries.TEMPLATE_POOL, structure.id()),
                            new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY),
                                    List.of(Pair.of(StructurePoolElement.single(structure.template().toString()), 1)),
                                    StructureTemplatePool.Projection.RIGID)));
                })
                .add(Registries.STRUCTURE, context -> {
                    final HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
                    final HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
                    structures.forEach(structure -> context.register(key(Registries.STRUCTURE, structure.id()),
                            new JigsawStructure(new Structure.StructureSettings(biomes.getOrThrow(structure.biomes()),
                                    Map.of(), structure.step(), structure.terrain()),
                                    pools.getOrThrow(key(Registries.TEMPLATE_POOL, structure.id())), 1,
                                    ConstantHeight.of(VerticalAnchor.absolute(0)), false, structure.heightmap())));
                })
                .add(Registries.STRUCTURE_SET, context -> {
                    final HolderGetter<Structure> built = context.lookup(Registries.STRUCTURE);
                    structures.forEach(structure -> context.register(key(Registries.STRUCTURE_SET, structure.id()),
                            new StructureSet(built.getOrThrow(key(Registries.STRUCTURE, structure.id())),
                                    new RandomSpreadStructurePlacement(structure.spacing(), structure.separation(),
                                            RandomSpreadType.LINEAR, structure.salt()))));
                });
    }

    private static <T> ResourceKey<T> key(final ResourceKey<? extends Registry<T>> registry,
                                          final ResourceLocation id) {
        return ResourceKey.create(registry, id);
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
