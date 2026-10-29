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
import dev.jstech.core.worldgen.landform.LandformShapes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:round_crater}: one great crater at its place on a round world, by longitude and latitude, as wide as
 * its {@code radius} in degrees of the world, so it lies where the world's sphere draws it whatever size the server
 * gave the world: how far it moves the ground, in blocks, down {@code depth} in its bowl, up {@code rim} on its rim,
 * with a central peak {@code peak} high, as the Core's craters are shaped. Nothing where the world's size is not known.
 */
public final class RoundCraterDensity implements DensityFunction.SimpleFunction {

    private final ResourceLocation world;
    private final double longitude;
    private final double latitude;
    private final double radius;
    private final double depth;
    private final double rim;
    private final double peak;
    private final ResourceKey<Level> dimension;

    private static final Codec<Double> LONGITUDE = Codec.doubleRange(-180, 180);
    private static final Codec<Double> LATITUDE = Codec.doubleRange(-90, 90);
    private static final Codec<Double> BLOCKS = Codec.doubleRange(0, 4096);

    public static final MapCodec<RoundCraterDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("world").forGetter(RoundCraterDensity::world),
            LONGITUDE.fieldOf("longitude").forGetter(RoundCraterDensity::longitude),
            LATITUDE.fieldOf("latitude").forGetter(RoundCraterDensity::latitude),
            Codec.doubleRange(1e-4, 90).fieldOf("radius").forGetter(RoundCraterDensity::radius),
            BLOCKS.fieldOf("depth").forGetter(RoundCraterDensity::depth),
            BLOCKS.optionalFieldOf("rim", 0.0).forGetter(RoundCraterDensity::rim),
            BLOCKS.optionalFieldOf("peak", 0.0).forGetter(RoundCraterDensity::peak)
    ).apply(i, RoundCraterDensity::new));
    public static final KeyDispatchDataCodec<RoundCraterDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /**
     * A crater of the round world {@code world} at {@code longitude} and {@code latitude}, {@code radius} degrees in
     * radius, its bowl {@code depth} deep, its rim {@code rim} high and its central peak {@code peak} high, in blocks.
     */
    public RoundCraterDensity(final ResourceLocation world, final double longitude, final double latitude,
                              final double radius, final double depth, final double rim, final double peak) {
        this.world = world;
        this.longitude = longitude;
        this.latitude = latitude;
        this.radius = radius;
        this.depth = depth;
        this.rim = rim;
        this.peak = peak;
        this.dimension = ResourceKey.create(Registries.DIMENSION, world);
    }

    public ResourceLocation world() {
        return this.world;
    }

    public double longitude() {
        return this.longitude;
    }

    public double latitude() {
        return this.latitude;
    }

    public double radius() {
        return this.radius;
    }

    public double depth() {
        return this.depth;
    }

    public double rim() {
        return this.rim;
    }

    public double peak() {
        return this.peak;
    }

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        final Optional<DimensionBorders.Bounds> bounds = DimensionBorders.inForce(this.dimension);
        if (bounds.isEmpty()) {
            return 0;
        }
        final double perDegree = (bounds.get().maxX() - bounds.get().minX()) / 360;
        final double dx = context.blockX() - this.longitude * perDegree;
        final double dz = context.blockZ() + this.latitude * perDegree;
        return LandformShapes.crater(Math.sqrt(dx * dx + dz * dz), this.radius * perDegree, this.depth, this.rim,
                this.peak);
    }

    @Override
    public double minValue() {
        return -this.depth;
    }

    @Override
    public double maxValue() {
        return this.rim + this.peak;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
