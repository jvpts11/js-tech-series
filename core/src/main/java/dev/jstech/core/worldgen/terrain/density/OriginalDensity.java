/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain.density;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code jscore:original}: in a worldgen modifier that changes a density function, the function as it was before, so
 * the change wraps it ("the old continents, plus a little") rather than writing it again. The modifier puts the old
 * function in its place; anywhere else it is nothing, and says so once in the log.
 */
public enum OriginalDensity implements DensityFunction.SimpleFunction {

    /** The one marker. */
    INSTANCE;

    private volatile boolean told;

    public static final MapCodec<OriginalDensity> DATA_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<OriginalDensity> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        if (!told) {
            told = true;
            JsCore.LOGGER.warn("jscore:original stands for the function a worldgen modifier changes, and is used "
                    + "outside one; it is taken as 0");
        }
        return 0;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
