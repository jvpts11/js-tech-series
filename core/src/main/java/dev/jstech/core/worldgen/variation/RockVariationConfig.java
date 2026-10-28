/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A rock's variants and where geology puts each, as its feature's file says: each block of the rock takes the first
 * variant whose place it lies in, and stays as it is where it lies in none.
 *
 * <pre>{@code
 * { "rock": "mymod:martian_basalt",
 *   "variants": [
 *     {"block": {"Name": "mymod:columnar_martian_basalt"}, "where": {"exposure": "side", "min_slope": 2}},
 *     {"block": {"Name": "mymod:vesicular_martian_basalt"}, "where": {"exposure": "top"},
 *      "biomes": "#mymod:lava_plains"} ] }
 * }</pre>
 *
 * @param rock     the rock it varies
 * @param variants its variants, the first that fits taken
 */
public record RockVariationConfig(Block rock, List<Variant> variants) implements FeatureConfiguration {

    public static final Codec<RockVariationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("rock").forGetter(RockVariationConfig::rock),
            Variant.CODEC.listOf().fieldOf("variants").forGetter(RockVariationConfig::variants))
            .apply(instance, RockVariationConfig::new));

    public RockVariationConfig {
        variants = List.copyOf(variants);
    }

    /** The deepest any of its variants reaches under the top of the ground, past which none is looked for. */
    public int deepest() {
        int deepest = 0;
        for (final Variant variant : this.variants) {
            deepest = Math.max(deepest, variant.where().maxDepth());
        }
        return deepest;
    }

    /**
     * One variant of the rock.
     *
     * @param block  its block
     * @param where  where geology puts it
     * @param biomes the biomes it is put in, if not any
     */
    public record Variant(BlockState block, RockRule where, Optional<HolderSet<Biome>> biomes) {

        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("block").forGetter(Variant::block),
                RockRule.CODEC.optionalFieldOf("where", RockRule.ANYWHERE).forGetter(Variant::where),
                RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(Variant::biomes))
                .apply(instance, Variant::new));
    }
}
