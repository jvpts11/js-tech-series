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
 * A process that shapes a world's ground: how impacts pile up craters, how wind carves dunes and canyons. It is only
 * the process; everything that makes a world itself is the world's data, which it reads, so two worlds of the same
 * generator never look alike. The data generation asks it once for each world that names it, and writes what it gives
 * as that world's terrain.
 *
 * <pre>{@code
 * WorldGenerators.register(rl("shallow_seas"), new IWorldGenerator() { ... });
 * }</pre>
 */
public interface IWorldGenerator {

    /**
     * The ground of {@code world}, as a density function the game samples: solid where it is above zero. Its noises are
     * read from {@code noises} and the density functions it builds on from {@code densities}.
     */
    DensityFunction ground(SpaceWorld world, HolderGetter<NormalNoise.NoiseParameters> noises,
                           HolderGetter<DensityFunction> densities);

    /** What lies on the ground of {@code world}: its top blocks, the rock under them, by height, slope and biome. */
    SurfaceRules.RuleSource surface(SpaceWorld world);
}
