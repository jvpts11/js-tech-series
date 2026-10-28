/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.space.api.IWorldGenerator;
import dev.jstech.space.api.IWorldModifier;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.WorldGenerators;
import dev.jstech.space.api.WorldModifiers;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * A world's ground and surface, put together from its layers: its generator's, then each of its modifiers laid over
 * in the order the world lists them.
 */
public final class WorldShapes {

    private WorldShapes() {
    }

    /**
     * The ground of {@code world}.
     *
     * @throws IllegalArgumentException when the world names no generator, or one or a modifier no mod registered
     */
    public static DensityFunction ground(final SpaceWorld world, final HolderGetter<NormalNoise.NoiseParameters> noises,
                                         final HolderGetter<DensityFunction> densities) {
        DensityFunction ground = generatorOf(world).ground(world, noises, densities);
        for (final ResourceLocation modifier : world.modifiers()) {
            ground = modifierOf(modifier).ground(world, ground, noises, densities);
        }
        return ground;
    }

    /**
     * What lies on the ground of {@code world}.
     *
     * @throws IllegalArgumentException when the world names no generator, or one or a modifier no mod registered
     */
    public static SurfaceRules.RuleSource surface(final SpaceWorld world) {
        SurfaceRules.RuleSource surface = generatorOf(world).surface(world);
        for (final ResourceLocation modifier : world.modifiers()) {
            surface = modifierOf(modifier).surface(world, surface);
        }
        return surface;
    }

    private static IWorldGenerator generatorOf(final SpaceWorld world) {
        final ResourceLocation id = world.generator().orElseThrow(() -> new IllegalArgumentException(
                world.dimension().location() + " names no generator: its dimension generates itself"));
        return WorldGenerators.get(id).orElseThrow(() -> new IllegalArgumentException(
                world.dimension().location() + " names the generator " + id + ", which no mod registered"));
    }

    private static IWorldModifier modifierOf(final ResourceLocation id) {
        return WorldModifiers.get(id).orElseThrow(() -> new IllegalArgumentException(
                "the modifier " + id + " is registered by no mod"));
    }
}
