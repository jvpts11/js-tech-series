/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds where every creature of the data appears to each biome it names, as the server starts: {@code jscore:creatures},
 * one file in the Core's data that covers every creature. Whether it appears in a dimension, and in its air, is checked
 * as it appears ({@link Creatures}).
 */
public record CreaturesBiomeModifier() implements BiomeModifier {

    public static final CreaturesBiomeModifier INSTANCE = new CreaturesBiomeModifier();
    public static final MapCodec<CreaturesBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase,
                       final ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }
        for (final Map.Entry<ResourceLocation, CreatureDef> creature : Creatures.onServer().entrySet()) {
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(creature.getKey()).orElse(null);
            if (type == null) {
                continue;
            }
            for (final CreatureDef.Spawn spawn : creature.getValue().spawns()) {
                if (spawn.in(biome)) {
                    builder.getMobSpawnSettings().addSpawn(spawn.category().orElse(type.getCategory()),
                            new MobSpawnSettings.SpawnerData(type, spawn.weight(), spawn.least(), spawn.most()));
                }
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return Creatures.BIOME_MODIFIER.get();
    }
}
