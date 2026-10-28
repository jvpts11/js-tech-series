/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * What is laid over any generator: a world's polar caps, a giant basin, lava tubes, craters of eternal shadow. A
 * world lists its modifiers in order, each laid over the ground and the surface the ones before it left; one leaves
 * alone what it does not say.
 */
public interface IWorldModifier {

    /** The ground of {@code world} once this is laid over {@code under}; {@code under} itself by default. */
    default DensityFunction ground(final SpaceWorld world, final DensityFunction under,
                                   final HolderGetter<NormalNoise.NoiseParameters> noises,
                                   final HolderGetter<DensityFunction> densities) {
        return under;
    }

    /** What lies on the ground of {@code world} once this is laid over {@code under}; {@code under} by default. */
    default SurfaceRules.RuleSource surface(final SpaceWorld world, final SurfaceRules.RuleSource under) {
        return under;
    }
}
