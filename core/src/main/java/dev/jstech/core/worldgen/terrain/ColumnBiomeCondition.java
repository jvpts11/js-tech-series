/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * A surface rule's test of the biome, {@code jscore:column_biome}, read once a column: the biome where the column's
 * rules are first read, its highest block they reach, near its floor. The game's own test reads the biome anew at
 * every block it is asked at, a costly look into the biomes' noise, down every block of the ground its rules reach;
 * the ground's biome does not change within the few blocks under its floor a surface lays.
 *
 * @param biomes the biomes it holds in
 */
public record ColumnBiomeCondition(List<ResourceKey<Biome>> biomes) implements SurfaceRules.ConditionSource {

    /** How it is written in a surface rule: {@code {"type": "jscore:column_biome", "biome_is": [...]}}. */
    public static final MapCodec<ColumnBiomeCondition> CODEC = ResourceKey.codec(Registries.BIOME).listOf()
            .fieldOf("biome_is").xmap(ColumnBiomeCondition::new, ColumnBiomeCondition::biomes);
    private static final KeyDispatchDataCodec<ColumnBiomeCondition> DISPATCH = KeyDispatchDataCodec.of(CODEC);

    public ColumnBiomeCondition {
        biomes = List.copyOf(biomes);
    }

    /** The test that the column is in {@code biome}. */
    public static ColumnBiomeCondition of(final ResourceKey<Biome> biome) {
        return new ColumnBiomeCondition(List.of(Objects.requireNonNull(biome, "biome")));
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return DISPATCH;
    }

    @Override
    public SurfaceRules.Condition apply(final SurfaceRules.Context context) {
        return new Column(context, Set.copyOf(this.biomes));
    }

    /* The test as one chunk's surface reads it, keeping its answer for the column it was last asked in. */
    private static final class Column implements SurfaceRules.Condition {

        private final SurfaceRules.Context context;
        private final Set<ResourceKey<Biome>> biomes;
        private long column;
        private boolean holds;

        private Column(final SurfaceRules.Context context, final Set<ResourceKey<Biome>> biomes) {
            this.context = context;
            this.biomes = biomes;
            this.column = context.lastUpdateXZ - 1;
        }

        @Override
        public boolean test() {
            if (this.column != this.context.lastUpdateXZ) {
                this.column = this.context.lastUpdateXZ;
                this.holds = this.context.biome.get().is(this.biomes::contains);
            }
            return this.holds;
        }
    }
}
