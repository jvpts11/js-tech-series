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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.worldgen.WorldGen;
import dev.jstech.core.worldgen.landform.LandformKind;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.terrain.DensitySpec;
import dev.jstech.core.worldgen.terrain.NoiseSpec;
import dev.jstech.core.worldgen.terrain.Terrains;
import dev.jstech.core.worldgen.terrain.density.CoreDensities;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceLandmark;
import dev.jstech.space.api.SpaceLandmarks;
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
 * The Moon's ground, as its sphere shows it from orbit: the maria where its map has them, low and dark, the bright
 * highlands round them, Tycho at its place with its rays, the craters of eternal shadow round its poles, and in the
 * maria the rilles and the skylights of lava tubes. Its biomes are laid by its maps: the maria's map says mare or
 * highland, its latitude the poles, the young craters' map where their bright ejecta lie, and a slow noise where a
 * mare is cut by rilles or holed by skylights. Its ores lie in veins in the rocks their minerals are found in.
 */
@TextHolder
public final class MoonWorld {

    /* Where the biomes lie by warmth: the polar craters, the highlands and the maria. */
    private static final float POLAR = -1.4F;
    private static final float HIGHLAND = -0.2F;
    private static final float MARE = 1F;
    /* And by wetness: a mare cut by rilles or holed by skylights, and the young craters' ejecta. */
    private static final float CUT = 0.7F;
    private static final float YOUNG = 2F;
    private static final int BEDROCK = 3;

    public static final TextKey TYCHO_NAME = TextKey.of("jsspace.landmark.tycho", "Tycho");

    /** Tycho, young and bright, its rays across the near side's south: the first landmark of the worlds. */
    public static final SpaceLandmark TYCHO = SpaceLandmarks.declare(id("tycho"), new SpaceLandmark(
            TYCHO_NAME.key(), SolarSystemWorlds.MOON, SpaceLandmark.Kind.CRATER, -11.4, -43.3, 2.6));

    /** The slow noise that cuts a mare with rilles here and holes it with skylights there. */
    public static final NoiseSpec PATCHES = Terrains.noise(id("moon_patches"), -11, 1.0, 1.0, 0.5);
    /** The Moon's ground, as its generator makes it with the modifiers laid over it. */
    public static final DensitySpec GROUND = Terrains.density(id("moon/ground"), (noises, densities) ->
            WorldShapes.ground(world(), noises, densities));
    /** How warm a place is, to its biomes: highland, mare, or polar. */
    public static final DensitySpec WARMTH = Terrains.density(id("moon/warmth"), (noises, densities) ->
            DensityFunctions.add(
                    CoreDensities.roundMap(id("moon/maria"), dimension(), HIGHLAND, MARE),
                    CoreDensities.roundMap(id("moon/polar"), dimension(), 0, POLAR - HIGHLAND)));
    /** How wet a place is, to its biomes: the slow noise, and the young craters' ejecta far above it. */
    public static final DensitySpec WETNESS = Terrains.density(id("moon/wetness"), (noises, densities) ->
            DensityFunctions.add(
                    DensityFunctions.noise(noises.getOrThrow(PATCHES.key()), 1.0, 0.0),
                    CoreDensities.roundMap(id("moon/young"), dimension(), 0, YOUNG)));
    public static final Terrains.TerrainSpec TERRAIN = Terrains.terrain(id("moon"))
            .heights(CrateredGenerator.MIN_Y, CrateredGenerator.HEIGHT).ground(CrateredGenerator.GROUND).hills(1, 64)
            .sea(CrateredGenerator.MIN_Y).stone(SpaceRocks.ANORTHOSITE).liquid(() -> Blocks.AIR).bedrock(BEDROCK)
            .density(GROUND)
            .surfaceRules(Terrains.surfaceRules().bedrock(JsSpace.MODID + ":moon_bedrock", BEDROCK)
                    .rule(() -> WorldShapes.surface(world())))
            .biomesBy(WARMTH, WETNESS)
            .declare();
    public static final WorldGen.DimensionSpec DIMENSION = WorldGen.dimension(id("moon"))
            .terrain(TERRAIN)
            // Its sky and its light by J's Core's sky kit, from its sky's file: the hour by longitude.
            .effects(Skies.EFFECTS)
            .biome(MoonBiomes.MARE.key(), MARE, 0F)
            .biome(MoonBiomes.HIGHLANDS.key(), HIGHLAND, 0F)
            .biome(MoonBiomes.POLAR_CRATERS.key(), POLAR, 0F)
            .biome(MoonBiomes.RILLE.key(), MARE, CUT)
            .biome(MoonBiomes.LAVA_TUBE_SKYLIGHT.key(), MARE, -CUT)
            .biome(MoonBiomes.YOUNG_CRATER.key(), (MARE + HIGHLAND) / 2, YOUNG)
            .declare();

    /** The maria's craters: fewer and smaller than the highlands', their rims of the basalt they broke. */
    public static final Landforms.LandformSpec MARIA_CRATERS = Landforms.landform(id("moon_maria_craters"),
                    LandformKind.CRATER).cell(256).chance(0.45).radius(6, 40).height(0.3)
            .surface(SpaceRocks.MARE_REGOLITH).fill(SpaceRocks.MARE_BASALT).scatter(SpaceRocks.MARE_BASALT)
            .biomes(MoonBiomes.MARIA_CRATERS).declare();
    /** The highlands' craters, crater on crater. */
    public static final Landforms.LandformSpec HIGHLAND_CRATERS = Landforms.landform(id("moon_highland_craters"),
                    LandformKind.CRATER).cell(160).chance(0.75).radius(8, 64).height(0.35)
            .surface(SpaceRocks.HIGHLAND_REGOLITH).fill(SpaceRocks.ANORTHOSITE).scatter(SpaceRocks.ANORTHOSITE)
            .biomes(MoonBiomes.HIGHLAND_CRATERS).declare();
    /** Fresh craters, their rims sharp and their ejecta bright. */
    public static final Landforms.LandformSpec FRESH_CRATERS = Landforms.landform(id("moon_fresh_craters"),
                    LandformKind.CRATER).cell(320).chance(0.9).radius(24, 140).height(0.4)
            .surface(SpaceRocks.HIGHLAND_REGOLITH).fill(SpaceRocks.ANORTHOSITE)
            .scatter(SpaceRocks.HIGHLAND_REGOLITH).biomes(MoonBiomes.FRESH_CRATERS).declare();
    /** The craters of eternal shadow, their floors of regolith held hard by ice. */
    public static final Landforms.LandformSpec SHADOWED_CRATERS = Landforms.landform(id("moon_shadowed_craters"),
                    LandformKind.CRATER).cell(192).chance(0.85).radius(12, 90).height(0.45)
            .surface(SpaceRocks.ICY_REGOLITH).fill(SpaceRocks.ANORTHOSITE).scatter(SpaceRocks.ANORTHOSITE)
            .biomes(MoonBiomes.SHADOWED_CRATERS).declare();
    /** Rilles, the winding valleys rivers of lava cut. */
    public static final Landforms.LandformSpec RILLES = Landforms.landform(id("moon_rilles"), LandformKind.RILLE)
            .cell(768).chance(0.8).radius(160, 420).height(0.04).surface(SpaceRocks.MARE_REGOLITH)
            .biomes(MoonBiomes.RILLES).declare();
    /** Lava tubes under the maria, open to the sky where their roofs fell in. */
    public static final Landforms.LandformSpec LAVA_TUBES = Landforms.landform(id("moon_lava_tubes"),
                    LandformKind.LAVA_TUBE).cell(512).chance(0.75).radius(120, 320).height(0.03)
            .special(SpaceRocks.MARE_BASALT).biomes(MoonBiomes.LAVA_TUBES).declare();

    /** The titanium of the maria: ilmenite, with armalcolite and tranquillityite. */
    public static final ResourceLocation MARIA_TITANIUM = Deposits.deposit(id("moon_maria_titanium"))
            .mineral(SeriesMinerals.ILMENITE, 6).mineral(SeriesMinerals.ARMALCOLITE, 1)
            .mineral(SeriesMinerals.TRANQUILLITYITE, 1).heights(20, 72).frequency(0.15).size(18, 5).density(0.5)
            .biomes(MoonBiomes.MARIA).rocks(SpaceOres.MARE_BASALT, SpaceOres.MARE_REGOLITH).declare();
    /** KREEP's rare earths, in merrillite in the maria's basalt. */
    public static final ResourceLocation KREEP = Deposits.deposit(id("moon_kreep"))
            .mineral(SeriesMinerals.MERRILLITE, 1).heights(10, 60).frequency(0.05).size(14, 4).density(0.4)
            .biomes(MoonBiomes.MARIA).rocks(SpaceOres.MARE_BASALT).declare();
    /** The iron of meteorites, lying in the highlands' regolith. */
    public static final ResourceLocation METEORITIC_IRON = Deposits.deposit(id("moon_meteoritic_iron"))
            .mineral(SeriesMinerals.NATIVE_IRON, 1).heights(50, 80).frequency(0.08).size(10, 3).density(0.4)
            .biomes(MoonBiomes.ANORTHOSITE).rocks(SpaceOres.HIGHLAND_REGOLITH).declare();

    private MoonWorld() {
    }

    /** Declares the Moon's ground, once, from the mod's constructor, after its worlds and its rocks. */
    public static void declare() {
        // Loading the class declares everything above.
    }

    /** The Moon as J's Space declares it. */
    public static SpaceWorld world() {
        return SpaceWorlds.declared().get(SolarSystemWorlds.MOON);
    }

    private static ResourceLocation dimension() {
        return world().dimension().location();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
