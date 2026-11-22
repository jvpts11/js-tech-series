/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The kinds of condition the Core adds to the game's surface rules, for any mod's terrain and any datapack:
 * {@code jscore:column_biome}, the biome read once a column ({@link ColumnBiomeCondition}). A terrain's surface built
 * with {@link SurfaceRuleBuilder} uses it for its biomes.
 */
public final class CoreSurfaceConditions {

    private static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> TYPES =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, JsCore.MODID);

    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>,
            MapCodec<ColumnBiomeCondition>> COLUMN_BIOME = TYPES.register("column_biome",
            () -> ColumnBiomeCondition.CODEC);

    private CoreSurfaceConditions() {
    }

    /** Registers the kinds, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }
}
