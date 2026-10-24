/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.world;

import dev.jstech.core.jstechseries.SeriesDeposits;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.industrial.JsIndustrial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * J's Industrial's deposit types (industrial/docs/design/world.md), on J's Core's veins: each a real kind of ore
 * deposit, bringing together the minerals that form together in nature, and with them the ore of each of their
 * elements, the game's iron ore beside iron's minerals, at a quarter of the weight of the minerals mined for it. They
 * stand in for J's Core's plain veins of iron, copper, gold and tin; the Nether's gold stays the Core's. Each crosses
 * the rocks of its geological setting and stone and deepslate, where the game's own ores lie. Only the minerals that
 * exist so far are in them: the rest of each type's come with their minerals.
 */
public final class IndustrialDeposits {

    /** How much of a deposit the ore of an element is, against the minerals mined for it. */
    public static final double ELEMENT_ORE_SHARE = 0.25;

    /* Middle to deep in granite and andesite, huge and low-grade, as the porphyries of Chile are. */
    public static final ResourceLocation PORPHYRY_COPPER = deposit("porphyry_copper")
            .mineral(SeriesMinerals.CHALCOPYRITE, 4).mineral(SeriesMinerals.PYRITE, 2)
            .mineral(SeriesMinerals.MAGNETITE, 1).rocks(HostRocks.GRANITE, HostRocks.ANDESITE).heights(-32, 48)
            .frequency(0.06).size(24, 8).density(0.45).declare();
    /* Huge and poor, in stone and deepslate, as the banded iron of the Pilbara and the Mesabi Range. */
    public static final ResourceLocation BANDED_IRON = deposit("banded_iron")
            .mineral(SeriesMinerals.HEMATITE, 4).mineral(SeriesMinerals.MAGNETITE, 2).heights(-48, 64)
            .frequency(0.10).size(32, 5).density(0.35).declare();
    /* Shallow to middle, in beds of sediment. */
    public static final ResourceLocation SEDIMENTARY_IRON = deposit("sedimentary_iron")
            .mineral(SeriesMinerals.SIDERITE, 1).rocks(HostRocks.SANDSTONE, HostRocks.CALCITE).heights(0, 80)
            .frequency(0.06).size(20, 5).density(0.45).declare();
    /* Near the surface, above sulfide deposits, more in dry land, where water leaches the sulfides away. */
    public static final ResourceLocation OXIDISED_ZONE = deposit("oxidised_zone")
            .mineral(SeriesMinerals.MALACHITE, 3).mineral(SeriesMinerals.CUPRITE, 2)
            .rocks(HostRocks.SANDSTONE, HostRocks.CALCITE).heights(40, 96).biomes(Tags.Biomes.IS_DRY)
            .elsewhere(0.5).frequency(0.06).size(18, 4).density(0.5).declare();
    /* Shallow to middle in andesite and tuff, the low-sulfidation epithermal veins of gold and silver. */
    public static final ResourceLocation EPITHERMAL_GOLD = deposit("epithermal_gold")
            .mineral(SeriesMinerals.ELECTRUM, 1).rocks(HostRocks.ANDESITE, HostRocks.TUFF).heights(16, 96)
            .frequency(0.04).size(14, 5).density(0.4).declare();
    /* Middle depths in granite, as the tin of Cornwall. */
    public static final ResourceLocation TIN_GREISEN = deposit("tin_greisen")
            .mineral(SeriesMinerals.CASSITERITE, 1).rocks(HostRocks.GRANITE).heights(0, 64).frequency(0.06)
            .size(16, 6).density(0.5).declare();
    /* Middle to deep, massive sulfides laid by sea-floor vents, as Rio Tinto's. */
    public static final ResourceLocation MASSIVE_SULFIDE = deposit("massive_sulfide")
            .mineral(SeriesMinerals.PYRITE, 4).mineral(SeriesMinerals.CHALCOPYRITE, 2).heights(-48, 32)
            .frequency(0.05).size(18, 6).density(0.6).declare();
    /* Where granite meets calcite, at middle depths. */
    public static final ResourceLocation SKARN = deposit("skarn")
            .mineral(SeriesMinerals.MAGNETITE, 3).mineral(SeriesMinerals.CHALCOPYRITE, 1)
            .rocks(HostRocks.GRANITE, HostRocks.CALCITE).heights(-16, 48).frequency(0.05).size(16, 6).density(0.5)
            .declare();

    private IndustrialDeposits() {
    }

    /** Declares the deposit types, standing in for the Core's plain veins, from the mod's constructor. */
    public static void declare() {
        for (final ResourceLocation plain : List.of(SeriesDeposits.IRON, SeriesDeposits.COPPER, SeriesDeposits.GOLD,
                SeriesDeposits.TIN)) {
            Deposits.supersede(plain);
        }
    }

    /** Every deposit type of the mod, in order. */
    public static List<ResourceLocation> all() {
        return List.of(PORPHYRY_COPPER, BANDED_IRON, SEDIMENTARY_IRON, OXIDISED_ZONE, EPITHERMAL_GOLD, TIN_GREISEN,
                MASSIVE_SULFIDE, SKARN);
    }

    /** Starts declaring a deposit type of the mod: an overworld one, of the rocks it is told, stone and deepslate. */
    public static Builder deposit(final String path) {
        return new Builder(ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path));
    }

    /** A deposit type of J's Industrial being declared, which adds the ores of its minerals' elements. */
    public static final class Builder {

        private final ResourceLocation id;
        private final List<Brought> minerals = new ArrayList<>();
        private final Set<HostRock> rocks = new LinkedHashSet<>(List.of(HostRocks.STONE, HostRocks.DEEPSLATE));
        private TagKey<Biome> biomes = BiomeTags.IS_OVERWORLD;
        private double elsewhere;
        private int lowest = -48;
        private int highest = 64;
        private double frequency = 0.05;
        private double across = 18;
        private double upAndDown = 6;
        private double density = 0.5;

        private Builder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** Brings in {@code mineral}, as much as {@code weight} against the others, all through it. */
        public Builder mineral(final Material mineral, final double weight) {
            return mineral(mineral, weight, DepositType.Zone.ALL);
        }

        /** Brings in {@code mineral}, as much as {@code weight}, only in {@code zone} of it. */
        public Builder mineral(final Material mineral, final double weight, final DepositType.Zone zone) {
            this.minerals.add(new Brought(mineral, weight, zone, null));
            return this;
        }

        /** Brings in {@code mineral}, as much as {@code weight}, only where the biome is one of {@code where}. */
        public Builder mineralIn(final Material mineral, final double weight, final TagKey<Biome> where) {
            this.minerals.add(new Brought(mineral, weight, DepositType.Zone.ALL, where));
            return this;
        }

        /** It crosses {@code crossed} too, beside stone and deepslate. */
        public Builder rocks(final HostRock... crossed) {
            this.rocks.addAll(List.of(crossed));
            return this;
        }

        /** Its middle lies from {@code from} to {@code to}. */
        public Builder heights(final int from, final int to) {
            this.lowest = from;
            this.highest = to;
            return this;
        }

        /** How many of the world's zones hold one. */
        public Builder frequency(final double share) {
            this.frequency = share;
            return this;
        }

        /** How far it reaches across and up and down from its middle. */
        public Builder size(final double reach, final double height) {
            this.across = reach;
            this.upAndDown = height;
            return this;
        }

        /** How much of its middle is ore. */
        public Builder density(final double share) {
            this.density = share;
            return this;
        }

        /** The biomes it is found in, by the biome at its middle. */
        public Builder biomes(final TagKey<Biome> where) {
            this.biomes = Objects.requireNonNull(where, "where");
            return this;
        }

        /** How many as often it is found outside its biomes. */
        public Builder elsewhere(final double share) {
            this.elsewhere = share;
            return this;
        }

        /** Declares it on J's Core's veins, with the ores of its minerals' elements, and gives its id. */
        public ResourceLocation declare() {
            final Deposits.DepositBuilder deposit = Deposits.deposit(this.id).heights(this.lowest, this.highest)
                    .frequency(this.frequency).size(this.across, this.upAndDown).density(this.density)
                    .biomes(this.biomes).elsewhere(this.elsewhere).rocks(this.rocks.toArray(HostRock[]::new));
            final Map<Material, Double> elements = new LinkedHashMap<>();
            for (final Brought brought : this.minerals) {
                deposit.mineral(brought.mineral());
                brought.material().mineral().ifPresent(mineral -> elements.merge(mineral.main().material(),
                        brought.weight() * ELEMENT_ORE_SHARE, Double::sum));
            }
            elements.forEach(deposit::mineral);
            return deposit.declare();
        }
    }

    /* A mineral a deposit brings, where in it, and in which biomes, if only some. */
    private record Brought(Material material, double weight, DepositType.Zone zone, @Nullable TagKey<Biome> biomes) {

        DepositType.Mineral mineral() {
            return new DepositType.Mineral(this.material.id(), this.weight, this.zone,
                    Optional.ofNullable(this.biomes));
        }
    }
}
