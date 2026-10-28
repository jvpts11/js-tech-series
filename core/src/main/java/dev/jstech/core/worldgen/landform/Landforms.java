/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import dev.jstech.core.worldgen.strata.StrataConfig;
import dev.jstech.core.worldgen.variation.RockVariations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.jetbrains.annotations.Nullable;

/**
 * The shapes of the land a mod adds to the generation of the world, declared in code and written by the generator as
 * the datapack files the game reads (each one's configured feature, its placement and the biome modifier that adds it
 * to its biomes), so a datapack can change any of them: landforms of every kind, rock in layers, and seas that freeze
 * or boil away.
 *
 * <pre>{@code
 * Landforms.landform(id("craters"), LandformKind.CRATER).cell(192).chance(0.6).radius(8, 40).height(0.3)
 *         .surface(() -> Blocks.GRAVEL).scatter(() -> Blocks.COBBLESTONE).biomes(MY_MOON_BIOMES).declare();
 * Landforms.strata(id("moon_rock")).sequence(Landforms.layer(() -> Blocks.TUFF, 10),
 *         Landforms.layer(() -> Blocks.BASALT, 6)).biomes(MY_MOON_BIOMES).declare();
 * Landforms.sea(id("frozen_seas")).liquid(() -> Blocks.WATER).frozen(() -> Blocks.PACKED_ICE)
 *         .biomes(MY_MOON_BIOMES).declare();
 * }</pre>
 */
public final class Landforms {

    private static final List<LandformSpec> LANDFORMS = Collections.synchronizedList(new ArrayList<>());
    private static final List<StrataSpec> STRATA = Collections.synchronizedList(new ArrayList<>());
    private static final List<SeaSpec> SEAS = Collections.synchronizedList(new ArrayList<>());

    private Landforms() {
    }

    /** Starts declaring landforms of {@code kind}, their feature, placement and biome modifier all named {@code id}. */
    public static LandformBuilder landform(final ResourceLocation id, final LandformKind kind) {
        return new LandformBuilder(id, kind);
    }

    /** Starts declaring rock in layers, its feature, placement and biome modifier all named {@code id}. */
    public static StrataBuilder strata(final ResourceLocation id) {
        return new StrataBuilder(id);
    }

    /** Starts declaring the seas of a land, their feature, placement and biome modifier all named {@code id}. */
    public static SeaBuilder sea(final ResourceLocation id) {
        return new SeaBuilder(id);
    }

    /** A layer of rock, {@code thickness} blocks of {@code block}. */
    public static Layer layer(final Supplier<? extends Block> block, final int thickness) {
        return new Layer(block, thickness);
    }

    /** The landforms the mod of that namespace declared. */
    public static List<LandformSpec> landformsOf(final String namespace) {
        synchronized (LANDFORMS) {
            return LANDFORMS.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The rock in layers the mod of that namespace declared. */
    public static List<StrataSpec> strataOf(final String namespace) {
        synchronized (STRATA) {
            return STRATA.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The seas the mod of that namespace declared. */
    public static List<SeaSpec> seasOf(final String namespace) {
        synchronized (SEAS) {
            return SEAS.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** Whether the mod of that namespace declared any shape of the land. */
    public static boolean declares(final String namespace) {
        return !landformsOf(namespace).isEmpty() || !strataOf(namespace).isEmpty() || !seasOf(namespace).isEmpty()
                || !RockVariations.declaredBy(namespace).isEmpty();
    }

    /**
     * A layer of rock.
     *
     * @param block     its rock
     * @param thickness how many blocks thick
     */
    public record Layer(Supplier<? extends Block> block, int thickness) {
    }

    /**
     * Landforms of a kind, as declared: what {@link LandformConfig} says of them, the blocks still to be looked up.
     *
     * @param id       what their feature, placement and biome modifier are named
     * @param kind     their kind
     * @param cell     a cell's side, in blocks, at most one in each
     * @param chance   how many cells hold one
     * @param smallest the least radius
     * @param largest  the most radius
     * @param salt     what keeps them apart from another kind's
     * @param height   how tall or deep each is, a share of its radius
     * @param surface  their top
     * @param fill     what they are built of
     * @param special  their own block
     * @param scatter  what they scatter or line
     * @param bands    a mesa's bands
     * @param freezes  below how many kelvin their liquid freezes
     * @param boils    above how many kelvin their liquid boils away
     * @param biomes   the biomes they are drawn in
     * @param middle   the biomes one's middle lies in, or none for any biome they are drawn in
     * @param step     when in the generation they are laid
     */
    public record LandformSpec(ResourceLocation id, LandformKind kind, int cell, double chance, int smallest,
                               int largest, long salt, double height, Supplier<? extends Block> surface,
                               Supplier<? extends Block> fill, Supplier<? extends Block> special,
                               Supplier<? extends Block> scatter, List<Supplier<? extends Block>> bands,
                               double freezes, double boils, TagKey<Biome> biomes, @Nullable TagKey<Biome> middle,
                               GenerationStep.Decoration step) {

        public LandformSpec {
            bands = List.copyOf(bands);
        }

        /** Their feature's configuration, the blocks looked up now and the biomes of their middle from {@code all}. */
        public LandformConfig config(final HolderGetter<Biome> all) {
            final Optional<HolderSet<Biome>> where = this.middle == null ? Optional.empty()
                    : Optional.of(all.getOrThrow(this.middle));
            return new LandformConfig(this.kind, this.cell, this.chance, this.smallest, this.largest, this.salt,
                    this.height, this.surface.get().defaultBlockState(), this.fill.get().defaultBlockState(),
                    this.special.get().defaultBlockState(), this.scatter.get().defaultBlockState(),
                    this.bands.stream().map(band -> band.get().defaultBlockState()).toList(), this.freezes,
                    this.boils, where);
        }
    }

    /**
     * Rock in layers, as declared.
     *
     * @param id        what its feature, placement and biome modifier are named
     * @param sequences the sequences of layers, one of which each region holds
     * @param region    about how wide a region is, in blocks
     * @param wobble    how far the layers' boundaries rise and fall, in blocks
     * @param salt      what keeps its regions apart from another's
     * @param replaces  the rock the layers take the place of
     * @param biomes    the biomes it is laid in
     */
    public record StrataSpec(ResourceLocation id, List<List<Layer>> sequences, int region, double wobble, long salt,
                             TagKey<Block> replaces, TagKey<Biome> biomes) {

        public StrataSpec {
            sequences = sequences.stream().map(List::copyOf).toList();
        }

        /** Its feature's configuration, the blocks looked up now. */
        public StrataConfig config() {
            return new StrataConfig(this.sequences.stream().map(sequence -> sequence.stream()
                    .map(layer -> new StrataConfig.Layer(layer.block().get().defaultBlockState(), layer.thickness()))
                    .toList()).toList(), this.region, this.wobble, this.salt, this.replaces);
        }
    }

    /**
     * Seas, as declared.
     *
     * @param id      what their feature, placement and biome modifier are named
     * @param liquid  their liquid
     * @param frozen  what their top freezes to
     * @param freezes below how many kelvin they freeze
     * @param boils   above how many kelvin they boil away
     * @param biomes  the biomes they lie in
     */
    public record SeaSpec(ResourceLocation id, Supplier<? extends Block> liquid, Supplier<? extends Block> frozen,
                          double freezes, double boils, TagKey<Biome> biomes) {

        /** Their feature's configuration, the blocks looked up now. */
        public SeaConfig config() {
            return new SeaConfig(this.liquid.get().defaultBlockState(), this.frozen.get().defaultBlockState(),
                    this.freezes, this.boils);
        }
    }

    /**
     * Landforms being declared; they start as small craters of stone, one in each 256 blocks square, laid before the
     * lakes, with water and ice for their liquid.
     */
    public static final class LandformBuilder {

        private final ResourceLocation id;
        private final LandformKind kind;
        private final List<Supplier<? extends Block>> bands = new ArrayList<>();
        private int cell = 256;
        private double chance = 1.0;
        private int smallest = 8;
        private int largest = 24;
        private long salt;
        private double height = 0.25;
        private Supplier<? extends Block> surface = () -> Blocks.STONE;
        private Supplier<? extends Block> fill = () -> Blocks.STONE;
        private Supplier<? extends Block> special = () -> Blocks.WATER;
        private Supplier<? extends Block> scatter = () -> Blocks.ICE;
        private double freezes = 273.15;
        private double boils = 373.15;
        private @Nullable TagKey<Biome> biomes;
        private @Nullable TagKey<Biome> middle;
        private GenerationStep.Decoration step = GenerationStep.Decoration.RAW_GENERATION;

        private LandformBuilder(final ResourceLocation id, final LandformKind kind) {
            this.id = Objects.requireNonNull(id, "id");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.salt = id.hashCode();
        }

        /** At most one in each square {@code blocks} across, no two closer than about half of it. */
        public LandformBuilder cell(final int blocks) {
            this.cell = blocks;
            return this;
        }

        /** How many of the squares hold one, 0 to 1. */
        public LandformBuilder chance(final double share) {
            this.chance = share;
            return this;
        }

        /** Each from {@code least} to {@code most} blocks in radius: a canyon's or a river's half length. */
        public LandformBuilder radius(final int least, final int most) {
            this.smallest = least;
            this.largest = most;
            return this;
        }

        /** How tall or deep each is, as a share of its radius. */
        public LandformBuilder height(final double share) {
            this.height = share;
            return this;
        }

        public LandformBuilder salt(final long value) {
            this.salt = value;
            return this;
        }

        /** Its top: a crater's floor, a volcano's slopes, a mesa's top, a lake's bed. */
        public LandformBuilder surface(final Supplier<? extends Block> block) {
            this.surface = block;
            return this;
        }

        /** What it is built of where it raises the ground. */
        public LandformBuilder fill(final Supplier<? extends Block> block) {
            this.fill = block;
            return this;
        }

        /** Its own block: a volcano's lava, the ice of a sheet, the salt of a flat, a lake's liquid. */
        public LandformBuilder special(final Supplier<? extends Block> block) {
            this.special = block;
            return this;
        }

        /** What it scatters or lines: ejecta, cracks, a vent's magma, what its liquid freezes to. */
        public LandformBuilder scatter(final Supplier<? extends Block> block) {
            this.scatter = block;
            return this;
        }

        /** The bands of a mesa's body, from its foot up, again and again. */
        @SafeVarargs
        public final LandformBuilder bands(final Supplier<? extends Block>... blocks) {
            this.bands.addAll(List.of(blocks));
            return this;
        }

        /** Its liquid freezes below {@code freezesKelvin} and boils away above {@code boilsKelvin}. */
        public LandformBuilder liquidRange(final double freezesKelvin, final double boilsKelvin) {
            this.freezes = freezesKelvin;
            this.boils = boilsKelvin;
            return this;
        }

        /** The biomes they are drawn in. */
        public LandformBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /**
         * Only those whose middle lies in {@code where} are laid, drawn into every biome of {@link #biomes} they reach,
         * so a landform at a biome's edge is drawn whole across it.
         */
        public LandformBuilder middle(final TagKey<Biome> where) {
            this.middle = where;
            return this;
        }

        /** Laid in {@code when} of the generation, rather than first. */
        public LandformBuilder step(final GenerationStep.Decoration when) {
            this.step = when;
            return this;
        }

        /**
         * Declares the landforms.
         *
         * @throws IllegalStateException when they name no biomes, or their numbers make no landform
         */
        public LandformSpec declare() {
            if (biomes == null) {
                throw new IllegalStateException("the landforms " + id + " name the biomes they are drawn in");
            }
            if (cell < 16 || chance < 0 || chance > 1 || smallest < 1 || largest < smallest || height < 0) {
                throw new IllegalStateException("the landforms " + id + " have a cell, a chance, sizes and a height");
            }
            final LandformSpec spec = new LandformSpec(id, kind, cell, chance, smallest, largest, salt, height,
                    surface, fill, special, scatter, bands, freezes, boils, biomes, middle, step);
            LANDFORMS.add(spec);
            return spec;
        }
    }

    /** Rock in layers being declared; it takes the place of the overworld's stone, in regions 384 blocks wide. */
    public static final class StrataBuilder {

        private final ResourceLocation id;
        private final List<List<Layer>> sequences = new ArrayList<>();
        private int region = 384;
        private double wobble = 4;
        private long salt;
        private TagKey<Block> replaces = BlockTags.BASE_STONE_OVERWORLD;
        private @Nullable TagKey<Biome> biomes;

        private StrataBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.salt = id.hashCode();
        }

        /** Adds a sequence of layers, from the top down, which a region may hold. */
        public StrataBuilder sequence(final Layer... layers) {
            this.sequences.add(List.of(layers));
            return this;
        }

        /** Regions about {@code blocks} wide, each holding one of the sequences. */
        public StrataBuilder region(final int blocks) {
            this.region = blocks;
            return this;
        }

        /** The layers' boundaries rise and fall {@code blocks} across the land. */
        public StrataBuilder wobble(final double blocks) {
            this.wobble = blocks;
            return this;
        }

        public StrataBuilder salt(final long value) {
            this.salt = value;
            return this;
        }

        /** The rock the layers take the place of. */
        public StrataBuilder replaces(final TagKey<Block> rock) {
            this.replaces = rock;
            return this;
        }

        public StrataBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /**
         * Declares the rock in layers.
         *
         * @throws IllegalStateException when it names no biomes or no layers
         */
        public StrataSpec declare() {
            if (biomes == null || sequences.isEmpty() || sequences.stream().anyMatch(List::isEmpty)) {
                throw new IllegalStateException("the strata " + id + " name their biomes and their layers");
            }
            final StrataSpec spec = new StrataSpec(id, sequences, region, wobble, salt, replaces, biomes);
            STRATA.add(spec);
            return spec;
        }
    }

    /** Seas being declared; they start as water, freezing to ice as sea water does and boiling as water does. */
    public static final class SeaBuilder {

        private final ResourceLocation id;
        private Supplier<? extends Block> liquid = () -> Blocks.WATER;
        private Supplier<? extends Block> frozen = () -> Blocks.ICE;
        private double freezes = 271.35;
        private double boils = 373.15;
        private @Nullable TagKey<Biome> biomes;

        private SeaBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** Their liquid, the one the terrain filled its low ground with. */
        public SeaBuilder liquid(final Supplier<? extends Block> block) {
            this.liquid = block;
            return this;
        }

        public SeaBuilder frozen(final Supplier<? extends Block> block) {
            this.frozen = block;
            return this;
        }

        /** They freeze below {@code freezesKelvin} and boil away above {@code boilsKelvin}. */
        public SeaBuilder range(final double freezesKelvin, final double boilsKelvin) {
            this.freezes = freezesKelvin;
            this.boils = boilsKelvin;
            return this;
        }

        public SeaBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /**
         * Declares the seas.
         *
         * @throws IllegalStateException when they name no biomes, or they would freeze above where they boil
         */
        public SeaSpec declare() {
            if (biomes == null || freezes >= boils) {
                throw new IllegalStateException("the seas " + id + " name their biomes, freezing below boiling");
            }
            final SeaSpec spec = new SeaSpec(id, liquid, frozen, freezes, boils, biomes);
            SEAS.add(spec);
            return spec;
        }
    }
}
