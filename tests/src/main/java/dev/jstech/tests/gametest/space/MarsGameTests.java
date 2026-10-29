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
import dev.jstech.core.worldgen.terrain.RoundMap;
import dev.jstech.core.worldgen.terrain.RoundMaps;
import dev.jstech.space.JsSpace;
import dev.jstech.space.worldgen.MarsBiomes;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Mars' ground, as its sphere shows it from orbit: made by its generator from its maps, its biomes lying where its maps
 * put them across the whole planet, the caps, the dark dunes, the chaos, the northern plains and the southern
 * highlands, Olympus Mons standing high and Valles Marineris cut deep where its height map has them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MarsGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_mars";
    private static final int BIOME_Y = 64;
    /* The grid the biomes are read on, in degrees, and how sure a map must be to say what lies there. */
    private static final double LONGITUDE_STEP = 7.5;
    private static final double LATITUDE_STEP = 5;
    private static final double SURE = 0.98;
    /* Olympus Mons' summit and a plain west of it, Valles Marineris' floor and its plateau south of it. */
    private static final double[] OLYMPUS = {-133.8, 18.6};
    private static final double[] AMAZONIS = {-160, 18.6};
    private static final double[] VALLES = {-75, -10};
    private static final double[] PLATEAU = {-75, -18};
    private static final int OLYMPUS_OVER = 40;
    private static final int VALLES_UNDER = 15;

    private MarsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void mars_isMadeByItsGeneratorFromItsMaps(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper);
        final ChunkGenerator generator = mars.getChunkSource().getGenerator();
        helper.assertTrue(generator instanceof NoiseBasedChunkGenerator noise
                && noise.generatorSettings().is(MarsWorld.TERRAIN.key()), "Mars is made by its own terrain");
        final Set<ResourceKey<Biome>> biomes = generator.getBiomeSource().possibleBiomes().stream()
                .map(holder -> holder.unwrapKey().orElseThrow()).collect(Collectors.toSet());
        for (final WorldBiomes.BiomeSpec biome : MarsBiomes.ALL) {
            helper.assertTrue(biomes.contains(biome.key()), "Mars holds the biome " + biome.key().location());
        }
        helper.assertTrue(biomes.size() == MarsBiomes.ALL.size(), "and none of another world's: " + biomes);
        for (final String map : new String[] {"north", "dark", "canyon", "polar", "height"}) {
            helper.assertTrue(RoundMaps.get(map(map)).isPresent(), "the server holds Mars' map of its " + map);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void biomes_lieWhereMarsMapsPutThemAcrossThePlanet(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper);
        final RoundMap north = RoundMaps.get(map("north")).orElseThrow();
        final RoundMap dark = RoundMaps.get(map("dark")).orElseThrow();
        final RoundMap canyon = RoundMaps.get(map("canyon")).orElseThrow();
        final RoundMap polar = RoundMaps.get(map("polar")).orElseThrow();
        final List<String> wrong = new ArrayList<>();
        int read = 0;
        for (double lat = -85; lat <= 85; lat += LATITUDE_STEP) {
            for (double lon = -180; lon < 180; lon += LONGITUDE_STEP) {
                final Set<ResourceKey<Biome>> expected = expected(north.at(lon, lat), dark.at(lon, lat),
                        canyon.at(lon, lat), polar.at(lon, lat));
                if (expected.isEmpty()) {
                    continue;
                }
                read++;
                final Holder<Biome> found = biome(mars, lon, lat);
                if (expected.stream().noneMatch(found::is)) {
                    wrong.add(lon + ", " + lat + ": " + found.unwrapKey().map(ResourceKey::location).orElse(null));
                }
            }
        }
        helper.assertTrue(read > 0 && wrong.isEmpty(), "every place a map is sure of holds its biome, of " + read
                + " read: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void heights_raiseOlympusMonsAndCutVallesMarineris(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper);
        final int olympus = ground(mars, OLYMPUS);
        final int amazonis = ground(mars, AMAZONIS);
        helper.assertTrue(olympus - amazonis > OLYMPUS_OVER, "Olympus Mons stands high over the plain west of it: "
                + olympus + " over " + amazonis);
        final int valles = ground(mars, VALLES);
        final int plateau = ground(mars, PLATEAU);
        helper.assertTrue(plateau - valles > VALLES_UNDER, "Valles Marineris is cut deep under the plateau south of"
                + " it: " + valles + " under " + plateau);
        helper.succeed();
    }

    /*
     * The biomes a place may hold where the maps are sure of it, none where they are not: a cap, a dark region's
     * dunes, a canyon's chaos, the northern plains, or one of the south's three.
     */
    private static Set<ResourceKey<Biome>> expected(final double north, final double dark, final double canyon,
                                                    final double polar) {
        final boolean none = dark < 1 - SURE && canyon < 1 - SURE && polar < 1 - SURE;
        if (polar > SURE && dark < 1 - SURE) {
            return Set.of(MarsBiomes.POLAR_CAP.key());
        }
        if (dark > SURE && canyon < 1 - SURE) {
            return Set.of(MarsBiomes.DARK_DUNES.key());
        }
        if (canyon > SURE && dark < 1 - SURE) {
            return Set.of(MarsBiomes.CHAOS_TERRAIN.key());
        }
        if (none && north > SURE) {
            return Set.of(MarsBiomes.NORTHERN_PLAINS.key());
        }
        if (none && north < 1 - SURE) {
            return Set.of(MarsBiomes.SOUTHERN_HIGHLANDS.key(), MarsBiomes.OXIDE_PLAINS.key(),
                    MarsBiomes.LAKEBED.key());
        }
        return Set.of();
    }

    /* Mars, made from its dimension's file under its own name: the test server makes only its flat preset's. */
    private static ServerLevel mars(final GameTestHelper helper) {
        final ResourceKey<Level> key = MarsWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }

    private static ResourceLocation map(final String which) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "mars/" + which);
    }

    private static double perDegree(final ServerLevel mars) {
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(mars.dimension()).orElseThrow();
        return (bounds.maxX() - bounds.minX()) / 360;
    }

    private static Holder<Biome> biome(final ServerLevel mars, final double lon, final double lat) {
        final double perDegree = perDegree(mars);
        return mars.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock((int) Math.round(lon * perDegree)), QuartPos.fromBlock(BIOME_Y),
                QuartPos.fromBlock((int) Math.round(-lat * perDegree)), mars.getChunkSource().randomState().sampler());
    }

    private static int ground(final ServerLevel mars, final double[] at) {
        final double perDegree = perDegree(mars);
        return mars.getChunkSource().getGenerator().getBaseHeight((int) Math.round(at[0] * perDegree),
                (int) Math.round(-at[1] * perDegree), Heightmap.Types.OCEAN_FLOOR_WG, mars,
                mars.getChunkSource().randomState());
    }
}
