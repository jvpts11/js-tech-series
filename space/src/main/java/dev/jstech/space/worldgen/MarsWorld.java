/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.worldgen;

import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.worldgen.WorldGen;
import dev.jstech.core.worldgen.landform.LandformKind;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.terrain.DensitySpec;
import dev.jstech.core.worldgen.terrain.NoiseSpec;
import dev.jstech.core.worldgen.terrain.Terrains;
import dev.jstech.core.worldgen.terrain.density.CoreDensities;
import dev.jstech.core.worldgen.variation.RockRule;
import dev.jstech.core.worldgen.variation.RockVariations;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.geology.SpaceOres;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.WorldShapes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * Mars' ground, as its sphere shows it from orbit: the dichotomy between the low smooth north and the old cratered
 * south, Tharsis' volcanoes and Valles Marineris standing and cut where its height map has them, its dark regions of
 * basalt sand walking in dunes, chaos where its canyons open, and its caps of water ice under carbon dioxide frost.
 * Its biomes are laid by its maps: the dichotomy and the caps by their maps, the dunes by its dark regions', the chaos
 * by its canyons', and a slow noise where the south's plains are strewn with hematite or hold an old lake. Its basalt
 * and its mudstone are varied by geology, and its minerals lie in veins in their rocks.
 */
public final class MarsWorld {

    /* Where the biomes lie by warmth: the caps, the southern highlands and the northern plains. */
    private static final float POLAR = -1.4F;
    private static final float SOUTH = -0.2F;
    private static final float NORTH = 1F;
    /*
     * And by wetness: the south's oxide plains and old lakes, and the dark dunes and the chaos, north and south alike,
     * at the edge of the game's range of a biome's place; their maps push a place far past it, beyond where the slow
     * noise can pull it back.
     */
    private static final float PATCH = 0.4F;
    /* How much of the slow noise reaches the biomes: never past halfway to the dunes or the chaos. */
    private static final double NOISE = 0.5;
    private static final float DUNE = 2F;
    private static final float CHAOS = -2F;
    private static final float DARK_PUSH = 4F;
    private static final float CANYON_PUSH = -4F;
    private static final int BEDROCK = 3;
    /* How steep a wall is, its ground rising or falling this many blocks in two, for its rock's face to show. */
    private static final int SCARP = 3;

    /** The slow noise that strews the south with hematite here and holds an old lake there. */
    public static final NoiseSpec PATCHES = Terrains.noise(id("mars_patches"), -11, 1.0, 1.0, 0.5);
    /** Mars' ground, as its generator makes it with the modifiers laid over it. */
    public static final DensitySpec GROUND = Terrains.density(id("mars/ground"), (noises, densities) ->
            WorldShapes.ground(world(), noises, densities));
    /** How warm a place is, to its biomes: north, south, or a cap. */
    public static final DensitySpec WARMTH = Terrains.density(id("mars/warmth"), (noises, densities) ->
            DensityFunctions.add(
                    CoreDensities.roundMap(id("mars/north"), dimension(), SOUTH, NORTH),
                    CoreDensities.roundMap(id("mars/polar"), dimension(), 0, POLAR - NORTH)));
    /** How wet a place is, to its biomes: the slow noise, the dark regions far above it, the canyons far below. */
    public static final DensitySpec WETNESS = Terrains.density(id("mars/wetness"), (noises, densities) ->
            DensityFunctions.add(DensityFunctions.add(
                    DensityFunctions.mul(DensityFunctions.constant(NOISE),
                            DensityFunctions.noise(noises.getOrThrow(PATCHES.key()), 1.0, 0.0)),
                    CoreDensities.roundMap(id("mars/dark"), dimension(), 0, DARK_PUSH)),
                    CoreDensities.roundMap(id("mars/canyon"), dimension(), 0, CANYON_PUSH)));
    public static final Terrains.TerrainSpec TERRAIN = Terrains.terrain(id("mars"))
            .heights(CrateredGenerator.MIN_Y, CrateredGenerator.HEIGHT).ground(CrateredGenerator.GROUND).hills(1, 64)
            .sea(CrateredGenerator.MIN_Y).stone(SpaceRocks.MARTIAN_BASALT).liquid(() -> Blocks.AIR).bedrock(BEDROCK)
            .density(GROUND)
            .surfaceRules(Terrains.surfaceRules().bedrock(JsSpace.MODID + ":mars_bedrock", BEDROCK)
                    .rule(() -> WorldShapes.surface(world())))
            .biomesBy(WARMTH, WETNESS)
            .declare();
    public static final WorldGen.DimensionSpec DIMENSION = WorldGen.dimension(id("mars"))
            .terrain(TERRAIN)
            // Its sky and its light by J's Core's sky kit, from its sky's file: the hour by longitude.
            .effects(Skies.EFFECTS)
            .biome(MarsBiomes.NORTHERN_PLAINS.key(), NORTH, 0F)
            .biome(MarsBiomes.SOUTHERN_HIGHLANDS.key(), SOUTH, 0F)
            .biome(MarsBiomes.POLAR_CAP.key(), POLAR, 0F)
            .biome(MarsBiomes.OXIDE_PLAINS.key(), SOUTH, PATCH)
            .biome(MarsBiomes.LAKEBED.key(), SOUTH, -PATCH)
            .biome(MarsBiomes.DARK_DUNES.key(), NORTH, DUNE)
            .biome(MarsBiomes.DARK_DUNES.key(), SOUTH, DUNE)
            .biome(MarsBiomes.CHAOS_TERRAIN.key(), NORTH, CHAOS)
            .biome(MarsBiomes.CHAOS_TERRAIN.key(), SOUTH, CHAOS)
            .declare();

    /** The southern highlands' craters, their rims of the basalt they broke. */
    public static final Landforms.LandformSpec CRATERS = Landforms.landform(id("mars_craters"), LandformKind.CRATER)
            .cell(192).chance(0.7).radius(8, 60).height(0.3).surface(SpaceRocks.MARTIAN_REGOLITH)
            .fill(SpaceRocks.MARTIAN_BASALT).scatter(SpaceRocks.MARTIAN_BASALT).biomes(MarsBiomes.CRATERED)
            .declare();
    /** Crescents of dark basalt sand. */
    public static final Landforms.LandformSpec DUNES = Landforms.landform(id("mars_dunes"), LandformKind.DUNES)
            .cell(64).chance(0.9).radius(12, 40).height(0.3).surface(SpaceRocks.BASALT_SAND)
            .fill(SpaceRocks.BASALT_SAND).biomes(MarsBiomes.DUNES).declare();
    /** The flat-topped blocks of chaos terrain, of mudstone under the red regolith. */
    public static final Landforms.LandformSpec CHAOS_BLOCKS = Landforms.landform(id("mars_chaos_blocks"),
                    LandformKind.MESA).cell(96).chance(0.95).radius(12, 40).height(0.5)
            .surface(SpaceRocks.MARTIAN_REGOLITH).fill(SpaceRocks.MUDSTONE).biomes(MarsBiomes.CHAOS).declare();
    /** The craters that held old lakes, their floors of the mudstone the water laid. */
    public static final Landforms.LandformSpec LAKE_CRATERS = Landforms.landform(id("mars_lake_craters"),
                    LandformKind.CRATER).cell(768).chance(0.85).radius(150, 400).height(0.08)
            .surface(SpaceRocks.MUDSTONE).fill(SpaceRocks.MARTIAN_BASALT).scatter(SpaceRocks.MARTIAN_BASALT)
            .biomes(MarsBiomes.LAKES).declare();
    /** The spiral troughs cut in the caps, their walls of layered water ice. */
    public static final Landforms.LandformSpec TROUGHS = Landforms.landform(id("mars_troughs"), LandformKind.CANYON)
            .cell(640).chance(0.7).radius(120, 300).height(0.12).surface(SpaceRocks.WATER_ICE)
            .biomes(MarsBiomes.TROUGHS).declare();

    /** Martian Basalt in columns on the scarps and walls, pocked by gas on the tops of flows, faceted below. */
    public static final RockVariations.Spec BASALT = RockVariations.rock(id("mars_basalt_variations"),
                    SpaceRocks.MARTIAN_BASALT)
            .variant(SpaceRocks.COLUMNAR_MARTIAN_BASALT, RockRule.ANYWHERE.exposed(RockRule.Exposure.SIDE)
                    .slope(SCARP))
            .variant(SpaceRocks.VESICULAR_MARTIAN_BASALT, RockRule.ANYWHERE.exposed(RockRule.Exposure.TOP))
            .biomes(MarsBiomes.BASALT).declare();
    /** Mudstone dusted red in the upper layers of every exposure, buff on the scarps, Gale's grey below. */
    public static final RockVariations.Spec MUDSTONE = RockVariations.rock(id("mars_mudstone_variations"),
                    SpaceRocks.MUDSTONE)
            .variant(SpaceRocks.DUSTED_MUDSTONE, RockRule.ANYWHERE.depth(0, 2))
            .variant(SpaceRocks.BUFF_MUDSTONE, RockRule.ANYWHERE.exposed(RockRule.Exposure.SIDE).slope(SCARP))
            .biomes(MarsBiomes.MUDSTONE).declare();

    /** Hematite, in the regolith of the oxide plains and the mudstone of old lakes. */
    public static final ResourceLocation HEMATITE = Deposits.deposit(id("mars_hematite"))
            .mineral(SeriesMinerals.HEMATITE, 1).heights(40, 110).frequency(0.2).size(16, 4).density(0.5)
            .biomes(MarsBiomes.OXIDES).rocks(SpaceOres.MARTIAN_REGOLITH, SpaceOres.MUDSTONE).declare();
    /** Magnetite and olivine, in the basalt. */
    public static final ResourceLocation MAFIC = Deposits.deposit(id("mars_mafic"))
            .mineral(SeriesMinerals.MAGNETITE, 2).mineral(SeriesMinerals.OLIVINE, 1).heights(0, 100).frequency(0.12)
            .size(18, 5).density(0.45).biomes(MarsBiomes.BASALT).rocks(SpaceOres.MARTIAN_BASALT).declare();
    /** Jarosite and gypsum, the proofs of old water, in the mudstone. */
    public static final ResourceLocation SULFATES = Deposits.deposit(id("mars_sulfates"))
            .mineral(SeriesMinerals.GYPSUM, 2).mineral(SeriesMinerals.JAROSITE, 1).heights(20, 110).frequency(0.15)
            .size(14, 4).density(0.45).biomes(MarsBiomes.MUDSTONE).rocks(SpaceOres.MUDSTONE).declare();

    private MarsWorld() {
    }

    /** Declares Mars' ground, once, from the mod's constructor, after its worlds and its rocks. */
    public static void declare() {
        // Loading the class declares everything above.
    }

    /** Mars as J's Space declares it. */
    public static SpaceWorld world() {
        return SpaceWorlds.declared().get(SolarSystemWorlds.MARS);
    }

    private static ResourceLocation dimension() {
        return world().dimension().location();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
