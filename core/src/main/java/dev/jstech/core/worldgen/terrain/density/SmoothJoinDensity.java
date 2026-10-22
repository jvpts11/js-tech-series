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
 * {@code jscore:smooth_min} and {@code jscore:smooth_max}: the smaller or the larger of two functions, the crease where
 * they meet rounded over {@code smoothness}, so two shapes join as one: a hill grown out of a plateau, a cave widened
 * into another with no seam. The game's own {@code min} and {@code max} leave a sharp edge there.
 *
 * @param larger     whether it takes the larger, else the smaller
 * @param argument1  one shape
 * @param argument2  the other
 * @param smoothness how far round the corner is rounded, in the functions' own units
 */
public record SmoothJoinDensity(boolean larger, DensityFunction argument1, DensityFunction argument2,
                                double smoothness) implements DensityFunction {

    public static final MapCodec<SmoothJoinDensity> MIN_CODEC = codec(false);
    public static final MapCodec<SmoothJoinDensity> MAX_CODEC = codec(true);
    public static final KeyDispatchDataCodec<SmoothJoinDensity> MIN = KeyDispatchDataCodec.of(MIN_CODEC);
    public static final KeyDispatchDataCodec<SmoothJoinDensity> MAX = KeyDispatchDataCodec.of(MAX_CODEC);
    /* The corner at most sinks below the smaller, or rises over the larger, by a quarter of its smoothness. */
    private static final double MOST_ROUNDING = 0.25;

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        return join(argument1.compute(context), argument2.compute(context));
    }

    @Override
    public void fillArray(final double[] values, final DensityFunction.ContextProvider contexts) {
        argument1.fillArray(values, contexts);
        final double[] other = new double[values.length];
        argument2.fillArray(other, contexts);
        for (int i = 0; i < values.length; i++) {
            values[i] = join(values[i], other[i]);
        }
    }

    @Override
    public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
        return visitor.apply(new SmoothJoinDensity(larger, argument1.mapAll(visitor), argument2.mapAll(visitor),
                smoothness));
    }

    @Override
    public double minValue() {
        return larger ? Math.max(argument1.minValue(), argument2.minValue())
                : Math.min(argument1.minValue(), argument2.minValue()) - MOST_ROUNDING * smoothness;
    }

    @Override
    public double maxValue() {
        return larger ? Math.max(argument1.maxValue(), argument2.maxValue()) + MOST_ROUNDING * smoothness
                : Math.min(argument1.maxValue(), argument2.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return larger ? MAX : MIN;
    }

    private double join(final double a, final double b) {
        return larger ? DensityMath.smoothMax(a, b, smoothness) : DensityMath.smoothMin(a, b, smoothness);
    }

    private static MapCodec<SmoothJoinDensity> codec(final boolean larger) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(SmoothJoinDensity::argument1),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(SmoothJoinDensity::argument2),
                Codec.doubleRange(0, 1e6).fieldOf("smoothness").forGetter(SmoothJoinDensity::smoothness)
        ).apply(i, (a, b, k) -> new SmoothJoinDensity(larger, a, b, k)));
    }
}
