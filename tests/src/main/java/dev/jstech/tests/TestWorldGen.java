/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.worldgen.WorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/**
 * A vein of ore, a structure and a dimension declared the way a mod declares them, to prove the Core's world
 * generation. The vein and the structure are placed only in a biome tag that holds no biome, so they exist in the
 * registries and generate nowhere; the dimension stands apart, reached only by the tests.
 */
public final class TestWorldGen {

    /** A biome tag with no biome in it. */
    public static final TagKey<Biome> NOWHERE =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"));

    public static final WorldGen.OreVein ORE = WorldGen.ore(id("test_ore")).stone(() -> Blocks.AMETHYST_BLOCK)
            .vein(12).perChunk(4).heights(-16, 48).triangle().biomes(NOWHERE).declare();
    public static final WorldGen.TemplateStructure CAMP = WorldGen.structure(id("test_camp"))
            .template(ResourceLocation.withDefaultNamespace("village/plains/houses/plains_small_house_1"))
            .biomes(NOWHERE).spacing(40, 12).salt(7_340_211).declare();
    /**
     * A small grey moon: flat stone under a layer of dust, always at midnight, under rules of its own (a sixth of
     * the overworld's pull, no air, bitter cold, no weather). Only its copies exist, made by the tests that send
     * players there, so the test worlds keep the game's three dimensions and open without the experimental warning.
     */
    public static final WorldGen.DimensionSpec MOON = WorldGen.dimension(id("test_moon"))
            .layer(() -> Blocks.BEDROCK, 1).layer(() -> Blocks.STONE, 6).layer(() -> Blocks.LIGHT_GRAY_CONCRETE, 1)
            .biome(Biomes.DESERT).heights(0, 64).fixedTime(18_000).ambientLight(0.1F).unnatural().runtimeOnly()
            .declare();

    private TestWorldGen() {
    }

    /** Declares the vein and the structure, before the generator runs. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
