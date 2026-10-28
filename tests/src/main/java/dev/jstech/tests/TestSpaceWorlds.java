/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.space.api.IWorldGenerator;
import dev.jstech.space.api.IWorldModifier;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.api.WorldGenerators;
import dev.jstech.space.api.WorldModifiers;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * A world the way an add-on adds one to J's Space, to prove its world model: a generator of its own, ground that
 * thins from solid at the bottom to air at the top, and two modifiers laid over it in order, one raising the ground and
 * one paving its surface.
 */
public final class TestSpaceWorlds {

    /** A world of the test mod's, in a system of its own, shaped by the generator and both modifiers. */
    public static final ResourceLocation WORLD = id("test_world");
    public static final ResourceLocation GENERATOR = id("test_slab");
    public static final ResourceLocation RAISE = id("test_raise");
    public static final ResourceLocation PAVE = id("test_pave");
    /** How much the raising modifier adds to the ground. */
    public static final double RAISED_BY = 0.5;
    /** The ground's height range: solid below the middle, air above it. */
    public static final int BOTTOM = 0;
    public static final int TOP = 128;

    private TestSpaceWorlds() {
    }

    /** Registers the generator and the modifiers and declares the world, before the game has loaded. */
    public static void declare() {
        WorldGenerators.register(GENERATOR, new IWorldGenerator() {
            @Override
            public DensityFunction ground(final SpaceWorld world,
                                          final HolderGetter<NormalNoise.NoiseParameters> noises,
                                          final HolderGetter<DensityFunction> densities) {
                return DensityFunctions.yClampedGradient(BOTTOM, TOP, 1.0, -1.0);
            }

            @Override
            public SurfaceRules.RuleSource surface(final SpaceWorld world) {
                return SurfaceRules.state(Blocks.STONE.defaultBlockState());
            }
        });
        WorldModifiers.register(RAISE, new IWorldModifier() {
            @Override
            public DensityFunction ground(final SpaceWorld world, final DensityFunction under,
                                          final HolderGetter<NormalNoise.NoiseParameters> noises,
                                          final HolderGetter<DensityFunction> densities) {
                return DensityFunctions.add(under, DensityFunctions.constant(RAISED_BY));
            }
        });
        WorldModifiers.register(PAVE, new IWorldModifier() {
            @Override
            public SurfaceRules.RuleSource surface(final SpaceWorld world, final SurfaceRules.RuleSource under) {
                return SurfaceRules.sequence(SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState()), under);
            }
        });
        SpaceWorlds.declare(WORLD, new SpaceWorld("jstests.world.test_world", id("test_system"),
                ResourceKey.create(Registries.DIMENSION, WORLD), Optional.of(GENERATOR), List.of(RAISE, PAVE),
                1_000));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
