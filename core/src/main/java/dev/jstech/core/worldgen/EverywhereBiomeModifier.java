/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * The biome modifier {@code jscore:everywhere}: it adds its features to every biome of every dimension, a mod's and a
 * data pack's included, in one step of the generation; for what works out for itself where it lies, as the Core's
 * reservoirs do, by the biome at a body's middle.
 *
 * <pre>{@code
 * { "type": "jscore:everywhere", "features": "mymod:fault_lines", "step": "underground_structures" }
 * }</pre>
 *
 * @param features what is added
 * @param step     when in the generation it is placed
 */
public record EverywhereBiomeModifier(HolderSet<PlacedFeature> features, GenerationStep.Decoration step)
        implements BiomeModifier {

    public static final MapCodec<EverywhereBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(EverywhereBiomeModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(EverywhereBiomeModifier::step))
            .apply(instance, EverywhereBiomeModifier::new));

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase,
                       final ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD) {
            this.features.forEach(feature -> builder.getGenerationSettings().addFeature(this.step, feature));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CoreFeatures.EVERYWHERE.get();
    }
}
