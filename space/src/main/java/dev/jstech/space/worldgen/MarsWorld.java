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
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Mars' ground, as its sphere shows it from orbit: the dichotomy between the low smooth north and the old cratered
 * south, Tharsis' volcanoes and Valles Marineris standing and cut where its height map has them, its dark regions of
 * basalt sand walking in dunes, chaos where its canyons open, and its caps of water ice under carbon dioxide frost.
 * Its biomes lie in zones laid by its maps, where they lie on the real planet: the dichotomy and the caps by their
 * maps, the dunes by its dark regions', the chaos by its canyons'. Inside each zone they vary in patches a thousand
 * blocks or two across, as the overworld's biomes vary: the south's highlands strewn with hematite here, holding an
 * old lake there, walked by dark dunes elsewhere; the north's plains by dark dunes; a dark region opened onto its
 * hemisphere's ground; a canyon's floor an old lake. Three biomes more lie where their own maps put them: old hot
 * springs at Gusev, Nili Patera and Vernal, rare too in the south and the volcanic regions; layered mounds at Gale and
 * in Valles Marineris; spider fields in the cryptic region by the south cap. Its basalt and its mudstone are varied by
 * geology, and its minerals lie in veins in their rocks.
 */
public final class MarsWorld {

    /* Where the zones lie by warmth: the caps, the southern highlands and the northern plains. */
    private static final float POLAR = -1.4F;
    private static final float SOUTH = -0.2F;
    private static final float NORTH = 1F;
    /*
     * And by wetness: the dark dunes and the chaos, north and south alike, at the edge of the game's range of a
     * biome's place; their maps push a place past it, so a region holds its zone a quarter of the way into the map's
     * edge.
     */
    private static final float DUNE = 2F;
    private static final float CHAOS = -2F;
    private static final float DARK_PUSH = 4F;
    private static final float CANYON_PUSH = -4F;
    /*
     * The volcanic regions, a little wetter than the plains round them, so the north's and the south's each hold a
     * zone of their own there, old springs lying in it here and there.
     */
    private static final float VOLCANIC = 0.7F;
    /*
     * The corner of the range a new biome's zone takes where its map lies, warmth and wetness set there whatever the
     * place was: the springs' warm and wet, the layered mounds' warm and dry, the cryptic region's cold and dry; no
     * other zone reaches a corner.
     */
    private static final float CORNER = 2F;
    /* About how many blocks apart the patches of a zone come. */
    private static final int PATCHES = 2048;
    private static final int BEDROCK = 3;
    /* How steep a wall is, its ground rising or falling this many blocks in two, for its rock's face to show. */
    private static final int SCARP = 3;

    /** Mars' ground, as its generator makes it with the modifiers laid over it. */
    public static final DensitySpec GROUND = Terrains.density(id("mars/ground"), (noises, densities) ->
            WorldShapes.ground(world(), noises, densities));
    /** How warm a place is, to its biomes: north, south, or a cap; or a new biome's own where its map lies. */
    public static final DensitySpec WARMTH = Terrains.density(id("mars/warmth"), (noises, densities) ->
            corners(DensityFunctions.add(
                    CoreDensities.roundMap(id("mars/north"), dimension(), SOUTH, NORTH),
                    CoreDensities.roundMap(id("mars/polar"), dimension(), 0, POLAR - NORTH)),
                    CORNER, CORNER, -CORNER));
    /**
     * How wet a place is, to its biomes: the dark regions far above the rest, the canyons far below, the volcanic
     * regions a little above; or a new biome's own where its map lies.
     */
    public static final DensitySpec WETNESS = Terrains.density(id("mars/wetness"), (noises, densities) ->
            corners(DensityFunctions.add(DensityFunctions.add(
                    CoreDensities.roundMap(id("mars/dark"), dimension(), 0, DARK_PUSH),
                    CoreDensities.roundMap(id("mars/canyon"), dimension(), 0, CANYON_PUSH)),
                    CoreDensities.roundMap(id("mars/volcanic"), dimension(), 0, VOLCANIC)),
                    CORNER, -CORNER, -CORNER));
    public static final Terrains.TerrainSpec TERRAIN = Terrains.terrain(id("mars"))
            .heights(CrateredGenerator.MIN_Y, CrateredGenerator.HEIGHT).ground(CrateredGenerator.GROUND).hills(1, 64)
            .sea(CrateredGenerator.MIN_Y).stone(SpaceRocks.MARTIAN_BASALT).liquid(() -> Blocks.AIR).bedrock(BEDROCK)
            .density(GROUND)
            .surfaceRules(Terrains.surfaceRules().bedrock(JsSpace.MODID + ":mars_bedrock", BEDROCK)
                    .rule(() -> WorldShapes.surface(world())))
            .biomesBy(WARMTH, WETNESS)
            .patches(PATCHES)
            .declare();
    public static final WorldGen.DimensionSpec DIMENSION = WorldGen.dimension(id("mars"))
            .terrain(TERRAIN)
            // Its sky and its light by J's Core's sky kit, from its sky's file: the hour by longitude.
            .effects(Skies.EFFECTS)
            .biome(MarsBiomes.NORTHERN_PLAINS.key(), NORTH, 0F)
            .biome(MarsBiomes.SOUTHERN_HIGHLANDS.key(), SOUTH, 0F)
            .biome(MarsBiomes.POLAR_CAP.key(), POLAR, 0F)
            .biome(MarsBiomes.DARK_DUNES.key(), NORTH, DUNE)
            .biome(MarsBiomes.DARK_DUNES.key(), SOUTH, DUNE)
            .biome(MarsBiomes.CHAOS_TERRAIN.key(), NORTH, CHAOS)
            .biome(MarsBiomes.CHAOS_TERRAIN.key(), SOUTH, CHAOS)
            .patches(SOUTH, 0F, MarsBiomes.OXIDE_PLAINS.key(), MarsBiomes.LAKEBED.key(), MarsBiomes.DARK_DUNES.key())
            .patches(NORTH, 0F, MarsBiomes.DARK_DUNES.key())
            .patches(NORTH, DUNE, MarsBiomes.NORTHERN_PLAINS.key())
            .patches(SOUTH, DUNE, MarsBiomes.SOUTHERN_HIGHLANDS.key())
            .patches(NORTH, CHAOS, MarsBiomes.LAKEBED.key())
            .patches(SOUTH, CHAOS, MarsBiomes.LAKEBED.key())
            // The volcanic regions: the north's and the south's own biomes and patches, old springs rare among them.
            .biome(MarsBiomes.NORTHERN_PLAINS.key(), NORTH, VOLCANIC)
            .biome(MarsBiomes.SOUTHERN_HIGHLANDS.key(), SOUTH, VOLCANIC)
            .patches(NORTH, VOLCANIC, MarsBiomes.DARK_DUNES.key())
            .patches(SOUTH, VOLCANIC, MarsBiomes.OXIDE_PLAINS.key(), MarsBiomes.LAKEBED.key(),
                    MarsBiomes.DARK_DUNES.key())
            .rarePatches(SOUTH, 0F, MarsBiomes.LIFELESS_SPRINGS.key())
            .rarePatches(NORTH, VOLCANIC, MarsBiomes.LIFELESS_SPRINGS.key())
            .rarePatches(SOUTH, VOLCANIC, MarsBiomes.LIFELESS_SPRINGS.key())
            // The new biomes' own zones, each at its corner where its map lies.
            .biome(MarsBiomes.LIFELESS_SPRINGS.key(), CORNER, CORNER)
            .biome(MarsBiomes.LAYERED_MOUNDS.key(), CORNER, -CORNER)
            .biome(MarsBiomes.SPIDER_FIELDS.key(), -CORNER, -CORNER)
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
    /**
     * An old spring's terraces: dry pools stepping down an apron of sinter, red dust on their floors, each held by a
     * rim of nodules, the sinter bare on the steps.
     */
    public static final Landforms.LandformSpec SPRING_TERRACES = Landforms.landform(id("mars_spring_terraces"),
                    LandformKind.TERRACES).cell(160).chance(0.8).radius(16, 40).height(0.15)
            .surface(SpaceRocks.MARTIAN_REGOLITH).fill(SpaceRocks.SILICA_SINTER).scatter(SpaceRocks.NODULAR_SILICA)
            .biomes(MarsBiomes.SPRINGS).declare();
    /** An old spring's dry discharge channels, winding down from it, floored with nodular silica. */
    public static final Landforms.LandformSpec SPRING_CHANNELS = Landforms.landform(id("mars_spring_channels"),
                    LandformKind.RIVER).cell(128).chance(0.6).radius(24, 60).height(0.04)
            .surface(SpaceRocks.NODULAR_SILICA).special(() -> Blocks.AIR).scatter(() -> Blocks.AIR)
            .biomes(MarsBiomes.SPRINGS).declare();
    /** Extinct geysers: low mounds of sinter, nodules round their vents, the vents open and dry. */
    public static final Landforms.LandformSpec EXTINCT_GEYSERS = Landforms.landform(id("mars_extinct_geysers"),
                    LandformKind.GEYSER).cell(96).chance(0.6).radius(4, 8).height(0.6)
            .surface(SpaceRocks.NODULAR_SILICA).fill(SpaceRocks.SILICA_SINTER).special(() -> Blocks.AIR)
            .scatter(SpaceRocks.SILICA_SINTER).biomes(MarsBiomes.SPRINGS).declare();
    /** Short chimneys of nodular silica, hollow at their tops. */
    public static final Landforms.LandformSpec SPRING_CHIMNEYS = Landforms.landform(id("mars_spring_chimneys"),
                    LandformKind.CHIMNEY).cell(32).chance(0.3).radius(1, 2).height(2.0)
            .surface(SpaceRocks.NODULAR_SILICA).fill(SpaceRocks.NODULAR_SILICA).biomes(MarsBiomes.SPRINGS).declare();
    /** Layered mounds as Mount Sharp: light sulfate, grey, buff and dusted mudstone in beds, climbing in steps. */
    public static final Landforms.LandformSpec LAYERED_MOUND = Landforms.landform(id("mars_layered_mound"),
                    LandformKind.LAYERED_MOUND).cell(768).chance(0.85).radius(120, 300).height(0.2)
            .surface(SpaceRocks.LAYERED_SULFATE)
            .bands(SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE,
                    SpaceRocks.MUDSTONE, SpaceRocks.MUDSTONE, SpaceRocks.MUDSTONE,
                    SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE,
                    SpaceRocks.BUFF_MUDSTONE, SpaceRocks.BUFF_MUDSTONE, SpaceRocks.BUFF_MUDSTONE,
                    SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE, SpaceRocks.LAYERED_SULFATE,
                    SpaceRocks.DUSTED_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE)
            .biomes(MarsBiomes.LAYERED).declare();
    /** Buttes of mudstone in beds under a dark caprock, as the Murray buttes at Mount Sharp's foot. */
    public static final Landforms.LandformSpec BUTTES = Landforms.landform(id("mars_buttes"), LandformKind.MESA)
            .cell(128).chance(0.6).radius(8, 20).height(0.7).surface(SpaceRocks.MARTIAN_BASALT)
            .fill(SpaceRocks.MUDSTONE).bands(SpaceRocks.MUDSTONE, SpaceRocks.MUDSTONE, SpaceRocks.BUFF_MUDSTONE,
                    SpaceRocks.BUFF_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE)
            .biomes(MarsBiomes.LAYERED).declare();
    /** Yardangs: ridges the wind cut in a bed of sulfate, all one way. */
    public static final Landforms.LandformSpec YARDANGS = Landforms.landform(id("mars_yardangs"), LandformKind.DUNES)
            .cell(96).chance(0.7).radius(20, 50).height(0.06).surface(SpaceRocks.LAYERED_SULFATE)
            .fill(SpaceRocks.LAYERED_SULFATE).biomes(MarsBiomes.LAYERED).declare();
    /** Spiders: troughs carved from a pit under the seasonal ice, floored with dark sand, a dark fan downwind. */
    public static final Landforms.LandformSpec SPIDERS = Landforms.landform(id("mars_spiders"), LandformKind.SPIDER)
            .cell(64).chance(0.7).radius(10, 24).surface(SpaceRocks.BASALT_SAND).scatter(SpaceRocks.FAN_DUST)
            .biomes(MarsBiomes.SPIDERS).declare();
    /** The ridges of Inca City: straight, low, meeting at angles. */
    public static final Landforms.LandformSpec INCA_RIDGES = Landforms.landform(id("mars_inca_ridges"),
                    LandformKind.RIDGES).cell(512).chance(0.5).radius(60, 160).height(0.04)
            .surface(SpaceRocks.MARTIAN_REGOLITH).fill(SpaceRocks.MARTIAN_BASALT).biomes(MarsBiomes.SPIDERS)
            .declare();
    /**
     * Lava tubes in Tharsis and the other volcanic regions, walled in basalt, a row of skylights open in their
     * roofs, the red regolith that fell heaped under each.
     */
    public static final Landforms.LandformSpec LAVA_TUBES = Landforms.landform(id("mars_lava_tubes"),
                    LandformKind.LAVA_TUBE).cell(768).chance(0.7).radius(120, 300).height(0.06)
            .special(SpaceRocks.MARTIAN_BASALT).scatter(SpaceRocks.MARTIAN_REGOLITH).biomes(MarsBiomes.CAVES)
            .within(id("mars/volcanic"), dimension()).declare();
    /**
     * Grottos in the canyons' walls, the faces of the chaos' blocks and of the layered mounds and buttes, at their feet
     * and up them: laid after the other landforms, so the blocks and mounds they open in already stand.
     */
    public static final Landforms.LandformSpec CANYON_GROTTOS = Landforms.landform(id("mars_canyon_grottos"),
                    LandformKind.GROTTO).cell(48).chance(0.9).radius(24, 40).height(0.25).biomes(MarsBiomes.CAVES)
            .middle(MarsBiomes.GROTTOS).step(GenerationStep.Decoration.LAKES).declare();
    /** Tunnels in the caps' water ice under the frost, dark deep inside, opening where they come out. */
    public static final Landforms.LandformSpec ICE_CAVES = Landforms.landform(id("mars_ice_caves"),
                    LandformKind.CAVE).cell(128).chance(0.6).radius(24, 48).height(0.3).biomes(MarsBiomes.CAVES)
            .middle(MarsBiomes.CAPS).declare();
    /** Caves under all of Mars' ground, chambers and winding tunnels, many open to the sky. */
    public static final Landforms.LandformSpec CAVES = Landforms.landform(id("mars_caves"), LandformKind.CAVE)
            .cell(112).chance(0.55).radius(24, 48).height(0.5).biomes(MarsBiomes.CAVES).declare();
    /** Small pits anywhere on Mars, opening on small hollows, the regolith that fell heaped under them. */
    public static final Landforms.LandformSpec PITS = Landforms.landform(id("mars_pits"), LandformKind.PIT)
            .cell(320).chance(0.4).radius(2, 4).height(4).special(() -> Blocks.AIR)
            .scatter(SpaceRocks.MARTIAN_REGOLITH).biomes(MarsBiomes.CAVES).declare();

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

    /*
     * {@code base} where none of the new biomes' maps lies, and their own value where one does, blended across a map's
     * soft edge: the springs', the layered mounds' and the cryptic region's.
     */
    private static DensityFunction corners(final DensityFunction base, final float springs, final float layered,
                                           final float cryptic) {
        DensityFunction out = base;
        out = DensityFunctions.lerp(CoreDensities.roundMap(id("mars/springs"), dimension(), 0, 1), out,
                DensityFunctions.constant(springs));
        out = DensityFunctions.lerp(CoreDensities.roundMap(id("mars/layered"), dimension(), 0, 1), out,
                DensityFunctions.constant(layered));
        return DensityFunctions.lerp(CoreDensities.roundMap(id("mars/cryptic"), dimension(), 0, 1), out,
                DensityFunctions.constant(cryptic));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
