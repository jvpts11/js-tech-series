/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen;

import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformFeature;
import dev.jstech.core.worldgen.landform.SeaConfig;
import dev.jstech.core.worldgen.landform.SeaFeature;
import dev.jstech.core.worldgen.strata.StrataConfig;
import dev.jstech.core.worldgen.strata.StrataFeature;
import dev.jstech.core.worldgen.vein.VeinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The kinds of feature the Core gives every mod's world generation: landforms, rock in layers, seas and veins. */
public final class CoreFeatures {

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, JsCore.MODID);

    /** {@code jscore:landform}: see {@link LandformFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<LandformConfig>> LANDFORM =
            FEATURES.register("landform", LandformFeature::new);
    /** {@code jscore:strata}: see {@link StrataFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<StrataConfig>> STRATA =
            FEATURES.register("strata", StrataFeature::new);
    /** {@code jscore:sea}: see {@link SeaFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<SeaConfig>> SEA = FEATURES.register("sea", SeaFeature::new);
    /** {@code jscore:veins}: see {@link VeinFeature}. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VEINS =
            FEATURES.register("veins", VeinFeature::new);

    private CoreFeatures() {
    }

    /** Registers the features, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
