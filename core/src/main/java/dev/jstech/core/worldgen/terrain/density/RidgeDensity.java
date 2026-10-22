/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:ridge}: another function's value folded into ridges, one less its distance from nothing, so a noise
 * that wanders round nothing draws long sharp crests: mountain chains, dunes, the walls between valleys.
 *
 * @param input the function folded
 */
public record RidgeDensity(DensityFunction input) implements ITransformDensity {

    public static final MapCodec<RidgeDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(RidgeDensity::input)
    ).apply(i, RidgeDensity::new));
    public static final KeyDispatchDataCodec<RidgeDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double transform(final double value) {
        return DensityMath.ridge(value);
    }

    @Override
    public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
        return visitor.apply(new RidgeDensity(input.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return 1 - Math.max(Math.abs(input.minValue()), Math.abs(input.maxValue()));
    }

    @Override
    public double maxValue() {
        final boolean crossesNothing = input.minValue() <= 0 && input.maxValue() >= 0;
        return crossesNothing ? 1 : 1 - Math.min(Math.abs(input.minValue()), Math.abs(input.maxValue()));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
