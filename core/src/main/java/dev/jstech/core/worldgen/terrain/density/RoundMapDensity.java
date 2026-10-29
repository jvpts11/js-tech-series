/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.worldgen.terrain.RoundMap;
import dev.jstech.core.worldgen.terrain.RoundMaps;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:round_map}: a map of a round world ({@link RoundMaps}) read at each place of it, by the place's
 * longitude and latitude on the world's sphere, from {@code low} where the map is black to {@code high} where it is
 * white. The world's size is its bounds in force ({@link DimensionBorders#inForce}), so the map lies where the world's
 * sphere is drawn whatever size the server gave it. Where the map or the world's size is not known, it reads
 * {@code low}.
 */
public final class RoundMapDensity implements DensityFunction.SimpleFunction {

    private final ResourceLocation map;
    private final ResourceLocation world;
    private final double low;
    private final double high;
    private final ResourceKey<Level> dimension;

    public static final MapCodec<RoundMapDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("map").forGetter(RoundMapDensity::map),
            ResourceLocation.CODEC.fieldOf("world").forGetter(RoundMapDensity::world),
            Codec.DOUBLE.optionalFieldOf("low", 0.0).forGetter(RoundMapDensity::low),
            Codec.DOUBLE.optionalFieldOf("high", 1.0).forGetter(RoundMapDensity::high)
    ).apply(i, RoundMapDensity::new));
    public static final KeyDispatchDataCodec<RoundMapDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** The map {@code map} of the round world {@code world}, read from {@code low} to {@code high}. */
    public RoundMapDensity(final ResourceLocation map, final ResourceLocation world, final double low,
                           final double high) {
        this.map = map;
        this.world = world;
        this.low = low;
        this.high = high;
        this.dimension = ResourceKey.create(Registries.DIMENSION, world);
    }

    public ResourceLocation map() {
        return this.map;
    }

    public ResourceLocation world() {
        return this.world;
    }

    public double low() {
        return this.low;
    }

    public double high() {
        return this.high;
    }

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        final Optional<DimensionBorders.Bounds> bounds = DimensionBorders.inForce(this.dimension);
        final Optional<RoundMap> read = RoundMaps.get(this.map);
        if (bounds.isEmpty() || read.isEmpty()) {
            return this.low;
        }
        final double width = bounds.get().maxX() - bounds.get().minX();
        return this.low + (this.high - this.low) * read.get().atBlock(context.blockX(), context.blockZ(), width);
    }

    @Override
    public double minValue() {
        return Math.min(this.low, this.high);
    }

    @Override
    public double maxValue() {
        return Math.max(this.low, this.high);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
