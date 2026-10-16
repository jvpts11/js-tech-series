/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * What a land's seas are of, as their feature's file says: the liquid the terrain filled its low ground with, what it
 * freezes to, and at how many kelvin it freezes and boils away.
 *
 * <pre>{@code
 * { "liquid": {"Name": "minecraft:water"}, "frozen": {"Name": "minecraft:packed_ice"}, "freezes": 271.35 }
 * }</pre>
 *
 * @param liquid  the seas' liquid
 * @param frozen  what its top freezes to where it is cold
 * @param freezes below how many kelvin it freezes
 * @param boils   above how many kelvin it boils away
 */
public record SeaConfig(BlockState liquid, BlockState frozen, double freezes, double boils)
        implements FeatureConfiguration {

    private static final double HOTTEST = 100_000;
    /* Sea water, salt in it, freezes a little under fresh water's freezing point. */
    private static final double SEA_WATER_FREEZES = 271.35;
    private static final double WATER_BOILS = 373.15;
    public static final Codec<SeaConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.optionalFieldOf("liquid", Blocks.WATER.defaultBlockState()).forGetter(SeaConfig::liquid),
            BlockState.CODEC.optionalFieldOf("frozen", Blocks.ICE.defaultBlockState()).forGetter(SeaConfig::frozen),
            Codec.doubleRange(0, HOTTEST).optionalFieldOf("freezes", SEA_WATER_FREEZES)
                    .forGetter(SeaConfig::freezes),
            Codec.doubleRange(0, HOTTEST).optionalFieldOf("boils", WATER_BOILS).forGetter(SeaConfig::boils))
            .apply(instance, SeaConfig::new));

    public SeaConfig {
        Objects.requireNonNull(liquid, "liquid");
        Objects.requireNonNull(frozen, "frozen");
    }
}
