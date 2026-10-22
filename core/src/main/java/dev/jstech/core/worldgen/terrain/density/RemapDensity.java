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
 * {@code jscore:remap}: another function's value moved along a line from one range to another, held inside the new
 * one when {@code clamped}: a noise of -1 to 1 made a height of 60 to 120, in one step rather than a multiply and an
 * add.
 *
 * @param input    the function moved
 * @param fromLow  where the old range starts
 * @param fromHigh where it ends
 * @param toLow    what its start becomes
 * @param toHigh   what its end becomes
 * @param clamped  whether what falls past the old range is held at the new range's ends
 */
public record RemapDensity(DensityFunction input, double fromLow, double fromHigh, double toLow, double toHigh,
                           boolean clamped) implements ITransformDensity {

    private static final double MOST = 1e6;

    public static final MapCodec<RemapDensity> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(RemapDensity::input),
            Codec.doubleRange(-MOST, MOST).fieldOf("from_low").forGetter(RemapDensity::fromLow),
            Codec.doubleRange(-MOST, MOST).fieldOf("from_high").forGetter(RemapDensity::fromHigh),
            Codec.doubleRange(-MOST, MOST).fieldOf("to_low").forGetter(RemapDensity::toLow),
            Codec.doubleRange(-MOST, MOST).fieldOf("to_high").forGetter(RemapDensity::toHigh),
            Codec.BOOL.optionalFieldOf("clamped", false).forGetter(RemapDensity::clamped)
    ).apply(i, RemapDensity::new));
    public static final KeyDispatchDataCodec<RemapDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double transform(final double value) {
        return DensityMath.remap(value, fromLow, fromHigh, toLow, toHigh, clamped);
    }

    @Override
    public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
        return visitor.apply(new RemapDensity(input.mapAll(visitor), fromLow, fromHigh, toLow, toHigh, clamped));
    }

    /* A line takes the ends of a range to the ends of the new one, either way round. */
    @Override
    public double minValue() {
        return Math.min(transform(input.minValue()), transform(input.maxValue()));
    }

    @Override
    public double maxValue() {
        return Math.max(transform(input.minValue()), transform(input.maxValue()));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
