/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.variation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The variants of a rock a mod adds to the generation of the world, each where geology puts it, declared in code and
 * written by the generator as the datapack files the game reads (its configured feature, its placement and the biome
 * modifier that adds it to its biomes), so a datapack can change any of them.
 *
 * <pre>{@code
 * RockVariations.rock(id("martian_basalt"), () -> MARTIAN_BASALT)
 *         .variant(() -> COLUMNAR_MARTIAN_BASALT, RockRule.ANYWHERE.exposed(RockRule.Exposure.SIDE).slope(2))
 *         .variant(() -> VESICULAR_MARTIAN_BASALT, RockRule.ANYWHERE.exposed(RockRule.Exposure.TOP), LAVA_PLAINS)
 *         .biomes(MARS_BIOMES).declare();
 * }</pre>
 *
 * Each block of the rock takes the first variant whose place it lies in, and stays as it is where it lies in none.
 */
public final class RockVariations {

    private static final List<Spec> DECLARED = Collections.synchronizedList(new ArrayList<>());

    private RockVariations() {
    }

    /** Starts declaring the variants of {@code rock}, their feature, placement and biome modifier named {@code id}. */
    public static Builder rock(final ResourceLocation id, final Supplier<? extends Block> rock) {
        return new Builder(id, rock);
    }

    /** The variations the mod of that namespace declared. */
    public static List<Spec> declaredBy(final String namespace) {
        synchronized (DECLARED) {
            return DECLARED.stream().filter(one -> one.id().getNamespace().equals(namespace)).toList();
        }
    }

    /**
     * A rock's variations, as declared.
     *
     * @param id       what its feature, placement and biome modifier are named
     * @param rock     the rock it varies
     * @param variants its variants, the first that fits taken
     * @param biomes   the biomes it is varied in
     */
    public record Spec(ResourceLocation id, Supplier<? extends Block> rock, List<VariantSpec> variants,
                       TagKey<Biome> biomes) {

        public Spec {
            variants = List.copyOf(variants);
        }

        /** Its feature's configuration, the blocks and biomes looked up now. */
        public RockVariationConfig config(final HolderGetter<Biome> biomes) {
            return new RockVariationConfig(this.rock.get(), this.variants.stream()
                    .map(variant -> new RockVariationConfig.Variant(variant.block().get().defaultBlockState(),
                            variant.where(), variant.biomes().map(biomes::getOrThrow)))
                    .toList());
        }
    }

    /**
     * One variant, as declared.
     *
     * @param block  its block
     * @param where  where geology puts it
     * @param biomes the biomes it is put in, if not any
     */
    public record VariantSpec(Supplier<? extends Block> block, RockRule where, Optional<TagKey<Biome>> biomes) {
    }

    /** A rock's variations being declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final Supplier<? extends Block> rock;
        private final List<VariantSpec> variants = new ArrayList<>();
        private @Nullable TagKey<Biome> biomes;

        private Builder(final ResourceLocation id, final Supplier<? extends Block> rock) {
            this.id = Objects.requireNonNull(id, "id");
            this.rock = Objects.requireNonNull(rock, "rock");
        }

        /** Adds a variant, put wherever {@code where} holds. */
        public Builder variant(final Supplier<? extends Block> block, final RockRule where) {
            this.variants.add(new VariantSpec(block, where, Optional.empty()));
            return this;
        }

        /** Adds a variant, put wherever {@code where} holds in the biomes of {@code in}. */
        public Builder variant(final Supplier<? extends Block> block, final RockRule where, final TagKey<Biome> in) {
            this.variants.add(new VariantSpec(block, where, Optional.of(in)));
            return this;
        }

        /** The biomes the rock is varied in. */
        public Builder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        /**
         * Declares the variations.
         *
         * @throws IllegalStateException when they name no biomes or no variants
         */
        public Spec declare() {
            if (this.biomes == null || this.variants.isEmpty()) {
                throw new IllegalStateException("the variations " + this.id + " name their biomes and variants");
            }
            final Spec spec = new Spec(this.id, this.rock, this.variants, this.biomes);
            DECLARED.add(spec);
            return spec;
        }
    }
}
