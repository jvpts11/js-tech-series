/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.worldgen.biome.WorldBiomes;
import dev.jstech.core.worldgen.terrain.RoundMaps;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceLandmark;
import dev.jstech.space.api.SpaceLandmarks;
import dev.jstech.space.worldgen.MoonBiomes;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.JsTests;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Moon's ground, as its sphere shows it from orbit: made by its generator from its maps, its maria where the map
 * has them and its highlands round them, the craters of eternal shadow at its poles, Tycho a great crater at its place
 * with its young ejecta round it, and Tycho the first landmark of the worlds.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MoonGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_moon";
    /* Where biomes are read, about where the ground lies. */
    private static final int BIOME_Y = 64;
    /* Places of the Moon's map, by longitude and latitude: the middle of Mare Imbrium, far-side highlands, the
    south pole. */
    private static final double[] IMBRIUM = {-15.6, 32.8};
    private static final double[] FAR_HIGHLANDS = {120, -20};
    private static final double[] SOUTH_POLE = {0, -88};
    /* Shares of Tycho's radius: down in its bowl, and on its floor just past its central peak, a fifth across. */
    private static final double BOWL = 0.6;
    private static final double FLOOR = 0.25;
    private static final int RIM_OVER_BOWL = 40;
    private static final int PEAK_OVER_FLOOR = 25;

    private MoonGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void moon_isMadeByItsGeneratorFromItsMaps(final GameTestHelper helper) {
        final ServerLevel moon = moon(helper);
        final ChunkGenerator generator = moon.getChunkSource().getGenerator();
        helper.assertTrue(generator instanceof NoiseBasedChunkGenerator noise
                && noise.generatorSettings().is(MoonWorld.TERRAIN.key()), "the Moon is made by its own terrain");
        final Set<ResourceKey<Biome>> biomes = generator.getBiomeSource().possibleBiomes().stream()
                .map(holder -> holder.unwrapKey().orElseThrow()).collect(Collectors.toSet());
        for (final WorldBiomes.BiomeSpec biome : MoonBiomes.ALL) {
            helper.assertTrue(biomes.contains(biome.key()), "the Moon holds the biome " + biome.key().location());
        }
        helper.assertTrue(biomes.size() == MoonBiomes.ALL.size(), "and none of another world's: " + biomes);
        for (final String map : new String[] {"maria", "height", "young", "polar"}) {
            helper.assertTrue(RoundMaps.get(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "moon/" + map))
                    .isPresent(), "the server holds the Moon's map of its " + map);
        }
        helper.assertTrue(DimensionBorders.inForce(moon.dimension()).isPresent(),
                "and the Moon's size is in force for its generator");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void biomes_lieWhereTheMoonsMapPutsThem(final GameTestHelper helper) {
        final ServerLevel moon = moon(helper);
        final Holder<Biome> imbrium = biome(moon, IMBRIUM);
        helper.assertTrue(imbrium.is(MoonBiomes.MARIA), "the middle of Mare Imbrium is a mare; " + name(imbrium));
        final Holder<Biome> far = biome(moon, FAR_HIGHLANDS);
        helper.assertTrue(far.is(MoonBiomes.ANORTHOSITE) && !far.is(MoonBiomes.POLAR_CRATERS.key()),
                "the far side's highlands are highland; " + name(far));
        final Holder<Biome> pole = biome(moon, SOUTH_POLE);
        helper.assertTrue(pole.is(MoonBiomes.POLAR_CRATERS.key()), "the south pole holds the craters of eternal"
                + " shadow; " + name(pole));
        final Holder<Biome> tycho = biome(moon, new double[] {MoonWorld.TYCHO.longitude(),
                MoonWorld.TYCHO.latitude()});
        helper.assertTrue(tycho.is(MoonBiomes.YOUNG_CRATER.key()), "Tycho lies in its young ejecta; " + name(tycho));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void tycho_isAGreatCraterAtItsPlace(final GameTestHelper helper) {
        final ServerLevel moon = moon(helper);
        final SpaceLandmark tycho = MoonWorld.TYCHO;
        final double perDegree = width(moon) / 360;
        final int x = (int) Math.round(tycho.longitude() * perDegree);
        final int z = (int) Math.round(-tycho.latitude() * perDegree);
        final int radius = (int) Math.round(tycho.radius() * perDegree);
        final int middle = ground(moon, x, z);
        final int floor = ground(moon, x + (int) (radius * FLOOR), z);
        final int bowl = ground(moon, x + (int) (radius * BOWL), z);
        final int rim = ground(moon, x + radius, z);
        helper.assertTrue(rim - bowl > RIM_OVER_BOWL, "its rim stands high over its bowl, " + radius
                + " blocks from its middle: " + rim + " over " + bowl);
        helper.assertTrue(middle - floor > PEAK_OVER_FLOOR, "and its central peak rises from its floor: " + middle
                + " over " + floor);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void landmarks_beginWithTycho(final GameTestHelper helper) {
        final SpaceLandmark tycho = SpaceLandmarks.declared().get(ResourceLocation.fromNamespaceAndPath(
                JsSpace.MODID, "tycho"));
        helper.assertTrue(tycho != null && tycho.equals(MoonWorld.TYCHO), "Tycho is a landmark of the worlds");
        helper.assertTrue(tycho.world().equals(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "moon"))
                && tycho.kind() == SpaceLandmark.Kind.CRATER, "a crater of the Moon");
        helper.assertTrue(SpaceLandmarks.all(helper.getLevel()).containsValue(tycho),
                "known to the server as declared, no data pack saying otherwise");
        helper.succeed();
    }

    /*
     * The Moon. The game's test server makes only the dimensions of its flat preset, so the Moon is made from its own
     * dimension's file under its own name, as a world makes it.
     */
    private static ServerLevel moon(final GameTestHelper helper) {
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }

    private static double width(final ServerLevel moon) {
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(moon.dimension())
                .orElseThrow(() -> new GameTestAssertException("the Moon has its size"));
        return bounds.maxX() - bounds.minX();
    }

    /* The biome at a place of the Moon given as its longitude and latitude. */
    private static Holder<Biome> biome(final ServerLevel moon, final double[] at) {
        final double perDegree = width(moon) / 360;
        final RandomState random = moon.getChunkSource().randomState();
        return moon.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock((int) Math.round(at[0] * perDegree)), QuartPos.fromBlock(BIOME_Y),
                QuartPos.fromBlock((int) Math.round(-at[1] * perDegree)), random.sampler());
    }

    /* The height of the Moon's ground at a block, as its generator makes it, made or not. */
    private static int ground(final ServerLevel moon, final int x, final int z) {
        return moon.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, moon,
                moon.getChunkSource().randomState());
    }

    private static String name(final Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("?");
    }
}
