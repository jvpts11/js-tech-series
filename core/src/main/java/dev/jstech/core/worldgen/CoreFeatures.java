/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformFeature;
import dev.jstech.core.worldgen.landform.SeaConfig;
import dev.jstech.core.worldgen.landform.SeaFeature;
import dev.jstech.core.worldgen.reservoir.ReservoirFeature;
import dev.jstech.core.worldgen.strata.StrataConfig;
import dev.jstech.core.worldgen.strata.StrataFeature;
import dev.jstech.core.worldgen.variation.RockVariationConfig;
import dev.jstech.core.worldgen.variation.RockVariationFeature;
import dev.jstech.core.worldgen.vein.VeinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The kinds of feature the Core gives every mod's world generation (landforms, rock in layers, rock varied by
 * geology, seas, veins and reservoirs), and the biome modifier that adds features to every biome.
 */
public final class CoreFeatures {

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, JsCore.MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, JsCore.MODID);

    /** {@code jscore:landform}: see {@link LandformFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<LandformConfig>> LANDFORM =
            FEATURES.register("landform", LandformFeature::new);
    /** {@code jscore:strata}: see {@link StrataFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<StrataConfig>> STRATA =
            FEATURES.register("strata", StrataFeature::new);
    /** {@code jscore:rock_variations}: see {@link RockVariationFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<RockVariationConfig>> ROCK_VARIATIONS =
            FEATURES.register("rock_variations", RockVariationFeature::new);
    /** {@code jscore:sea}: see {@link SeaFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<SeaConfig>> SEA = FEATURES.register("sea", SeaFeature::new);
    /** {@code jscore:veins}: see {@link VeinFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VEINS =
            FEATURES.register("veins", VeinFeature::new);
    /** {@code jscore:reservoirs}: see {@link ReservoirFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RESERVOIRS =
            FEATURES.register("reservoirs", ReservoirFeature::new);
    /** {@code jscore:everywhere}: see {@link EverywhereBiomeModifier}. */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>,
            MapCodec<EverywhereBiomeModifier>> EVERYWHERE =
            MODIFIERS.register("everywhere", () -> EverywhereBiomeModifier.CODEC);

    private CoreFeatures() {
    }

    /** Registers the features and the biome modifier, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        MODIFIERS.register(modEventBus);
    }
}
