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
 * {@code jscore:terrace}: another function's value in steps {@code step} high, flat across most of each and rising
 * smoothly to the next over its last {@code smoothness}: mesas, rice terraces, a quarry's benches.
 *
 * @param input      the function stepped
 * @param step       how high a step is
 * @param smoothness the share of each step it rises over, from 0 (sheer) to 1 (no flats)
 */
public record TerraceDensity(DensityFunction input, double step, double smoothness) implements ITransformDensity {

    public static final MapCodec<TerraceDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(TerraceDensity::input),
            Codec.doubleRange(1e-6, 1e6).fieldOf("step").forGetter(TerraceDensity::step),
            Codec.doubleRange(0, 1).optionalFieldOf("smoothness", 0.25).forGetter(TerraceDensity::smoothness)
    ).apply(i, TerraceDensity::new));
    public static final KeyDispatchDataCodec<TerraceDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double transform(final double value) {
        return DensityMath.terrace(value, step, smoothness);
    }

    @Override
    public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
        return visitor.apply(new TerraceDensity(input.mapAll(visitor), step, smoothness));
    }

    @Override
    public double minValue() {
        return Math.floor(input.minValue() / step) * step;
    }

    @Override
    public double maxValue() {
        return Math.ceil(input.maxValue() / step) * step;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
