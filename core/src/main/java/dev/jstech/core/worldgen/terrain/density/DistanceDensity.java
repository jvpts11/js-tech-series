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
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:distance}: how far a place is from a point, across, in units of {@code radius}: 0 at the point, 1 a
 * radius away. An island, a crater, a ring of mountains round a world's spawn are drawn from it with the game's own
 * functions.
 *
 * @param x      the point's x
 * @param z      the point's z
 * @param radius the distance that counts as 1
 */
public record DistanceDensity(double x, double z, double radius) implements DensityFunction.SimpleFunction {

    /* The farthest a place of the world can be from a point of it, across. */
    private static final double WORLD_ACROSS = 30_000_000 * Math.sqrt(8);

    public static final MapCodec<DistanceDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(-WORLD_ACROSS, WORLD_ACROSS).optionalFieldOf("x", 0.0).forGetter(DistanceDensity::x),
            Codec.doubleRange(-WORLD_ACROSS, WORLD_ACROSS).optionalFieldOf("z", 0.0).forGetter(DistanceDensity::z),
            Codec.doubleRange(1, WORLD_ACROSS).fieldOf("radius").forGetter(DistanceDensity::radius)
    ).apply(i, DistanceDensity::new));
    public static final KeyDispatchDataCodec<DistanceDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        return DensityMath.distance(context.blockX(), context.blockZ(), x, z, radius);
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return WORLD_ACROSS / radius;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
