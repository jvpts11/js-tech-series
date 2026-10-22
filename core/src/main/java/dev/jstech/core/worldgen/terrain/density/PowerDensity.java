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
 * {@code jscore:power}: another function's value raised to {@code exponent}, keeping its sign: above 1 the ground
 * flattens near nothing and steepens far from it (broad plains under sharp peaks), below 1 the reverse.
 *
 * @param input    the function raised
 * @param exponent the power it is raised to
 */
public record PowerDensity(DensityFunction input, double exponent) implements ITransformDensity {

    public static final MapCodec<PowerDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(PowerDensity::input),
            Codec.doubleRange(1e-3, 16).fieldOf("exponent").forGetter(PowerDensity::exponent)
    ).apply(i, PowerDensity::new));
    public static final KeyDispatchDataCodec<PowerDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double transform(final double value) {
        return DensityMath.power(value, exponent);
    }

    @Override
    public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
        return visitor.apply(new PowerDensity(input.mapAll(visitor), exponent));
    }

    /* The power keeping its sign never turns a larger value smaller, so the ends of the range go to the ends. */
    @Override
    public double minValue() {
        return transform(input.minValue());
    }

    @Override
    public double maxValue() {
        return transform(input.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
