/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A density function that changes one other's value, point by point, as {@link #transform} says: worked a column at a
 * time by filling the other's values first, as the game's own transforms are.
 */
public interface ITransformDensity extends DensityFunction {

    /** The function whose value it changes. */
    DensityFunction input();

    /** What it makes of {@code value}. */
    double transform(double value);

    @Override
    default double compute(final DensityFunction.FunctionContext context) {
        return transform(input().compute(context));
    }

    @Override
    default void fillArray(final double[] values, final DensityFunction.ContextProvider contexts) {
        input().fillArray(values, contexts);
        for (int i = 0; i < values.length; i++) {
            values[i] = transform(values[i]);
        }
    }
}
