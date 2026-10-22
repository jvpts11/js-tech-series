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
 *
 * <p>A terrain that wants more than hills declares its parts one by one, each written as a file of its own: its
 * noises ({@link #noise}), the density functions its ground is made of ({@link #density}, made from the noises and
 * from one another), and its surface as rules read in order ({@link #surfaceRules}).
 *
 * <pre>{@code
 * NoiseSpec RIDGE = Terrains.noise(id("ridge"), -7, 1.0, 0.5);
 * DensitySpec RIDGED = Terrains.density(id("ridged_ground"), (noises, densities) -> DensityFunctions.add(
 *         DensityFunctions.yClampedGradient(0, 256, 2.0, -2.0),
 *         DensityFunctions.noise(noises.getOrThrow(RIDGE.key()), 1.0, 0.0).abs()));
 * Terrains.terrain(id("ridges")).heights(0, 256).ground(96).density(RIDGED)
 *         .surfaceRules(Terrains.surfaceRules().above(160, () -> Blocks.SNOW_BLOCK).steep(() -> Blocks.STONE)
 *                 .layers(() -> Blocks.GRASS_BLOCK, () -> Blocks.DIRT, 3)).declare();
 * }</pre>
 */
public final class Terrains {

    private static final List<TerrainSpec> TERRAINS = Collections.synchronizedList(new ArrayList<>());
    private static final List<NoiseSpec> NOISES = Collections.synchronizedList(new ArrayList<>());
    private static final List<DensitySpec> DENSITIES = Collections.synchronizedList(new ArrayList<>());
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

    /**
     * Declares a noise named {@code id}: its broadest octave {@code firstOctave}, a feature about two to the minus that
     * blocks across, and how much each octave counts from there, each finer than the last.
     */
    public static NoiseSpec noise(final ResourceLocation id, final int firstOctave, final double... amplitudes) {
        final List<Double> each = new ArrayList<>(amplitudes.length);
        for (final double amplitude : amplitudes) {
            each.add(amplitude);
        }
        final NoiseSpec spec = new NoiseSpec(id, firstOctave, each);
        NOISES.add(spec);
        return spec;
    }

    /** Declares a density function named {@code id}, made by {@code recipe} from the noises and the others. */
    public static DensitySpec density(final ResourceLocation id, final IDensityRecipe recipe) {
        final DensitySpec spec = new DensitySpec(id, recipe);
        DENSITIES.add(spec);
        return spec;
    }

    /** Starts a terrain's surface as rules read in order; none, until some are given. */
    public static SurfaceRuleBuilder surfaceRules() {
        return new SurfaceRuleBuilder();
    }

    /** The terrains the mod of that namespace declared. */
    public static List<TerrainSpec> terrainsOf(final String namespace) {
        synchronized (TERRAINS) {
            return TERRAINS.stream().filter(terrain -> terrain.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The noises the mod of that namespace declared, beside those of its terrains. */
    public static List<NoiseSpec> noisesOf(final String namespace) {
        synchronized (NOISES) {
            return NOISES.stream().filter(noise -> noise.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The density functions the mod of that namespace declared. */
    public static List<DensitySpec> densitiesOf(final String namespace) {
        synchronized (DENSITIES) {
            return DENSITIES.stream().filter(density -> density.id().getNamespace().equals(namespace)).toList();
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
     * @param density   the density function its ground is, or null for its hills
     * @param rules     its surface as rules, or null for its surfaces
     */
    public record TerrainSpec(ResourceLocation id, int minY, int height, int ground, double hills, int hillWidth,
                              double roughness, int sea, Supplier<? extends Block> stone,
                              Supplier<? extends Block> liquid, int bedrock, List<Surface> surfaces,
                              @Nullable DensitySpec density, @Nullable SurfaceRuleBuilder rules) {

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

        /**
         * Its noises, by their names, as the game registers them: the two its biomes are placed by, and, for ground of
         * hills, the hills' and the roughness's; ground of its own density function samples noises of its own.
         */
        public List<Noise> noises() {
            final List<Noise> out = new ArrayList<>();
            if (this.density == null) {
                final int hillOctave = -(int) Math.round(Math.log(this.hillWidth) / Math.log(2));
                out.add(new Noise("hills", new NormalNoise.NoiseParameters(hillOctave, HILL_OCTAVES[0],
                        HILL_OCTAVES[1], HILL_OCTAVES[2], HILL_OCTAVES[3])));
                out.add(new Noise("rough", new NormalNoise.NoiseParameters(ROUGH_OCTAVE, 1.0, 0.5)));
            }
            out.add(new Noise("warmth", new NormalNoise.NoiseParameters(CLIMATE_OCTAVE, 1.0, 1.0, 0.5)));
            out.add(new Noise("wetness", new NormalNoise.NoiseParameters(CLIMATE_OCTAVE, 1.0, 1.0, 0.5)));
            return List.copyOf(out);
        }

        /**
         * Its noise settings, as the game registers them: the ground solid wherever the land's height, raised and
         * lowered by the hills' noise, is above a block, broken by the roughness, or wherever its own density function
         * says; the liquid below its sea, and its surfaces laid on it, or its rules.
         */
        public NoiseGeneratorSettings settings(final HolderGetter<NormalNoise.NoiseParameters> noises,
                                               final HolderGetter<DensityFunction> densities) {
            final DensityFunction density = this.density != null ? this.density.reference(densities) : hills(noises);
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

        /* The land's height, raised and lowered by the hills' noise, broken by the roughness. */
        private DensityFunction hills(final HolderGetter<NormalNoise.NoiseParameters> noises) {
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
            return density;
        }

        /* Its rules when it was given them; else bedrock, then each surface, a biome's own before the rest's. */
        private SurfaceRules.RuleSource surfaceRule() {
            if (this.rules != null) {
                return this.rules.build();
            }
            final SurfaceRuleBuilder laid = new SurfaceRuleBuilder();
            if (this.bedrock > 0) {
                laid.bedrock(this.id + "_bedrock", this.bedrock);
            }
            for (final Surface surface : this.surfaces) {
                if (surface.biome() == null) {
                    laid.layers(surface.top(), surface.under(), surface.depth());
                } else {
                    laid.in(surface.biome(), new SurfaceRuleBuilder().layers(surface.top(), surface.under(),
                            surface.depth()));
                }
            }
            return laid.build();
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
        private @Nullable DensitySpec density;
        private @Nullable SurfaceRuleBuilder rules;

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

        /** Its ground is {@code ground}, a declared density function, solid where it is above zero, not its hills. */
        public TerrainBuilder density(final DensitySpec ground) {
            this.density = Objects.requireNonNull(ground, "ground");
            return this;
        }

        /** Its surface is these rules, read in order, not its surfaces; its bedrock goes in them if wanted. */
        public TerrainBuilder surfaceRules(final SurfaceRuleBuilder given) {
            this.rules = Objects.requireNonNull(given, "given");
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
                    stone, liquid, bedrock, all, density, rules);
            TERRAINS.add(spec);
            return spec;
        }
    }
}
