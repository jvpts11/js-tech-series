/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

/**
 * The ground of a dimension, declared in code and written by the generator as the datapack files the game reads (its
 * noise settings and the noises they sample), so a datapack can change them: how high the land lies, how far hills
 * rise from it and how wide they are, how rough it is, what rock it is made of, what liquid fills it up to its sea,
 * and its surface, biome by biome. The noise that says which biome lies where is its own, two of them, its warmth and
 * its wetness, which a dimension's biomes are placed by ({@code WorldGen.dimension(id).terrain(spec)}).
 *
 * <pre>{@code
 * Terrains.terrain(id("salt_plains")).heights(-64, 256).ground(72).hills(18, 320).sea(63)
 *         .surface(() -> Blocks.SAND, () -> Blocks.SANDSTONE, 4)
 *         .surface(SALT_STEPPE, () -> Blocks.CALCITE, () -> Blocks.DRIPSTONE_BLOCK, 3).declare();
 * }</pre>
 */
public final class Terrains {

    private static final List<TerrainSpec> TERRAINS = Collections.synchronizedList(new ArrayList<>());
    /* The noise that picks biomes changes over a few thousand blocks, as the overworld's does. */
    private static final int CLIMATE_OCTAVE = -10;
    /* A hill's noise, its octaves halving: the broad shape, then the hills on it, then the knolls. */
    private static final double[] HILL_OCTAVES = {1.0, 0.5, 0.25, 0.125};
    private static final int ROUGH_OCTAVE = -5;

    private Terrains() {
    }

    /** Starts declaring a terrain, its noise settings named {@code id}. */
    public static TerrainBuilder terrain(final ResourceLocation id) {
        return new TerrainBuilder(id);
    }

    /** The terrains the mod of that namespace declared. */
    public static List<TerrainSpec> terrainsOf(final String namespace) {
        synchronized (TERRAINS) {
            return TERRAINS.stream().filter(terrain -> terrain.id().getNamespace().equals(namespace)).toList();
        }
    }

    /**
     * A surface: the block on top, the block under it, and how deep that goes.
     *
     * @param biome the biome it is of, or null for every biome without one of its own
     * @param top   the block on top, where the sky or the air meets the ground
     * @param under the block under it, and on a floor under a liquid
     * @param depth how many blocks of the block under it
     */
    public record Surface(@Nullable ResourceKey<Biome> biome, Supplier<? extends Block> top,
                          Supplier<? extends Block> under, int depth) {
    }

    /**
     * A terrain.
     *
     * @param id        what its noise settings are named, and its noises after it
     * @param minY      its lowest block
     * @param height    how many blocks tall it is
     * @param ground    the height the land lies at, on the whole
     * @param hills     how far hills rise from it and valleys fall, at most, in blocks
     * @param hillWidth about how wide a hill is, in blocks
     * @param roughness how much the ground breaks into overhangs and arches, 0 for none
     * @param sea       the height its liquid fills the low ground up to
     * @param stone     the rock it is made of
     * @param liquid    the liquid of its seas
     * @param bedrock   how many blocks of bedrock floor it, fading upwards
     * @param surfaces  its surfaces, a biome's own first and the one for every other biome last
     */
    public record TerrainSpec(ResourceLocation id, int minY, int height, int ground, double hills, int hillWidth,
                              double roughness, int sea, Supplier<? extends Block> stone,
                              Supplier<? extends Block> liquid, int bedrock, List<Surface> surfaces) {

        public TerrainSpec {
            surfaces = List.copyOf(surfaces);
        }

        /** The key of its noise settings. */
        public ResourceKey<NoiseGeneratorSettings> key() {
            return ResourceKey.create(Registries.NOISE_SETTINGS, this.id);
        }

        /** The key of one of its noises: {@code hills}, {@code rough}, {@code warmth} or {@code wetness}. */
        public ResourceKey<NormalNoise.NoiseParameters> noise(final String which) {
            return ResourceKey.create(Registries.NOISE, this.id.withSuffix("_" + which));
        }

        /** Its noises, by their names, as the game registers them. */
        public List<Noise> noises() {
            final int hillOctave = -(int) Math.round(Math.log(this.hillWidth) / Math.log(2));
            return List.of(new Noise("hills", new NormalNoise.NoiseParameters(hillOctave, HILL_OCTAVES[0],
                            HILL_OCTAVES[1], HILL_OCTAVES[2], HILL_OCTAVES[3])),
                    new Noise("rough", new NormalNoise.NoiseParameters(ROUGH_OCTAVE, 1.0, 0.5)),
                    new Noise("warmth", new NormalNoise.NoiseParameters(CLIMATE_OCTAVE, 1.0, 1.0, 0.5)),
                    new Noise("wetness", new NormalNoise.NoiseParameters(CLIMATE_OCTAVE, 1.0, 1.0, 0.5)));
        }

        /**
         * Its noise settings, as the game registers them: the ground solid wherever the land's height, raised and
         * lowered by the hills' noise, is above a block, broken by the roughness, the liquid below its sea, and the
         * surfaces laid on it.
         */
        public NoiseGeneratorSettings settings(final HolderGetter<NormalNoise.NoiseParameters> noises) {
            final int top = this.minY + this.height;
            final DensityFunction slope = DensityFunctions.yClampedGradient(this.minY, top,
                    (this.ground - this.minY) / this.hills, (this.ground - top) / this.hills);
            final DensityFunction hilly = DensityFunctions.flatCache(DensityFunctions.noise(
                    noises.getOrThrow(noise("hills")), 1.0, 0.0));
            DensityFunction density = DensityFunctions.add(slope, hilly);
            if (this.roughness > 0) {
                density = DensityFunctions.add(density, DensityFunctions.mul(DensityFunctions.constant(this.roughness),
                        DensityFunctions.noise(noises.getOrThrow(noise("rough")), 1.0, 1.0)));
            }
            final DensityFunction warmth = DensityFunctions.flatCache(DensityFunctions.noise(
                    noises.getOrThrow(noise("warmth")), 1.0, 0.0));
            final DensityFunction wetness = DensityFunctions.flatCache(DensityFunctions.noise(
                    noises.getOrThrow(noise("wetness")), 1.0, 0.0));
            final DensityFunction none = DensityFunctions.zero();
            final NoiseRouter router = new NoiseRouter(none, none, none, none, warmth, wetness, none, none, none,
                    none, density, DensityFunctions.interpolated(density), none, none, none);
            return new NoiseGeneratorSettings(NoiseSettings.create(this.minY, this.height, 1, 2),
                    this.stone.get().defaultBlockState(), this.liquid.get().defaultBlockState(), router,
                    surfaceRule(), List.of(), this.sea, false, false, false, false);
        }

        private SurfaceRules.RuleSource surfaceRule() {
            final List<SurfaceRules.RuleSource> rules = new ArrayList<>();
            if (this.bedrock > 0) {
                rules.add(SurfaceRules.ifTrue(SurfaceRules.verticalGradient(this.id + "_bedrock",
                        VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(this.bedrock)),
                        SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())));
            }
            for (final Surface surface : this.surfaces) {
                final SurfaceRules.RuleSource laid = laid(surface);
                rules.add(surface.biome() == null ? laid
                        : SurfaceRules.ifTrue(SurfaceRules.isBiome(surface.biome()), laid));
            }
            return SurfaceRules.sequence(rules.toArray(SurfaceRules.RuleSource[]::new));
        }

        /* The top on a floor open to the air, the block under it on a floor under a liquid and below either. */
        private static SurfaceRules.RuleSource laid(final Surface surface) {
            final SurfaceRules.RuleSource top = SurfaceRules.state(surface.top().get().defaultBlockState());
            final SurfaceRules.RuleSource under = SurfaceRules.state(surface.under().get().defaultBlockState());
            return SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), top), under)),
                    SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(surface.depth(), false, CaveSurface.FLOOR),
                            under));
        }
    }

    /**
     * A noise of a terrain.
     *
     * @param name       what it is named after the terrain
     * @param parameters its octaves
     */
    public record Noise(String name, NormalNoise.NoiseParameters parameters) {
    }

    /** A terrain being declared; it starts as gentle hills of stone over a sea of water, as tall as the overworld. */
    public static final class TerrainBuilder {

        private final ResourceLocation id;
        private final List<Surface> surfaces = new ArrayList<>();
        private int minY = -64;
        private int height = 384;
        private int ground = 72;
        private double hills = 16;
        private int hillWidth = 256;
        private double roughness;
        private int sea = 63;
        private Supplier<? extends Block> stone = () -> Blocks.STONE;
        private Supplier<? extends Block> liquid = () -> Blocks.WATER;
        private int bedrock = 5;
        private Surface fallback = new Surface(null, () -> Blocks.GRASS_BLOCK, () -> Blocks.DIRT, 3);

        private TerrainBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public TerrainBuilder heights(final int lowest, final int blocksTall) {
            this.minY = lowest;
            this.height = blocksTall;
            return this;
        }

        /** The land lies at {@code y} on the whole. */
        public TerrainBuilder ground(final int y) {
            this.ground = y;
            return this;
        }

        /** Hills rise and valleys fall up to {@code blocks} from the ground, each about {@code width} blocks wide. */
        public TerrainBuilder hills(final double blocks, final int width) {
            this.hills = blocks;
            this.hillWidth = width;
            return this;
        }

        /** The ground breaks into overhangs and arches, more the larger {@code share} is; 0.1 is a little. */
        public TerrainBuilder rough(final double share) {
            this.roughness = share;
            return this;
        }

        /** Its liquid fills the low ground up to {@code y}. */
        public TerrainBuilder sea(final int y) {
            this.sea = y;
            return this;
        }

        public TerrainBuilder stone(final Supplier<? extends Block> rock) {
            this.stone = rock;
            return this;
        }

        public TerrainBuilder liquid(final Supplier<? extends Block> seas) {
            this.liquid = seas;
            return this;
        }

        /** Floored by {@code blocks} of bedrock, fading upwards; 0 for none. */
        public TerrainBuilder bedrock(final int blocks) {
            this.bedrock = blocks;
            return this;
        }

        /** The surface of every biome without one of its own. */
        public TerrainBuilder surface(final Supplier<? extends Block> top, final Supplier<? extends Block> under,
                                      final int depth) {
            this.fallback = new Surface(null, top, under, depth);
            return this;
        }

        /** The surface of {@code biome}. */
        public TerrainBuilder surface(final ResourceKey<Biome> biome, final Supplier<? extends Block> top,
                                      final Supplier<? extends Block> under, final int depth) {
            this.surfaces.add(new Surface(Objects.requireNonNull(biome, "biome"), top, under, depth));
            return this;
        }

        /**
         * Declares the terrain.
         *
         * @throws IllegalStateException when its heights are not whole sections, or its ground or hills make no land
         */
        public TerrainSpec declare() {
            if (height < 16 || height % 16 != 0 || minY % 16 != 0) {
                throw new IllegalStateException("the terrain " + id + " is a whole number of sections tall");
            }
            if (hills <= 0 || hillWidth < 2 || ground <= minY || ground >= minY + height) {
                throw new IllegalStateException("the terrain " + id + " has its ground inside it, and some hills");
            }
            final List<Surface> all = new ArrayList<>(surfaces);
            all.add(fallback);
            final TerrainSpec spec = new TerrainSpec(id, minY, height, ground, hills, hillWidth, roughness, sea,
                    stone, liquid, bedrock, all);
            TERRAINS.add(spec);
            return spec;
        }
    }
}
