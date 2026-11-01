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
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.worldgen.MarsBiomes;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
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
    /* The new biomes' real places: Home Plate, Nili Patera, Mount Sharp, Candor Chasma's deposits and Inca City. */
    private static final double[] HOME_PLATE = {175.5, -14.6};
    private static final double[] NILI_PATERA = {67.3, 8.9};
    private static final double[] MOUNT_SHARP = {137.8, -5.1};
    private static final double[] CANDOR = {-71.0, -6.5};
    private static final double[] INCA_CITY = {-64.0, -81.0};
    /* The stretch of chunks made round each, either way of its middle, and how deep its ground is read. */
    private static final int STRETCH = 4;
    private static final int SKIN = 4;

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
        for (final String map : new String[] {"north", "dark", "canyon", "polar", "height", "springs", "layered",
                "volcanic", "cryptic"}) {
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
        final RoundMap volcanic = RoundMaps.get(map("volcanic")).orElseThrow();
        final RoundMap[] own = {RoundMaps.get(map("springs")).orElseThrow(),
                RoundMaps.get(map("layered")).orElseThrow(), RoundMaps.get(map("cryptic")).orElseThrow()};
        final List<String> wrong = new ArrayList<>();
        int read = 0;
        for (double lat = -85; lat <= 85; lat += LATITUDE_STEP) {
            for (double lon = -180; lon < 180; lon += LONGITUDE_STEP) {
                final Set<ResourceKey<Biome>> expected = expected(north.at(lon, lat), dark.at(lon, lat),
                        canyon.at(lon, lat), polar.at(lon, lat), volcanic.at(lon, lat), own[0].at(lon, lat),
                        own[1].at(lon, lat), own[2].at(lon, lat));
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

    @GameTest(template = ARENA, batch = BATCH)
    public static void newBiomes_lieAtTheirRealPlaces(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper);
        final Holder<Biome> homePlate = biome(mars, HOME_PLATE[0], HOME_PLATE[1]);
        helper.assertTrue(homePlate.is(MarsBiomes.LIFELESS_SPRINGS.key()), "Home Plate in Gusev holds old springs: "
                + homePlate.unwrapKey().map(ResourceKey::location).orElse(null));
        final Holder<Biome> nili = biome(mars, NILI_PATERA[0], NILI_PATERA[1]);
        helper.assertTrue(nili.is(MarsBiomes.LIFELESS_SPRINGS.key()), "and Nili Patera, in Syrtis Major's dark: "
                + nili.unwrapKey().map(ResourceKey::location).orElse(null));
        final Holder<Biome> sharp = biome(mars, MOUNT_SHARP[0], MOUNT_SHARP[1]);
        helper.assertTrue(sharp.is(MarsBiomes.LAYERED_MOUNDS.key()), "Mount Sharp in Gale is a layered mound: "
                + sharp.unwrapKey().map(ResourceKey::location).orElse(null));
        final Holder<Biome> candor = biome(mars, CANDOR[0], CANDOR[1]);
        helper.assertTrue(candor.is(MarsBiomes.LAYERED_MOUNDS.key()), "and Candor Chasma's layered deposits: "
                + candor.unwrapKey().map(ResourceKey::location).orElse(null));
        final Holder<Biome> inca = biome(mars, INCA_CITY[0], INCA_CITY[1]);
        helper.assertTrue(inca.is(MarsBiomes.SPIDER_FIELDS.key()), "Inca City lies in the spider fields: "
                + inca.unwrapKey().map(ResourceKey::location).orElse(null));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 2400)
    public static void ground_ofEachNewBiomeIsOfItsOwnBlocks(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper);
        helper.runAfterDelay(1, () -> {
            final Map<Block, Integer> springs = blocksOf(mars, HOME_PLATE);
            helper.assertTrue(springs.getOrDefault(SpaceRocks.SILICA_SOIL.get(), 0) > 0, "the springs' red dust lies"
                    + " over white silica soil: " + springs);
            helper.assertTrue(springs.getOrDefault(SpaceRocks.SILICA_SINTER.get(), 0)
                    + springs.getOrDefault(SpaceRocks.NODULAR_SILICA.get(), 0) > 0, "and their terraces, mounds and"
                    + " chimneys stand in sinter and nodules: " + springs);
        });
        helper.runAfterDelay(2, () -> {
            final Map<Block, Integer> layered = blocksOf(mars, MOUNT_SHARP);
            helper.assertTrue(layered.getOrDefault(SpaceRocks.LAYERED_SULFATE.get(), 0) > 0, "Mount Sharp's ground"
                    + " is of layered sulfate: " + layered);
        });
        helper.runAfterDelay(3, () -> {
            final Map<Block, Integer> spiders = blocksOf(mars, INCA_CITY);
            helper.assertTrue(spiders.getOrDefault(SpaceRocks.DRY_ICE.get(), 0) > 0, "the spider fields lie under"
                    + " seasonal ice: " + spiders);
            helper.assertTrue(spiders.getOrDefault(SpaceRocks.BASALT_SAND.get(), 0)
                    + spiders.getOrDefault(SpaceRocks.FAN_DUST.get(), 0) > 0, "cut by spiders floored with dark sand"
                    + " and their fans: " + spiders);
            helper.succeed();
        });
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
     * The biomes a place may hold where the maps are sure of it, none where they are not: its zone's own or one of the
     * zone's patches. A new biome's own zone where its map lies; a cap; a dark region's dunes, opened onto its
     * hemisphere's ground, either where a cap cools it; a canyon's chaos or an old lake; the northern plains or dark
     * dunes, old springs rare among them in the volcanic regions; the southern highlands, one of the south's three
     * patches or old springs.
     */
    private static Set<ResourceKey<Biome>> expected(final double north, final double dark, final double canyon,
                                                    final double polar, final double volcanic, final double springs,
                                                    final double layered, final double cryptic) {
        if (springs > SURE) {
            return Set.of(MarsBiomes.LIFELESS_SPRINGS.key());
        }
        if (layered > SURE) {
            return Set.of(MarsBiomes.LAYERED_MOUNDS.key());
        }
        if (cryptic > SURE) {
            return Set.of(MarsBiomes.SPIDER_FIELDS.key());
        }
        if (springs > 1 - SURE || layered > 1 - SURE || cryptic > 1 - SURE) {
            return Set.of();
        }
        final boolean none = dark < 1 - SURE && canyon < 1 - SURE && polar < 1 - SURE;
        if (none && north > SURE && volcanic > 1 - SURE) {
            return Set.of(MarsBiomes.NORTHERN_PLAINS.key(), MarsBiomes.DARK_DUNES.key(),
                    MarsBiomes.LIFELESS_SPRINGS.key());
        }
        if (polar > SURE && dark < 1 - SURE) {
            return Set.of(MarsBiomes.POLAR_CAP.key());
        }
        if (dark > SURE && canyon < 1 - SURE) {
            if (polar < 1 - SURE && north > SURE) {
                return Set.of(MarsBiomes.DARK_DUNES.key(), MarsBiomes.NORTHERN_PLAINS.key());
            }
            if (polar < 1 - SURE && north < 1 - SURE) {
                return Set.of(MarsBiomes.DARK_DUNES.key(), MarsBiomes.SOUTHERN_HIGHLANDS.key());
            }
            return Set.of(MarsBiomes.DARK_DUNES.key(), MarsBiomes.NORTHERN_PLAINS.key(),
                    MarsBiomes.SOUTHERN_HIGHLANDS.key());
        }
        if (canyon > SURE && dark < 1 - SURE) {
            return Set.of(MarsBiomes.CHAOS_TERRAIN.key(), MarsBiomes.LAKEBED.key());
        }
        if (none && north > SURE) {
            return Set.of(MarsBiomes.NORTHERN_PLAINS.key(), MarsBiomes.DARK_DUNES.key());
        }
        if (none && north < 1 - SURE) {
            return Set.of(MarsBiomes.SOUTHERN_HIGHLANDS.key(), MarsBiomes.OXIDE_PLAINS.key(),
                    MarsBiomes.LAKEBED.key(), MarsBiomes.DARK_DUNES.key(), MarsBiomes.LIFELESS_SPRINGS.key());
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

    /*
     * The blocks of the ground of a stretch of chunks round a place given as its longitude and latitude, made whole:
     * the top few blocks of each column and whatever stands on them, counted by block.
     */
    private static Map<Block, Integer> blocksOf(final ServerLevel mars, final double[] at) {
        final double perDegree = perDegree(mars);
        final int middleX = SectionPos.blockToSectionCoord((int) Math.round(at[0] * perDegree));
        final int middleZ = SectionPos.blockToSectionCoord((int) Math.round(-at[1] * perDegree));
        final Map<Block, Integer> counts = new HashMap<>();
        for (int cx = middleX - STRETCH; cx < middleX + STRETCH; cx++) {
            for (int cz = middleZ - STRETCH; cz < middleZ + STRETCH; cz++) {
                final LevelChunk chunk = mars.getChunk(cx, cz);
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                        final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                        for (int y = top - SKIN; y <= top; y++) {
                            counts.merge(chunk.getBlockState(new BlockPos(x, y, z)).getBlock(), 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return counts;
    }

    private static int ground(final ServerLevel mars, final double[] at) {
        final double perDegree = perDegree(mars);
        return mars.getChunkSource().getGenerator().getBaseHeight((int) Math.round(at[0] * perDegree),
                (int) Math.round(-at[1] * perDegree), Heightmap.Types.OCEAN_FLOOR_WG, mars,
                mars.getChunkSource().randomState());
    }
}
