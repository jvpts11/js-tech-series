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
import dev.jstech.space.api.SpaceLandmark;
import dev.jstech.space.api.SpaceLandmarks;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.geology.SpaceRocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * {@code jsspace:cratered}: the ground of an airless world of maria and highlands. Its heights are read from the
 * world's own height map ({@code <world>/height}, as its sphere is drawn), squeezed into the world's height, its
 * great craters are the landmarks declared on it, each where its sphere draws it, and small bumps roughen it. On it
 * lie a mare's regolith over mare basalt and the highlands' over anorthosite, the rock bare on the steep walls.
 *
 * <p>The craters by the thousand, the rilles and the lava tubes are the world's landforms, laid on this ground in the
 * biomes they belong to.
 */
public final class CrateredGenerator implements IWorldGenerator {

    /** The lowest block of a cratered world, how tall it is, and the height its ground lies at on the whole. */
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int GROUND = 64;
    /** How far the world's height map raises its highest ground and sinks its lowest, in blocks. */
    public static final double RELIEF = 48;
    /** How far the small bumps raise and sink the ground, in blocks. */
    public static final double BUMPS = 3;
    /** A great crater's bowl, in blocks deep for each degree of its radius: the world's height squeezes it. */
    public static final double DEPTH_PER_DEGREE = 27;
    /* A great crater's rim and central peak, as shares of its depth. */
    private static final double RIM = 0.43;
    private static final double PEAK = 0.57;
    /* How deep a mare's basalt lies under its regolith, and the regolith over each rock. */
    private static final int MARE_BASALT = 12;
    private static final int REGOLITH = 3;

    /** The small bumps of the ground, a few dozen blocks across. */
    public static final NoiseSpec BUMP_NOISE = Terrains.noise(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "cratered_bumps"), -6, 1.0, 0.5, 0.25);

    @Override
    public DensityFunction ground(final SpaceWorld world, final HolderGetter<NormalNoise.NoiseParameters> noises,
                                  final HolderGetter<DensityFunction> densities) {
        final ResourceLocation dimension = world.dimension().location();
        DensityFunction height = DensityFunctions.add(
                CoreDensities.roundMap(map(world, "height"), dimension, -RELIEF, RELIEF),
                DensityFunctions.mul(DensityFunctions.constant(BUMPS),
                        DensityFunctions.noise(noises.getOrThrow(BUMP_NOISE.key()), 1.0, 0.0)));
        for (final SpaceLandmark landmark : SpaceLandmarks.declared().values()) {
            if (landmark.kind() == SpaceLandmark.Kind.CRATER && on(landmark, world)) {
                final double depth = landmark.radius() * DEPTH_PER_DEGREE;
                height = DensityFunctions.add(height, CoreDensities.roundCrater(dimension, landmark.longitude(),
                        landmark.latitude(), landmark.radius(), depth, depth * RIM, depth * PEAK));
            }
        }
        // Solid where the ground's height is above the block: a sixteenth of a block of density for each block of it.
        final double top = MIN_Y + HEIGHT;
        return DensityFunctions.add(
                DensityFunctions.yClampedGradient(MIN_Y, (int) top, (GROUND - MIN_Y) / 16.0, (GROUND - top) / 16.0),
                DensityFunctions.mul(DensityFunctions.constant(1 / 16.0), DensityFunctions.flatCache(height)));
    }

    @Override
    public SurfaceRules.RuleSource surface(final SpaceWorld world) {
        final SurfaceRuleBuilder maria = Terrains.surfaceRules().steep(SpaceRocks.MARE_BASALT)
                .layers(SpaceRocks.MARE_REGOLITH, SpaceRocks.MARE_BASALT, MARE_BASALT);
        return Terrains.surfaceRules()
                .in(MoonBiomes.MARE.key(), maria)
                .in(MoonBiomes.RILLE.key(), maria)
                .in(MoonBiomes.LAVA_TUBE_SKYLIGHT.key(), maria)
                .steep(SpaceRocks.ANORTHOSITE)
                .layers(SpaceRocks.HIGHLAND_REGOLITH, SpaceRocks.ANORTHOSITE, REGOLITH)
                .build();
    }

    /** The map {@code which} of {@code world}: {@code jsspace:moon/height} for the Moon's heights. */
    public static ResourceLocation map(final SpaceWorld world, final String which) {
        final ResourceLocation dimension = world.dimension().location();
        return dimension.withPath(dimension.getPath() + "/" + which);
    }

    /* Whether {@code landmark} lies on {@code world}. */
    private static boolean on(final SpaceLandmark landmark, final SpaceWorld world) {
        final SpaceWorld its = SpaceWorlds.declared().get(landmark.world());
        return its != null && its.dimension().equals(world.dimension());
    }
}
