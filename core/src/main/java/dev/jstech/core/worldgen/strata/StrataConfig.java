/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.strata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The rock of a land in layers, as its feature's file says: the sequences of layers its regions hold, each layer's
 * rock and thickness from the ground's top down, how wide a region is, how far the layers' boundaries rise and fall,
 * and what rock the layers take the place of. Below a sequence's last layer the rock is left as it was.
 *
 * <pre>{@code
 * { "sequences": [ [ {"block": {"Name": "minecraft:sandstone"}, "thickness": 8},
 *                    {"block": {"Name": "minecraft:calcite"}, "thickness": 5} ],
 *                  [ {"block": {"Name": "minecraft:tuff"}, "thickness": 12} ] ],
 *   "region": 384, "wobble": 4, "salt": 11 }
 * }</pre>
 *
 * @param sequences the sequences of layers, one of which each region holds
 * @param region    about how wide a region is, in blocks
 * @param wobble    how far the layers' boundaries rise and fall across the land, in blocks
 * @param salt      what keeps its regions apart from another's on the same seed
 * @param replaces  the rock the layers take the place of
 */
public record StrataConfig(List<List<Layer>> sequences, int region, double wobble, long salt,
                           TagKey<Block> replaces) implements FeatureConfiguration {

    private static final int THICKEST = 4096;
    private static final int WIDEST = 65_536;
    private static final double MOST_WOBBLE = 256;
    private static final int REGION = 384;
    private static final double WOBBLE = 4;
    public static final Codec<StrataConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Layer.CODEC.listOf().listOf().fieldOf("sequences").forGetter(StrataConfig::sequences),
            Codec.intRange(16, WIDEST).optionalFieldOf("region", REGION).forGetter(StrataConfig::region),
            Codec.doubleRange(0, MOST_WOBBLE).optionalFieldOf("wobble", WOBBLE).forGetter(StrataConfig::wobble),
            Codec.LONG.optionalFieldOf("salt", 0L).forGetter(StrataConfig::salt),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("replaces", BlockTags.BASE_STONE_OVERWORLD)
                    .forGetter(StrataConfig::replaces))
            .apply(instance, StrataConfig::new));

    public StrataConfig {
        sequences = sequences.stream().map(List::copyOf).toList();
        Objects.requireNonNull(replaces, "replaces");
    }

    /**
     * One layer of rock.
     *
     * @param block     its rock
     * @param thickness how many blocks thick
     */
    public record Layer(BlockState block, int thickness) {

        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("block").forGetter(Layer::block),
                Codec.intRange(1, THICKEST).fieldOf("thickness").forGetter(Layer::thickness))
                .apply(instance, Layer::new));
    }
}
