/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen;

import dev.jstech.core.dimension.RuntimeDimensions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jetbrains.annotations.Nullable;

/**
 * What a mod adds to the generation of the world: veins of ore and structures built from a saved template, declared
 * once and written by the generator as the datapack files the game reads (the features, the biome modifiers, the
 * template pools, the structures and their sets), so a datapack can change any of them.
 *
 * <pre>{@code
 * WorldGen.ore(id("tin_ore")).stone(TIN_ORE).deepslate(DEEPSLATE_TIN_ORE).vein(9).perChunk(12)
 *         .heights(-32, 64).triangle().biomes(BiomeTags.IS_OVERWORLD).declare();
 * WorldGen.structure(id("relay_hut")).template(id("relay_hut")).biomes(BiomeTags.IS_OVERWORLD)
 *         .spacing(48, 20).salt(482_919).declare();
 * }</pre>
 */
public final class WorldGen {

    private static final List<OreVein> ORES = Collections.synchronizedList(new ArrayList<>());
    private static final List<TemplateStructure> STRUCTURES = Collections.synchronizedList(new ArrayList<>());
    private static final List<DimensionSpec> DIMENSIONS = Collections.synchronizedList(new ArrayList<>());

    private WorldGen() {
    }

    /**
     * Starts declaring a dimension, its type and its level stem both named {@code id}: the world loads it with every
     * other, and {@link RuntimeDimensions} makes more of it while the game runs. The game counts a world of more than
     * its own three dimensions as "experimental" and warns each time it is opened; a dimension that is only ever
     * copied is declared {@link DimensionBuilder#runtimeOnly()} and adds none.
     */
    public static DimensionBuilder dimension(final ResourceLocation id) {
        return new DimensionBuilder(id);
    }

    /** The dimensions the mod of that namespace declared. */
    public static List<DimensionSpec> dimensionsOf(final String namespace) {
        synchronized (DIMENSIONS) {
            return DIMENSIONS.stream().filter(dimension -> dimension.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** Starts declaring a vein of ore, its feature, placement and biome modifier all named {@code id}. */
    public static OreBuilder ore(final ResourceLocation id) {
        return new OreBuilder(id);
    }

    /** Starts declaring a structure of one saved template, its pool, structure and set all named {@code id}. */
    public static StructureBuilder structure(final ResourceLocation id) {
        return new StructureBuilder(id);
    }

    /** The veins the mod of that namespace declared. */
    public static List<OreVein> oresOf(final String namespace) {
        synchronized (ORES) {
            return ORES.stream().filter(ore -> ore.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The structures the mod of that namespace declared. */
    public static List<TemplateStructure> structuresOf(final String namespace) {
        synchronized (STRUCTURES) {
            return STRUCTURES.stream().filter(structure -> structure.id().getNamespace().equals(namespace))
                    .toList();
        }
    }

    /** Whether the mod of that namespace declared anything for the generation of the world. */
    public static boolean declares(final String namespace) {
        return !oresOf(namespace).isEmpty() || !structuresOf(namespace).isEmpty()
                || !dimensionsOf(namespace).isEmpty();
    }

    /**
     * One layer of a flat dimension's ground.
     *
     * @param block     what it is made of
     * @param thickness how many blocks thick
     */
    public record Layer(Supplier<? extends Block> block, int thickness) {
    }

    /**
     * A dimension: what kind of place it is, and how its ground is made.
     *
     * @param id           what its type and its level stem are named
     * @param skylight     whether the sky lights it
     * @param ceiling      whether it has a ceiling of bedrock overhead, as the Nether does
     * @param fixedTime    the time of day it always stands at, or empty for days that pass
     * @param ambientLight how bright it is without any light, from 0 to 1
     * @param minY         the lowest block
     * @param height       how many blocks tall it is, from {@code minY}
     * @param effects      how its sky and fog are drawn: the overworld's, the Nether's or the End's
     * @param ultraWarm    whether water boils away in it
     * @param natural      whether compasses and clocks work, and beds and portals behave as in the overworld
     * @param bedWorks     whether a bed can be slept in
     * @param biome        the one biome it is all of
     * @param layers       a flat dimension's ground, lowest layer first; empty for ground made by {@code noise}
     * @param noise        the noise settings its ground is made with, when it is not flat
     * @param runtimeOnly  whether only its copies made while the game runs exist, the world loading none of it
     */
    public record DimensionSpec(ResourceLocation id, boolean skylight, boolean ceiling, OptionalLong fixedTime,
                                float ambientLight, int minY, int height, ResourceLocation effects,
                                boolean ultraWarm, boolean natural, boolean bedWorks, ResourceKey<Biome> biome,
                                List<Layer> layers, @Nullable ResourceKey<NoiseGeneratorSettings> noise,
                                boolean runtimeOnly) {

        public DimensionSpec {
            layers = List.copyOf(layers);
        }

        /**
         * The kind of place it is, as the game registers it: monsters spawn only in darkness, as in the overworld; no
         * piglins are safe and no raids come.
         */
        public DimensionType dimensionType() {
            return new DimensionType(fixedTime, skylight, ceiling, ultraWarm, natural, 1.0, bedWorks, false, minY,
                    height, height, BlockTags.INFINIBURN_OVERWORLD, effects, ambientLight,
                    new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0));
        }

        /** How its ground is made: flat layers over its one biome, or noise over it. */
        public ChunkGenerator generator(final HolderGetter<Biome> biomes,
                                        final HolderGetter<NoiseGeneratorSettings> noiseSettings) {
            final Holder<Biome> only = biomes.getOrThrow(biome);
            if (noise != null) {
                return new NoiseBasedChunkGenerator(new FixedBiomeSource(only), noiseSettings.getOrThrow(noise));
            }
            final List<FlatLayerInfo> flat = new ArrayList<>();
            for (final Layer layer : layers) {
                flat.add(new FlatLayerInfo(layer.thickness(), layer.block().get()));
            }
            return new FlatLevelSource(new FlatLevelGeneratorSettings(Optional.empty(), only, List.of())
                    .withBiomeAndLayers(flat, Optional.empty(), only));
        }
    }

    /** The dimension declared as {@code id}, by any mod, or none. */
    public static Optional<DimensionSpec> declaredDimension(final ResourceLocation id) {
        synchronized (DIMENSIONS) {
            return DIMENSIONS.stream().filter(dimension -> dimension.id().equals(id)).findFirst();
        }
    }

    /** A dimension being declared; it starts as a copy of the overworld's kind of place, with flat ground. */
    public static final class DimensionBuilder {

        private final ResourceLocation id;
        private final List<Layer> layers = new ArrayList<>();
        private boolean skylight = true;
        private boolean ceiling;
        private OptionalLong fixedTime = OptionalLong.empty();
        private float ambientLight;
        private int minY = -64;
        private int height = 384;
        private ResourceLocation effects = BuiltinDimensionTypes.OVERWORLD_EFFECTS;
        private boolean ultraWarm;
        private boolean natural = true;
        private boolean bedWorks = true;
        private ResourceKey<Biome> biome = Biomes.PLAINS;
        private @Nullable ResourceKey<NoiseGeneratorSettings> noise;
        private boolean runtimeOnly;

        private DimensionBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Only copies of it, made while the game runs, exist: a pocket world, an instance of a dungeon. The world
         * loads none of it as it starts, so it stays a world of the game's own three dimensions, which the game
         * otherwise warns about as "experimental" each time it is opened.
         */
        public DimensionBuilder runtimeOnly() {
            this.runtimeOnly = true;
            return this;
        }

        /** Adds a layer of flat ground over the ones before it. */
        public DimensionBuilder layer(final Supplier<? extends Block> block, final int thickness) {
            layers.add(new Layer(block, thickness));
            return this;
        }

        /** Makes its ground with those noise settings, as the overworld's is made, rather than in flat layers. */
        public DimensionBuilder noise(final ResourceKey<NoiseGeneratorSettings> settings) {
            this.noise = settings;
            return this;
        }

        public DimensionBuilder biome(final ResourceKey<Biome> only) {
            this.biome = only;
            return this;
        }

        /** No sky over it: no daylight, no rain, no sun or moon. */
        public DimensionBuilder noSkylight() {
            this.skylight = false;
            return this;
        }

        public DimensionBuilder ceiling() {
            this.ceiling = true;
            return this;
        }

        /** Always at that time of day, in ticks from sunrise: 6000 is noon, 18000 midnight. */
        public DimensionBuilder fixedTime(final long ticks) {
            this.fixedTime = OptionalLong.of(ticks);
            return this;
        }

        public DimensionBuilder ambientLight(final float light) {
            this.ambientLight = light;
            return this;
        }

        public DimensionBuilder heights(final int lowest, final int blocksTall) {
            this.minY = lowest;
            this.height = blocksTall;
            return this;
        }

        /** Its sky and fog drawn as that dimension's are: one of the effects of {@code BuiltinDimensionTypes}. */
        public DimensionBuilder effects(final ResourceLocation drawnLike) {
            this.effects = drawnLike;
            return this;
        }

        public DimensionBuilder ultraWarm() {
            this.ultraWarm = true;
            return this;
        }

        /** Compasses and clocks spin, and beds do not hold a spawn, as away from the overworld. */
        public DimensionBuilder unnatural() {
            this.natural = false;
            this.bedWorks = false;
            return this;
        }

        /**
         * Declares the dimension.
         *
         * @throws IllegalStateException when its ground is neither flat nor made by noise, or its heights are not
         *                               ones the game can hold
         */
        public DimensionSpec declare() {
            if (layers.isEmpty() == (noise == null)) {
                throw new IllegalStateException("the dimension " + id + " has flat layers or noise, one of them");
            }
            if (height < 16 || height % 16 != 0 || minY % 16 != 0) {
                throw new IllegalStateException("the dimension " + id + " is a whole number of sections tall");
            }
            if (minY < DimensionType.MIN_Y || height > DimensionType.Y_SIZE
                    || (long) minY + height > DimensionType.MAX_Y + 1L) {
                throw new IllegalStateException("the dimension " + id + " is from y " + minY + " for " + height
                        + " blocks, past what the game holds: from " + DimensionType.MIN_Y + " to "
                        + (DimensionType.MAX_Y + 1) + ", at most " + DimensionType.Y_SIZE + " tall");
            }
            final DimensionSpec spec = new DimensionSpec(id, skylight, ceiling, fixedTime, ambientLight, minY, height,
                    effects, ultraWarm, natural, bedWorks, biome, layers, noise, runtimeOnly);
            DIMENSIONS.add(spec);
            return spec;
        }
    }

    /**
     * A vein of ore.
     *
     * @param id        what its feature, placement and biome modifier are named
     * @param stone     the ore that replaces stone
     * @param deepslate the ore that replaces deepslate, or null to leave deepslate alone
     * @param veinSize  how many blocks a vein holds at most
     * @param perChunk  how many veins a chunk is tried for
     * @param minY      the lowest a vein is placed, in blocks
     * @param maxY      the highest
     * @param triangle  whether veins gather round the middle of the range rather than spread through it evenly
     * @param biomes    where it is placed
     */
    public record OreVein(ResourceLocation id, Supplier<? extends Block> stone,
                          @Nullable Supplier<? extends Block> deepslate, int veinSize, int perChunk, int minY,
                          int maxY, boolean triangle, TagKey<Biome> biomes) {
    }

    /**
     * A structure of one saved template, set down on the surface.
     *
     * @param id         what its pool, structure and set are named
     * @param template   the template it is built from, a file under {@code data/<namespace>/structure/}
     * @param biomes     where it is placed
     * @param spacing    the size, in chunks, of the cells the world is cut into, one structure tried in each
     * @param separation the fewest chunks between two of them
     * @param salt       what keeps its spread apart from other structures' on the same seed
     * @param step       when in the generation it is placed
     * @param terrain    how the ground under and round it is shaped to it
     * @param heightmap  the surface it is set on
     */
    public record TemplateStructure(ResourceLocation id, ResourceLocation template, TagKey<Biome> biomes, int spacing,
                                    int separation, int salt, GenerationStep.Decoration step,
                                    TerrainAdjustment terrain, Heightmap.Types heightmap) {
    }

    /** A vein of ore being declared. */
    public static final class OreBuilder {

        private final ResourceLocation id;
        private @Nullable Supplier<? extends Block> stone;
        private @Nullable Supplier<? extends Block> deepslate;
        private int veinSize = 8;
        private int perChunk = 8;
        private int minY = -64;
        private int maxY = 64;
        private boolean triangle;
        private @Nullable TagKey<Biome> biomes;

        private OreBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public OreBuilder stone(final Supplier<? extends Block> ore) {
            this.stone = ore;
            return this;
        }

        public OreBuilder deepslate(final Supplier<? extends Block> ore) {
            this.deepslate = ore;
            return this;
        }

        public OreBuilder vein(final int size) {
            this.veinSize = size;
            return this;
        }

        public OreBuilder perChunk(final int count) {
            this.perChunk = count;
            return this;
        }

        public OreBuilder heights(final int lowest, final int highest) {
            this.minY = lowest;
            this.maxY = highest;
            return this;
        }

        /** Gathers the veins round the middle of the range, as most of the game's ores are. */
        public OreBuilder triangle() {
            this.triangle = true;
            return this;
        }

        public OreBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /**
         * Declares the vein.
         *
         * @throws IllegalStateException when it names no ore or no biomes, or its numbers make no vein
         */
        public OreVein declare() {
            if (stone == null || biomes == null) {
                throw new IllegalStateException("the vein " + id + " names its ore and its biomes");
            }
            if (veinSize < 1 || perChunk < 1 || minY > maxY) {
                throw new IllegalStateException("the vein " + id + " has a size, a count and a range of heights");
            }
            final OreVein vein = new OreVein(id, stone, deepslate, veinSize, perChunk, minY, maxY, triangle, biomes);
            ORES.add(vein);
            return vein;
        }
    }

    /** A structure being declared. */
    public static final class StructureBuilder {

        private final ResourceLocation id;
        private @Nullable ResourceLocation template;
        private @Nullable TagKey<Biome> biomes;
        private int spacing = 32;
        private int separation = 8;
        private int salt;
        private GenerationStep.Decoration step = GenerationStep.Decoration.SURFACE_STRUCTURES;
        private TerrainAdjustment terrain = TerrainAdjustment.BEARD_THIN;
        private Heightmap.Types heightmap = Heightmap.Types.WORLD_SURFACE_WG;

        private StructureBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.salt = id.hashCode() & Integer.MAX_VALUE;
        }

        public StructureBuilder template(final ResourceLocation saved) {
            this.template = saved;
            return this;
        }

        public StructureBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /** One tried in each cell of {@code cells} chunks, never closer than {@code apart} chunks. */
        public StructureBuilder spacing(final int cells, final int apart) {
            this.spacing = cells;
            this.separation = apart;
            return this;
        }

        public StructureBuilder salt(final int value) {
            this.salt = value;
            return this;
        }

        public StructureBuilder step(final GenerationStep.Decoration when) {
            this.step = when;
            return this;
        }

        public StructureBuilder terrain(final TerrainAdjustment shaping) {
            this.terrain = shaping;
            return this;
        }

        public StructureBuilder surface(final Heightmap.Types surface) {
            this.heightmap = surface;
            return this;
        }

        /**
         * Declares the structure.
         *
         * @throws IllegalStateException when it names no template or no biomes, or its separation is not less than
         *                               its spacing
         */
        public TemplateStructure declare() {
            if (template == null || biomes == null) {
                throw new IllegalStateException("the structure " + id + " names its template and its biomes");
            }
            if (separation >= spacing || separation < 0) {
                throw new IllegalStateException("the structure " + id + " keeps its separation under its spacing");
            }
            final TemplateStructure structure = new TemplateStructure(id, template, biomes, spacing, separation,
                    salt, step, terrain, heightmap);
            STRUCTURES.add(structure);
            return structure;
        }
    }
}
