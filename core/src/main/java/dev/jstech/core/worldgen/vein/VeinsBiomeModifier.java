/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * The biome modifier {@code jscore:veins}: it adds the veins to every biome of every dimension, and puts
 * {@link ScatteredOres} first on the scattered ores veins stand in for ({@code covered}, the game's iron, copper and
 * gold) and on other mods' ores that place blocks of {@code tagged}, so the server's settings decide, chunk by chunk,
 * which of them are still laid. The Core writes the one file of it.
 *
 * @param veins   the veins' feature, placed once in each chunk
 * @param covered the scattered ores veins stand in for
 * @param tagged  the blocks by which other mods' scattered ores are known, to be moved into veins when the server
 *                tries; none to leave them be
 */
public record VeinsBiomeModifier(Holder<PlacedFeature> veins, HolderSet<PlacedFeature> covered,
                                 Optional<TagKey<Block>> tagged) implements BiomeModifier {

    /* Each scattered ore's feature, as it is laid with the filter first: one for every biome, so the game sorts one. */
    private static final Map<PlacedFeature, Holder<PlacedFeature>> FILTERED = new ConcurrentHashMap<>();
    private static final String GAME = ResourceLocation.DEFAULT_NAMESPACE;

    public static final MapCodec<VeinsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PlacedFeature.CODEC.fieldOf("veins").forGetter(VeinsBiomeModifier::veins),
            PlacedFeature.LIST_CODEC.fieldOf("covered").forGetter(VeinsBiomeModifier::covered),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("tagged").forGetter(VeinsBiomeModifier::tagged))
            .apply(instance, VeinsBiomeModifier::new));

    /** Forgets the filtered features, as the server stops and its biomes go. */
    public static void forget() {
        FILTERED.clear();
    }

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase,
                       final ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD) {
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, this.veins);
            return;
        }
        if (phase != Phase.MODIFY) {
            return;
        }
        for (final GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            final List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(step);
            for (int i = 0; i < features.size(); i++) {
                final Holder<PlacedFeature> feature = features.get(i);
                if (this.covered.contains(feature)) {
                    features.set(i, filtered(feature.value(), false));
                } else if (moved(feature)) {
                    features.set(i, filtered(feature.value(), true));
                }
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return Veins.BIOME_MODIFIER.get();
    }

    /* Whether {@code feature} is another mod's scattered ore of the tagged blocks, noting it as moved when it is. */
    private boolean moved(final Holder<PlacedFeature> feature) {
        final Optional<ResourceKey<PlacedFeature>> key = feature.unwrapKey();
        if (this.tagged.isEmpty() || key.isEmpty() || key.get().location().getNamespace().equals(GAME)) {
            return false;
        }
        if (!(feature.value().feature().value().config() instanceof OreConfiguration ore)
                || !MovedOres.places(ore, this.tagged.get())) {
            return false;
        }
        MovedOres.note(key.get().location(), ore);
        return true;
    }

    private static Holder<PlacedFeature> filtered(final PlacedFeature feature, final boolean moved) {
        return FILTERED.computeIfAbsent(feature, original -> {
            final List<PlacementModifier> placement = new ArrayList<>();
            placement.add(new ScatteredOres(moved));
            placement.addAll(original.placement());
            return Holder.direct(new PlacedFeature(original.feature(), placement));
        });
    }
}
