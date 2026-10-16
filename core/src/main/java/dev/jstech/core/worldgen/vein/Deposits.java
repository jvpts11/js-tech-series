/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of vein, by id: each declared in code by its mod with what it is while nothing else says, and each a file
 * under {@code data/<namespace>/jstech/deposits/} a data pack writes over or adds to. The data generation writes a
 * mod's declared kinds as its files ({@code DepositProvider}). A vein of a kind generates only the ores some installed
 * mod uses, so a kind may be declared by any mod for materials it does not use itself.
 *
 * <pre>{@code
 * public static final ResourceLocation MAGNETITE = Deposits.deposit(rl("magnetite"))
 *         .mineral(SeriesMaterials.IRON, 6).mineral(MyMaterials.TITANIUM, 1)
 *         .heights(-48, 96).frequency(0.12).size(20, 6).density(0.55)
 *         .biomes(BiomeTags.IS_OVERWORLD).rocks(HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE)
 *         .declare();
 * }</pre>
 */
public final class Deposits {

    /** The kinds the data packs say, by id. */
    public static final DataRegistry<DepositType> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "deposits"), "jstech/deposits", DepositType.CODEC)
            .register();

    private static final Map<ResourceLocation, DepositType> DECLARED = new ConcurrentHashMap<>();
    private static volatile Known known = new Known(-1, -1, -1, List.of());

    private Deposits() {
    }

    /** Declares the registry of the files, from the Core's constructor. */
    public static void register() {
        // Loading the class declares it.
    }

    /** Starts declaring the kind of vein {@code id}. */
    public static DepositBuilder deposit(final ResourceLocation id) {
        return new DepositBuilder(id);
    }

    /** Every kind declared in code, by id. */
    public static Map<ResourceLocation, DepositType> declared() {
        return Map.copyOf(DECLARED);
    }

    /**
     * Every kind of vein the server lays, by id in order: those the data packs' files say, those declared that no
     * file writes over, and the veins other mods' moved ores lie in ({@link MovedOres}).
     */
    public static List<Entry> all() {
        final Known now = known;
        final long files = FILES.version();
        final long moved = MovedOres.version();
        if (now.files() == files && now.declared() == DECLARED.size() && now.moved() == moved) {
            return now.entries();
        }
        final Map<ResourceLocation, Entry> byId = new TreeMap<>();
        DECLARED.forEach((id, type) -> byId.put(id, new Entry(id, type, List.of())));
        FILES.entries().forEach((id, type) -> byId.put(id, new Entry(id, type, List.of())));
        MovedOres.deposits().forEach(entry -> byId.putIfAbsent(entry.id(), entry));
        final List<Entry> entries = List.copyOf(byId.values());
        known = new Known(files, DECLARED.size(), moved, entries);
        return entries;
    }

    /** The kind of vein {@code id}, or none. */
    public static Optional<Entry> get(final ResourceLocation id) {
        return all().stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    /**
     * A kind of vein the server lays.
     *
     * @param id      its id
     * @param type    what it is
     * @param targets for the vein of a moved ore, what its blocks replace and turn into, as the ore's own feature
     *                says; empty for a vein of materials
     */
    public record Entry(ResourceLocation id, DepositType type, List<OreConfiguration.TargetBlockState> targets) {

        public Entry {
            targets = List.copyOf(targets);
        }
    }

    /* The kinds as last worked out, and what they were worked out from. */
    private record Known(long files, int declared, long moved, List<Entry> entries) {
    }

    /** A kind of vein being declared; it starts as an overworld vein of every rock, as common as copper. */
    public static final class DepositBuilder {

        private final ResourceLocation id;
        private final List<DepositType.Mineral> minerals = new ArrayList<>();
        private final List<ResourceLocation> rocks = new ArrayList<>();
        private int minY = -48;
        private int maxY = 64;
        private double frequency = 0.1;
        private double radius = 20;
        private double thickness = 6;
        private double density = 0.5;
        private @Nullable TagKey<Biome> biomes;
        private double elsewhere;

        private DepositBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** Brings {@code material} into it, as much of it as {@code weight} against the others'. */
        public DepositBuilder mineral(final Material material, final double weight) {
            this.minerals.add(new DepositType.Mineral(material.id(), weight));
            return this;
        }

        /** Its middle lies from {@code lowest} to {@code highest}. */
        public DepositBuilder heights(final int lowest, final int highest) {
            this.minY = lowest;
            this.maxY = highest;
            return this;
        }

        /** How many of the world's zones hold one, 0 to 1. */
        public DepositBuilder frequency(final double share) {
            this.frequency = share;
            return this;
        }

        /** It reaches {@code across} blocks from its middle across and {@code upAndDown} up and down. */
        public DepositBuilder size(final double across, final double upAndDown) {
            this.radius = across;
            this.thickness = upAndDown;
            return this;
        }

        /** How much of it is ore at its middle, 0 to 1. */
        public DepositBuilder density(final double share) {
            this.density = share;
            return this;
        }

        /** It is found where the biome at its middle is one of {@code where}. */
        public DepositBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /** It is found {@code share} as often outside its biomes as in them. */
        public DepositBuilder elsewhere(final double share) {
            this.elsewhere = share;
            return this;
        }

        /** It crosses only {@code crossed}; otherwise every rock ores sit in. */
        public DepositBuilder rocks(final HostRock... crossed) {
            for (final HostRock rock : crossed) {
                this.rocks.add(rock.id());
            }
            return this;
        }

        /**
         * Declares the kind, and gives its id.
         *
         * @throws IllegalStateException when it brings no material together, or its numbers make no vein
         */
        public ResourceLocation declare() {
            if (minerals.isEmpty()) {
                throw new IllegalStateException("the vein " + id + " brings some material together");
            }
            if (frequency < 0 || frequency > 1 || density < 0 || density > 1 || radius < 1 || thickness < 1) {
                throw new IllegalStateException("the vein " + id + " has a frequency, a size and a density");
            }
            DECLARED.put(id, new DepositType(minerals, minY, maxY, frequency, radius, thickness, density,
                    Optional.ofNullable(biomes), elsewhere, rocks));
            return id;
        }
    }
}
