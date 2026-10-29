/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.worldgen;

import dev.jstech.core.worldgen.terrain.NoiseSpec;
import dev.jstech.core.worldgen.terrain.SurfaceRuleBuilder;
import dev.jstech.core.worldgen.terrain.Terrains;
import dev.jstech.core.worldgen.terrain.density.CoreDensities;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.IWorldGenerator;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.geology.SpaceRocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * {@code jsspace:arid}: the ground of a cold desert world of thin air. Its heights are read from the world's own height
 * map ({@code <world>/height}, as its sphere is drawn), squeezed into the world's height, its volcanoes standing and
 * its canyons cut where the sphere shows them, and small bumps roughen it. On it lie red regolith over basalt, the
 * dark sand of its dunes, the hematite of its oxide plains, the mudstone of its old lakes and broken chaos, and the
 * ices of its caps; the rock bare on the steep walls, varied there by geology.
 */
public final class AridGenerator implements IWorldGenerator {

    /** How far the world's height map raises its highest ground and sinks its lowest, in blocks. */
    public static final double RELIEF = 80;
    /** How far the small bumps raise and sink the ground, in blocks. */
    public static final double BUMPS = 2;
    /* The regolith over the basalt, the mudstone laid by old water, and the water ice of a cap under its frost. */
    private static final int REGOLITH = 3;
    private static final int MUDSTONE = 14;
    private static final int ICE = 24;
    private static final int SAND = 4;

    /** The small bumps of the ground, a few dozen blocks across. */
    public static final NoiseSpec BUMP_NOISE = Terrains.noise(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "arid_bumps"), -6, 1.0, 0.5, 0.25);

    @Override
    public DensityFunction ground(final SpaceWorld world, final HolderGetter<NormalNoise.NoiseParameters> noises,
                                  final HolderGetter<DensityFunction> densities) {
        final DensityFunction height = DensityFunctions.add(
                CoreDensities.roundMap(CrateredGenerator.map(world, "height"), world.dimension().location(), -RELIEF,
                        RELIEF),
                DensityFunctions.mul(DensityFunctions.constant(BUMPS),
                        DensityFunctions.noise(noises.getOrThrow(BUMP_NOISE.key()), 1.0, 0.0)));
        // Solid where the ground's height is above the block: a sixteenth of a block of density for each block of it.
        final int top = CrateredGenerator.MIN_Y + CrateredGenerator.HEIGHT;
        return DensityFunctions.add(
                DensityFunctions.yClampedGradient(CrateredGenerator.MIN_Y, top,
                        (CrateredGenerator.GROUND - CrateredGenerator.MIN_Y) / 16.0,
                        (CrateredGenerator.GROUND - top) / 16.0),
                DensityFunctions.mul(DensityFunctions.constant(1 / 16.0), DensityFunctions.flatCache(height)));
    }

    @Override
    public SurfaceRules.RuleSource surface(final SpaceWorld world) {
        return Terrains.surfaceRules()
                .in(MarsBiomes.POLAR_CAP.key(), Terrains.surfaceRules().steep(SpaceRocks.WATER_ICE)
                        .layers(SpaceRocks.DRY_ICE, SpaceRocks.WATER_ICE, ICE))
                .in(MarsBiomes.DARK_DUNES.key(), Terrains.surfaceRules()
                        .layers(SpaceRocks.BASALT_SAND, SpaceRocks.BASALT_SAND, SAND))
                .in(MarsBiomes.OXIDE_PLAINS.key(), Terrains.surfaceRules().steep(SpaceRocks.MARTIAN_BASALT)
                        .layers(SpaceRocks.HEMATITE_REGOLITH, SpaceRocks.MARTIAN_REGOLITH, REGOLITH))
                .in(MarsBiomes.LAKEBED.key(), mudstone(Terrains.surfaceRules()
                        .layers(SpaceRocks.MUDSTONE, SpaceRocks.MUDSTONE, MUDSTONE)))
                .in(MarsBiomes.CHAOS_TERRAIN.key(), mudstone(Terrains.surfaceRules()
                        .layers(SpaceRocks.MARTIAN_REGOLITH, SpaceRocks.MUDSTONE, MUDSTONE)))
                .steep(SpaceRocks.MARTIAN_BASALT)
                .layers(SpaceRocks.MARTIAN_REGOLITH, SpaceRocks.MARTIAN_REGOLITH, REGOLITH)
                .build();
    }

    /* {@code rules} with the mudstone bare on the steep walls first. */
    private static SurfaceRuleBuilder mudstone(final SurfaceRuleBuilder rules) {
        return Terrains.surfaceRules().steep(SpaceRocks.MUDSTONE).rule(rules::build);
    }
}
