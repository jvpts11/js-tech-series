/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jetbrains.annotations.Nullable;

/**
 * The structures a mod adds, declared in code and written by the generator as the datapack files the game reads (the
 * template pools, the processor lists that wear them, the structure and its set), so a data pack can change any of
 * them. A structure is of one saved template or a layout of many joined by jigsaw blocks; it stands where the land
 * suits it, as often as it says, far enough from the world's origin and clear of every other structure; it can be sunk
 * into the ground, stand on foundations, be worn by rule, and grow over time.
 *
 * <pre>{@code
 * Structures.pool(rl("outpost/houses")).element(rl("outpost/house_a"), 3).element(rl("outpost/house_b"), 1)
 *         .declare();
 * Structures.structure(rl("outpost")).element(rl("outpost/plaza"), 1).size(6).reach(96)
 *         .biomes(BiomeTags.IS_OVERWORLD).spacing(48, 20).frequency(0.6).minDistance(1500).clearOf(4)
 *         .fit(new StructureFit(12, 6, 60, 180, true, 0)).foundation(() -> Blocks.COBBLESTONE)
 *         .worn(() -> Blocks.IRON_BLOCK, () -> MyBlocks.RUSTED_IRON.get(), 0.4)
 *         .grows(new Growth(4, 1, 3)).declare();
 * }</pre>
 */
public final class Structures {

    private static final List<StructureSpec> STRUCTURES = Collections.synchronizedList(new ArrayList<>());
    private static final List<PoolSpec> POOLS = Collections.synchronizedList(new ArrayList<>());

    private Structures() {
    }

    /** Starts declaring a structure, its start pool, processors, structure and set all named {@code id}. */
    public static StructureBuilder structure(final ResourceLocation id) {
        return new StructureBuilder(id);
    }

    /** Starts declaring a template pool named {@code id}, which a structure's jigsaw blocks join pieces from. */
    public static PoolBuilder pool(final ResourceLocation id) {
        return new PoolBuilder(id);
    }

    /** The structures the mod of that namespace declared. */
    public static List<StructureSpec> structuresOf(final String namespace) {
        synchronized (STRUCTURES) {
            return STRUCTURES.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /** The pools the mod of that namespace declared beside its structures' own. */
    public static List<PoolSpec> poolsOf(final String namespace) {
        synchronized (POOLS) {
            return POOLS.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /**
     * A saved template in a pool.
     *
     * @param template the template, a file under {@code data/<namespace>/structure/}
     * @param weight   how often it is picked against the pool's others
     */
    public record Element(ResourceLocation template, int weight) {
    }

    /**
     * A rule of wear, as declared: blocks of {@code from} wear into {@code worn} as often as {@code chance}.
     *
     * @param from   the block that wears
     * @param worn   what it wears into
     * @param chance how often, 0 to 1
     */
    public record Worn(Supplier<? extends Block> from, Supplier<? extends Block> worn, double chance) {
    }

    /**
     * A template pool, as declared.
     *
     * @param id       what it is named
     * @param elements its templates
     * @param worn     how its templates wear; empty for none
     */
    public record PoolSpec(ResourceLocation id, List<Element> elements, List<Worn> worn) {

        public PoolSpec {
            elements = List.copyOf(elements);
            worn = List.copyOf(worn);
        }
    }

    /**
     * A structure, as declared.
     *
     * @param id          what its start pool, processors, structure and set are named
     * @param elements    its start pool's templates
     * @param biomes      where it is placed
     * @param step        when in the generation it is placed
     * @param terrain     how the ground under and round it is shaped to it
     * @param spacing     the size, in chunks, of the cells the world is cut into, one tried in each
     * @param separation  the fewest chunks between two of them
     * @param salt        what keeps its spread apart from other structures' on the same seed
     * @param frequency   how many of the cells hold one, 0 to 1
     * @param minDistance how near the world's origin none lies, in blocks
     * @param clearOf     how many chunks from every other structure none lies; 0 to let them be near
     * @param fixedAt     the chunks it is placed at and nowhere else; empty for spread
     * @param size        how many pieces deep its jigsaw lays out
     * @param reach       how far from its start its pieces may reach, in blocks
     * @param fit         where the land suits it, and how it sits
     * @param foundation  what its foundations are made of, or null for none
     * @param growth      how it grows, for a settlement
     * @param worn        how its templates wear, the age they show; empty for none
     * @param age         how worn it is, 0 for new, 1 as the rules say
     * @param streets     whether its low pieces hold the taller ones within them, as a village's streets do
     */
    public record StructureSpec(ResourceLocation id, List<Element> elements, TagKey<Biome> biomes,
                                GenerationStep.Decoration step, TerrainAdjustment terrain, int spacing,
                                int separation, int salt, float frequency, int minDistance, int clearOf,
                                List<ChunkPos> fixedAt, int size, int reach, StructureFit fit,
                                @Nullable Supplier<? extends Block> foundation, Optional<Growth> growth,
                                List<Worn> worn, double age, boolean streets) {

        public StructureSpec {
            elements = List.copyOf(elements);
            fixedAt = List.copyOf(fixedAt);
            worn = List.copyOf(worn);
        }

        /** Its first template, the one the old declarations of one template named. */
        public ResourceLocation template() {
            return this.elements.getFirst().template();
        }
    }

    /** A template pool being declared. */
    public static final class PoolBuilder {

        private final ResourceLocation id;
        private final List<Element> elements = new ArrayList<>();
        private final List<Worn> worn = new ArrayList<>();

        private PoolBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** Adds the template {@code template}, as often as {@code weight} against the others. */
        public PoolBuilder element(final ResourceLocation template, final int weight) {
            this.elements.add(new Element(template, weight));
            return this;
        }

        /** Its blocks of {@code from} wear into {@code into} as often as {@code chance}. */
        public PoolBuilder worn(final Supplier<? extends Block> from, final Supplier<? extends Block> into,
                                final double chance) {
            this.worn.add(new Worn(from, into, chance));
            return this;
        }

        /**
         * Declares the pool.
         *
         * @throws IllegalStateException when it names no template
         */
        public PoolSpec declare() {
            if (elements.isEmpty()) {
                throw new IllegalStateException("the pool " + id + " names its templates");
            }
            final PoolSpec spec = new PoolSpec(id, elements, worn);
            POOLS.add(spec);
            return spec;
        }
    }

    /** A structure being declared; it starts as one template on any land, one tried in each 32 chunks. */
    public static final class StructureBuilder {

        private final ResourceLocation id;
        private final List<Element> elements = new ArrayList<>();
        private final List<ChunkPos> fixedAt = new ArrayList<>();
        private final List<Worn> worn = new ArrayList<>();
        private @Nullable TagKey<Biome> biomes;
        private int spacing = 32;
        private int separation = 8;
        private int salt;
        private float frequency = 1F;
        private int minDistance;
        private int clearOf;
        private int size = 1;
        private int reach = 80;
        private StructureFit fit = StructureFit.ANYWHERE;
        private @Nullable Supplier<? extends Block> foundation;
        private Optional<Growth> growth = Optional.empty();
        private double age = 1;
        private boolean streets;
        private GenerationStep.Decoration step = GenerationStep.Decoration.SURFACE_STRUCTURES;
        private TerrainAdjustment terrain = TerrainAdjustment.BEARD_THIN;

        private StructureBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.salt = id.hashCode() & Integer.MAX_VALUE;
        }

        /** It is of the one template {@code saved}. */
        public StructureBuilder template(final ResourceLocation saved) {
            return element(saved, 1);
        }

        /** Its start is picked from its templates, this one as often as {@code weight}. */
        public StructureBuilder element(final ResourceLocation template, final int weight) {
            this.elements.add(new Element(template, weight));
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

        /** How many of the cells hold one, 0 to 1: its density. */
        public StructureBuilder frequency(final float share) {
            this.frequency = share;
            return this;
        }

        /** None lies nearer the world's origin than {@code blocks}. */
        public StructureBuilder minDistance(final int blocks) {
            this.minDistance = blocks;
            return this;
        }

        /** None lies within {@code chunks} of any other structure, of any mod. */
        public StructureBuilder clearOf(final int chunks) {
            this.clearOf = chunks;
            return this;
        }

        /** It is placed at {@code chunk} only, one of a kind, as a landmark is; added to, for several such places. */
        public StructureBuilder at(final ChunkPos chunk) {
            this.fixedAt.add(chunk);
            return this;
        }

        /** Its jigsaw lays out up to {@code depth} pieces deep from its start. */
        public StructureBuilder size(final int depth) {
            this.size = depth;
            return this;
        }

        /** Its pieces reach up to {@code blocks} from its start. */
        public StructureBuilder reach(final int blocks) {
            this.reach = blocks;
            return this;
        }

        /** Where the land suits it, and how it sits. */
        public StructureBuilder fit(final StructureFit where) {
            this.fit = where;
            return this;
        }

        /** It stands on foundations of {@code block}, down to the ground. */
        public StructureBuilder foundation(final Supplier<? extends Block> block) {
            this.foundation = block;
            return this;
        }

        /** Its templates' blocks of {@code from} wear into {@code into} as often as {@code chance}. */
        public StructureBuilder worn(final Supplier<? extends Block> from, final Supplier<? extends Block> into,
                                     final double chance) {
            this.worn.add(new Worn(from, into, chance));
            return this;
        }

        /** It shows {@code share} of the wear its rules say, 0 for new. */
        public StructureBuilder age(final double share) {
            this.age = share;
            return this;
        }

        /**
         * Its low pieces, streets, make room above them for the taller pieces their jigsaws face inward to, its
         * houses, as the game's villages do; a layout of the game's village pieces needs it, or no house fits.
         */
        public StructureBuilder streets() {
            this.streets = true;
            return this;
        }

        /** It is a settlement that grows over time as {@code how} says. */
        public StructureBuilder grows(final Growth how) {
            this.growth = Optional.of(how);
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

        /**
         * Declares the structure.
         *
         * @throws IllegalStateException when it names no template or no biomes, or its separation is not less than
         *                               its spacing
         */
        public StructureSpec declare() {
            if (elements.isEmpty() || biomes == null) {
                throw new IllegalStateException("the structure " + id + " names its template and its biomes");
            }
            if (fixedAt.isEmpty() && (separation >= spacing || separation < 0)) {
                throw new IllegalStateException("the structure " + id + " keeps its separation under its spacing");
            }
            final StructureSpec spec = new StructureSpec(id, elements, biomes, step, terrain, spacing, separation, salt,
                    frequency, minDistance, clearOf, fixedAt, size, reach, fit, foundation, growth, worn, age, streets);
            STRUCTURES.add(spec);
            return spec;
        }
    }
}
