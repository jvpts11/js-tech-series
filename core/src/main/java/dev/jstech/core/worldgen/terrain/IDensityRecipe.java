/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * How a declared density function is made, from the noises and the other density functions it samples: the data
 * generation asks it once and writes what it gives as the density function's file.
 *
 * <pre>{@code
 * (noises, densities) -> DensityFunctions.add(DensityFunctions.yClampedGradient(0, 128, 1.5, -1.5),
 *         DensityFunctions.noise(noises.getOrThrow(RIDGE_NOISE.key()), 1.0, 0.0).abs())
 * }</pre>
 */
@FunctionalInterface
public interface IDensityRecipe {

    /** The density function, its noises read from {@code noises} and the density functions it uses from the other. */
    DensityFunction make(HolderGetter<NormalNoise.NoiseParameters> noises, HolderGetter<DensityFunction> densities);
}
