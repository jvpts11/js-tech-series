/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.worldgen.WorldGen;
import dev.jstech.core.worldgen.biome.WorldBiomes;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.landform.LandformKind;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.structure.Growth;
import dev.jstech.core.worldgen.structure.StructureFit;
import dev.jstech.core.worldgen.structure.Structures;
import dev.jstech.core.worldgen.terrain.Terrains;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Scattered ores, structures, dimensions, biomes, a terrain, shapes of the land, kinds of vein and of reservoir
 * declared the way a mod declares them, to prove the Core's world generation. The first vein and the camp are placed
 * only in a biome tag that holds no biome, so they exist in the registries and generate nowhere; the dimensions stand
 * apart, reached only by the tests, and the biomes, the shapes of the land, the veins, the reservoirs and the other
 * structures (a worn hut, a town that grows, a monument at one place) lie only in the test land.
 */
public final class TestWorldGen {

    /** A biome tag with no biome in it. */
    public static final TagKey<Biome> NOWHERE =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"));

    public static final WorldGen.OreVein ORE = WorldGen.ore(id("test_ore")).stone(() -> Blocks.AMETHYST_BLOCK)
            .vein(12).perChunk(4).heights(-16, 48).triangle().biomes(NOWHERE).declare();
    public static final Structures.StructureSpec CAMP = WorldGen.structure(id("test_camp"))
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
    /** A dry steppe of salt and sand, cool on the whole and swung hard by the seasons. */
    public static final WorldBiomes.BiomeSpec STEPPE = WorldBiomes.biome(id("test_salt_steppe"))
            .name("Test Salt Steppe").climate(9.5, 240).seasons(16).grass(0xB8B07A).declare();
    /** A plain of frost, far below freezing all the year. */
    public static final WorldBiomes.BiomeSpec FROST = WorldBiomes.biome(id("test_frost_flats"))
            .name("Test Frost Flats").climate(-25, 150).declare();
    /** The two biomes of the test land. */
    public static final TagKey<Biome> LAND = TagKey.create(Registries.BIOME, id("test_land"));
    /** The test land's ground: low hills of stone round a sea, sand on top, snow on the frost. */
    public static final Terrains.TerrainSpec TERRAIN = Terrains.terrain(id("test_land")).heights(0, 128).ground(64)
            .hills(10, 128).sea(60).bedrock(3).surface(() -> Blocks.SAND, () -> Blocks.SANDSTONE, 3)
            .surface(FROST.key(), () -> Blocks.SNOW_BLOCK, () -> Blocks.DIRT, 3).declare();
    /** A crater in every square 96 blocks across, floored with gravel, its ejecta of cobblestone. */
    public static final Landforms.LandformSpec CRATERS = Landforms.landform(id("test_craters"), LandformKind.CRATER)
            .cell(96).radius(10, 14).height(0.3).surface(() -> Blocks.GRAVEL).scatter(() -> Blocks.COBBLESTONE)
            .biomes(LAND).declare();
    /** A lake in every square 96 blocks across, its bed of clay, frozen on the frost. */
    public static final Landforms.LandformSpec LAKES = Landforms.landform(id("test_lakes"), LandformKind.LAKE)
            .cell(96).radius(6, 8).height(0.4).surface(() -> Blocks.CLAY).biomes(LAND).declare();
    /** Tuff over calcite under the test land's ground. */
    public static final Landforms.StrataSpec STRATA = Landforms.strata(id("test_strata"))
            .sequence(Landforms.layer(() -> Blocks.TUFF, 6), Landforms.layer(() -> Blocks.CALCITE, 4))
            .biomes(LAND).declare();
    /** The test land's sea, freezing to packed ice on the frost. */
    public static final Landforms.SeaSpec SEAS = Landforms.sea(id("test_seas")).frozen(() -> Blocks.PACKED_ICE)
            .biomes(LAND).declare();
    /**
     * The test land: its terrain under the steppe where the terrain's noise is warm and the frost where it is cold.
     * Only its copies exist, made by the tests that look at what it generates.
     */
    public static final WorldGen.DimensionSpec TEST_LAND = WorldGen.dimension(id("test_land")).terrain(TERRAIN)
            .biome(STEPPE.key(), 0.5F, 0F).biome(FROST.key(), -0.5F, 0F).runtimeOnly().declare();
    /** Veins of iron with some tin in the test land's stone, below its layers, in half its zones. */
    public static final ResourceLocation IRON_VEINS = Deposits.deposit(id("test_iron")).mineral(SeriesMaterials.IRON, 3)
            .mineral(SeriesMaterials.TIN, 1).heights(16, 40).frequency(0.5).size(14, 5).density(0.6).biomes(LAND)
            .declare();
    /** Veins of gold that cross only granite, where no mod uses gold's ore, so they lay nothing. */
    public static final ResourceLocation GRANITE_GOLD = Deposits.deposit(id("test_granite_gold"))
            .mineral(SeriesMaterials.GOLD, 1).heights(16, 40).frequency(0.01).biomes(NOWHERE)
            .rocks(HostRocks.GRANITE).declare();
    /** Reservoirs of brine in mud under a cap of smooth basalt, in the test land's stone, in most of its zones. */
    public static final ResourceLocation BRINES = Reservoirs.reservoir(id("test_brine"))
            .fluid(ResourceLocation.withDefaultNamespace("water"))
            .rocks(ResourceLocation.withDefaultNamespace("mud"), ResourceLocation.withDefaultNamespace("packed_mud"),
                    ResourceLocation.withDefaultNamespace("smooth_basalt"))
            .perBlock(1000).heights(20, 36).frequency(0.7).size(24, 4).capping(2).biomes(LAND).declare();
    /** Another mod's ore scattered as the game scatters its own, in the test land: one the veins can move. */
    public static final WorldGen.OreVein LAND_ORE = WorldGen.ore(id("test_land_ore")).stone(() -> Blocks.EMERALD_ORE)
            .vein(6).perChunk(2).heights(16, 48).biomes(LAND).declare();
    /** The chunk along the test land's east the monument stands in. */
    public static final int MONUMENT_CHUNK = 20;
    /** The test land's huts, as the instruments that look for them name them. */
    public static final TagKey<Structure> HUTS = TagKey.create(Registries.STRUCTURE, id("test_huts"));
    /**
     * A hut of the plains on the test land's dry and gentle ground, its oak planks half worn to birch, on foundations
     * of stone bricks: one tried in each square of eight chunks, none within 64 blocks of the origin, and none within a
     * chunk of the test town.
     */
    public static final Structures.StructureSpec HUT = Structures.structure(id("test_hut"))
            .template(ResourceLocation.withDefaultNamespace("village/plains/houses/plains_small_house_1"))
            .biomes(LAND).spacing(8, 3).salt(5_120_377).minDistance(64).clearOf(1)
            .fit(new StructureFit(4, 6, 40, 120, true, 0)).foundation(() -> Blocks.STONE_BRICKS)
            .worn(() -> Blocks.OAK_PLANKS, () -> Blocks.BIRCH_PLANKS, 0.5).declare();
    /** A small town of the plains, streets and houses, that grows in three stages, a day each, from its middle out. */
    public static final Structures.StructureSpec TOWN = Structures.structure(id("test_town"))
            .template(ResourceLocation.withDefaultNamespace("village/plains/town_centers/plains_meeting_point_1"))
            .biomes(LAND).spacing(12, 4).salt(1_906_442).size(4).streets().grows(new Growth(3, 1, 1)).declare();
    /** The test land's monument, as the instruments that look for it name it. */
    public static final TagKey<Structure> MONUMENTS = TagKey.create(Registries.STRUCTURE, id("test_monuments"));
    /** How deep the monument is sunk into the ground. */
    public static final int MONUMENT_SUNK = 2;
    /** A monument at one place of the test land only, as a landmark's structure is, sunk into the ground. */
    public static final Structures.StructureSpec MONUMENT = Structures.structure(id("test_monument"))
            .template(ResourceLocation.withDefaultNamespace("village/plains/houses/plains_small_house_2"))
            .biomes(LAND).at(new ChunkPos(MONUMENT_CHUNK, 0)).salt(77_031)
            .fit(new StructureFit(4, -1, StructureFit.ANYWHERE.minY(), StructureFit.ANYWHERE.maxY(), false,
                    MONUMENT_SUNK)).declare();

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
