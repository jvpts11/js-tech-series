/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.worldgen.structure.FixedPlacement;
import dev.jstech.core.worldgen.structure.PlacedStructure;
import dev.jstech.core.worldgen.structure.SpreadPlacement;
import dev.jstech.core.worldgen.structure.Structures;
import dev.jstech.core.worldgen.structure.Weathering;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

/**
 * The registry entries of a mod's declared structures ({@link Structures}): for each structure its start pool, the
 * processor list that wears its templates, the structure of the Core's {@code jscore:placed} kind, and its set, spread
 * ({@code jscore:spread}) or at fixed places ({@code jscore:fixed}); for each pool declared beside them, the pool and
 * its processor list.
 */
final class StructureEntries {

    private StructureEntries() {
    }

    static void processors(final BootstrapContext<StructureProcessorList> context, final String modid) {
        Structures.structuresOf(modid).forEach(structure -> context.register(
                ResourceKey.create(Registries.PROCESSOR_LIST, structure.id()),
                processors(structure.worn(), structure.age())));
        Structures.poolsOf(modid).forEach(pool -> context.register(
                ResourceKey.create(Registries.PROCESSOR_LIST, pool.id()), processors(pool.worn(), 1)));
    }

    static void pools(final BootstrapContext<StructureTemplatePool> context, final String modid) {
        final HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        final HolderGetter<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST);
        Structures.structuresOf(modid).forEach(structure -> context.register(
                ResourceKey.create(Registries.TEMPLATE_POOL, structure.id()),
                pool(pools, processors.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, structure.id())),
                        structure.elements())));
        Structures.poolsOf(modid).forEach(pool -> context.register(ResourceKey.create(Registries.TEMPLATE_POOL,
                pool.id()), pool(pools, processors.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST,
                pool.id())), pool.elements())));
    }

    static void structures(final BootstrapContext<Structure> context, final String modid) {
        final HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        final HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Structures.structuresOf(modid).forEach(structure -> context.register(
                ResourceKey.create(Registries.STRUCTURE, structure.id()),
                new PlacedStructure(new Structure.StructureSettings(biomes.getOrThrow(structure.biomes()), Map.of(),
                        structure.step(), structure.terrain()),
                        pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, structure.id())),
                        structure.size(), structure.reach(), structure.fit(),
                        Optional.ofNullable(structure.foundation()).map(block -> block.get().defaultBlockState()),
                        structure.growth(), structure.streets())));
    }

    static void sets(final BootstrapContext<StructureSet> context, final String modid) {
        final HolderGetter<Structure> built = context.lookup(Registries.STRUCTURE);
        Structures.structuresOf(modid).forEach(structure -> {
            final StructurePlacement placement = structure.fixedAt().isEmpty()
                    ? new SpreadPlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT,
                    structure.frequency(), structure.salt(), Optional.empty(), structure.spacing(),
                    structure.separation(), RandomSpreadType.LINEAR, structure.minDistance(), structure.clearOf())
                    : new FixedPlacement(structure.fixedAt(), structure.salt());
            context.register(ResourceKey.create(Registries.STRUCTURE_SET, structure.id()), new StructureSet(
                    built.getOrThrow(ResourceKey.create(Registries.STRUCTURE, structure.id())), placement));
        });
    }

    /* A pool of {@code elements}, each worn by {@code processors}, falling back on the game's empty pool. */
    private static StructureTemplatePool pool(final HolderGetter<StructureTemplatePool> pools,
                                              final Holder<StructureProcessorList> processors,
                                              final List<Structures.Element> elements) {
        final List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> picks =
                new ArrayList<>();
        for (final Structures.Element element : elements) {
            picks.add(Pair.of(StructurePoolElement.single(element.template().toString(), processors),
                    element.weight()));
        }
        return new StructureTemplatePool(pools.getOrThrow(Pools.EMPTY), picks, StructureTemplatePool.Projection.RIGID);
    }

    /* The processors of {@code worn}: none but the wear, if any. */
    private static StructureProcessorList processors(final List<Structures.Worn> worn, final double age) {
        if (worn.isEmpty()) {
            return new StructureProcessorList(List.of());
        }
        return new StructureProcessorList(List.of(new Weathering(worn.stream().map(rule -> new Weathering.Rule(
                new BlockMatchTest(rule.from().get()), rule.worn().get().defaultBlockState(), rule.chance()))
                .toList(), age).processor()));
    }
}
