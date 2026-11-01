/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.worldgen.biome.BiomeSearch;
import dev.jstech.core.worldgen.terrain.RoundMap;
import dev.jstech.core.worldgen.terrain.RoundMaps;
import dev.jstech.space.JsSpace;
import dev.jstech.space.worldgen.MarsBiomes;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonBiomes;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Moon's and Mars' biomes in two scales: zones where their maps lay them, where they lie on the real worlds, and
 * inside each zone its patch biomes in patches a thousand blocks or two across, each within the game's own search of
 * every place of its zone, the zone's own biome holding most of it; and the game's {@code /locate} finding every biome
 * of either world from its middle, however far it lies, through J's Core's search of the whole world.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BiomeZonesGameTests {

    private static final String ARENA = "empty";
    private static final String MARS_BATCH = "jsspace_mars";
    private static final String MOON_BATCH = "jsspace_moon";
    private static final int BIOME_Y = 64;
    /* The game's own biome search, as /locate runs it: its reach and its steps. */
    private static final int SEARCH = 6400;
    private static final int STEP = 32;
    private static final int VERTICAL_STEP = 64;
    /* How sure a map must be to put a place in a zone, and the grid a zone's places are looked for on, in degrees. */
    private static final double SURE = 0.98;
    private static final double GRID = 2.5;
    private static final double LATITUDES = 80;
    /* How many places of each zone are read, spread across it. */
    private static final int PLACES = 3;
    /* The square read round a zone's place for its shares: samples on a side, and blocks apart. */
    private static final int SIDE = 48;
    private static final int SPACING = 192;
    /*
     * The zone's own biome's share of it at least, each patch biome's at least and at most, and how much of the
     * squares may lie in another zone.
     */
    private static final double LEAST_OWN = 0.4;
    private static final double LEAST_PATCH = 0.08;
    private static final double MOST_PATCH = 0.3;
    private static final double MOST_ELSEWHERE = 0.2;
    /* Ticks between the places or biomes read, so that no tick reads too much. */
    private static final int PACE = 2;
    private static final int TIMEOUT = 200;

    private BiomeZonesGameTests() {
    }

    @GameTest(template = ARENA, batch = MARS_BATCH, timeoutTicks = TIMEOUT)
    public static void patches_ofMarsZonesLieWithinTheGamesSearchOfThem(final GameTestHelper helper) {
        patchesWithinSearch(helper, mars(helper), marsZones());
    }

    @GameTest(template = ARENA, batch = MOON_BATCH, timeoutTicks = TIMEOUT)
    public static void patches_ofTheMoonsZonesLieWithinTheGamesSearchOfThem(final GameTestHelper helper) {
        patchesWithinSearch(helper, moon(helper), moonZones());
    }

    @GameTest(template = ARENA, batch = MARS_BATCH, timeoutTicks = TIMEOUT)
    public static void zones_ofMarsStayMostlyTheirOwnBiome(final GameTestHelper helper) {
        shares(helper, mars(helper), marsZones());
    }

    @GameTest(template = ARENA, batch = MOON_BATCH, timeoutTicks = TIMEOUT)
    public static void zones_ofTheMoonStayMostlyTheirOwnBiome(final GameTestHelper helper) {
        shares(helper, moon(helper), moonZones());
    }

    @GameTest(template = ARENA, batch = MARS_BATCH, timeoutTicks = TIMEOUT)
    public static void locate_findsEveryBiomeOfMarsFromItsMiddle(final GameTestHelper helper) {
        locateEvery(helper, mars(helper));
    }

    @GameTest(template = ARENA, batch = MOON_BATCH, timeoutTicks = TIMEOUT)
    public static void locate_findsEveryBiomeOfTheMoonFromItsMiddle(final GameTestHelper helper) {
        locateEvery(helper, moon(helper));
    }

    /* From each of a few places of every zone, the game's own search finds each of the zone's patch biomes. */
    private static void patchesWithinSearch(final GameTestHelper helper, final ServerLevel level,
                                            final List<Zone> zones) {
        int tick = 1;
        for (final Zone zone : zones) {
            for (final BlockPos place : places(helper, level, zone)) {
                helper.runAfterDelay(tick, () -> {
                    for (final ResourceKey<Biome> patch : zone.patches()) {
                        final Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                                biome -> biome.is(patch), place, SEARCH, STEP, VERTICAL_STEP);
                        helper.assertTrue(found != null, "from " + place.toShortString() + " in " + zone.name()
                                + ", a patch of " + patch.location() + " lies within " + SEARCH + " blocks");
                    }
                });
                tick += PACE;
            }
        }
        helper.runAfterDelay(tick, helper::succeed);
    }

    /*
     * Round a few places of every zone, the zone's own biome holds the most of the ground, each of its patch biomes a
     * share of it, and little of it is another zone's.
     */
    private static void shares(final GameTestHelper helper, final ServerLevel level, final List<Zone> zones) {
        int tick = 1;
        for (final Zone zone : zones) {
            final Map<ResourceKey<Biome>, AtomicInteger> counts = new HashMap<>();
            final AtomicInteger read = new AtomicInteger();
            for (final BlockPos place : places(helper, level, zone)) {
                helper.runAfterDelay(tick, () -> {
                    for (int i = 0; i < SIDE; i++) {
                        for (int j = 0; j < SIDE; j++) {
                            final Holder<Biome> biome = biomeAt(level, place.getX() + (i - SIDE / 2) * SPACING,
                                    place.getZ() + (j - SIDE / 2) * SPACING);
                            counts.computeIfAbsent(biome.unwrapKey().orElseThrow(), key -> new AtomicInteger())
                                    .incrementAndGet();
                            read.incrementAndGet();
                        }
                    }
                });
                tick += PACE;
            }
            helper.runAfterDelay(tick, () -> {
                final double total = read.get();
                final double own = share(counts, zone.own(), total);
                helper.assertTrue(own >= LEAST_OWN, zone.name() + " is mostly its own biome: "
                        + shares(counts, total));
                double inZone = own;
                for (final ResourceKey<Biome> patch : zone.patches()) {
                    final double patchShare = share(counts, patch, total);
                    inZone += patchShare;
                    helper.assertTrue(patchShare >= LEAST_PATCH && patchShare <= MOST_PATCH && patchShare < own,
                            "in " + zone.name() + ", " + patch.location() + " is a patch of a share of it, less"
                                    + " than its own biome's: " + shares(counts, total));
                }
                helper.assertTrue(1 - inZone <= MOST_ELSEWHERE, "and little of the ground round " + zone.name()
                        + "'s places is another zone's: " + shares(counts, total));
            });
            tick += PACE;
        }
        helper.runAfterDelay(tick, helper::succeed);
    }

    /*
     * The game's /locate, run from the world's middle, finds each of its biomes, the ones farther than the game's own
     * search by the whole world's; and some lie that far.
     */
    private static void locateEvery(final GameTestHelper helper, final ServerLevel level) {
        final BlockPos middle = new BlockPos(0, BIOME_Y, 0);
        final List<Holder<Biome>> biomes = new ArrayList<>(level.getChunkSource().getGenerator().getBiomeSource()
                .possibleBiomes());
        final AtomicInteger far = new AtomicInteger();
        int tick = 1;
        for (final Holder<Biome> biome : biomes) {
            final ResourceKey<Biome> key = biome.unwrapKey().orElseThrow();
            helper.runAfterDelay(tick, () -> {
                final boolean[] located = new boolean[1];
                final CommandSourceStack source = level.getServer().createCommandSourceStack()
                        .withSource(CommandSource.NULL).withLevel(level).withPosition(Vec3.atBottomCenterOf(middle))
                        .withCallback((success, result) -> located[0] = success);
                level.getServer().getCommands().performPrefixedCommand(source, "locate biome " + key.location());
                helper.assertTrue(located[0], "/locate biome finds " + key.location() + " from the middle of "
                        + level.dimension().location());
                if (level.findClosestBiome3d(found -> found.is(key), middle, SEARCH, STEP, VERTICAL_STEP) == null) {
                    far.incrementAndGet();
                    final Pair<BlockPos, Holder<Biome>> whole = BiomeSearch.wholeWorld(level, found -> found.is(key),
                            middle);
                    helper.assertTrue(whole != null, "the whole world's search finds " + key.location());
                    final Holder<Biome> there = biomeAt(level, whole.getFirst().getX(), whole.getFirst().getZ());
                    helper.assertTrue(whole.getSecond().is(key) && there.is(key), "the whole world's search puts "
                            + key.location() + " where it lies, at " + whole.getFirst().toShortString() + ": "
                            + whole.getSecond().unwrapKey().orElseThrow().location() + ", read "
                            + there.unwrapKey().orElseThrow().location());
                }
            });
            tick += PACE;
        }
        helper.runAfterDelay(tick, () -> {
            helper.assertTrue(far.get() > 0, "some biome of " + level.dimension().location() + " lies past the"
                    + " game's own search of its middle");
            helper.succeed();
        });
    }

    /* A few places of the zone, spread across the world, as its maps lay it. */
    private static List<BlockPos> places(final GameTestHelper helper, final ServerLevel level, final Zone zone) {
        final List<double[]> all = new ArrayList<>();
        for (double lat = -LATITUDES; lat <= LATITUDES; lat += GRID) {
            for (double lon = -180; lon < 180; lon += GRID) {
                if (zone.where().holds(lon, lat)) {
                    all.add(new double[] {lon, lat});
                }
            }
        }
        helper.assertTrue(all.size() >= PLACES, zone.name() + " lies somewhere its maps are sure of");
        final double perDegree = perDegree(level);
        final List<BlockPos> places = new ArrayList<>();
        for (int i = 0; i < PLACES; i++) {
            final double[] at = all.get(i * (all.size() - 1) / (PLACES - 1));
            places.add(new BlockPos((int) Math.round(at[0] * perDegree), BIOME_Y,
                    (int) Math.round(-at[1] * perDegree)));
        }
        return places;
    }

    /* Mars' zones, by its maps of the north, its dark regions, its canyons and its caps. */
    private static List<Zone> marsZones() {
        final RoundMap north = map("mars/north");
        final RoundMap dark = map("mars/dark");
        final RoundMap canyon = map("mars/canyon");
        final RoundMap polar = map("mars/polar");
        final IPlace open = (lon, lat) -> dark.at(lon, lat) < 1 - SURE && canyon.at(lon, lat) < 1 - SURE
                && polar.at(lon, lat) < 1 - SURE;
        final IPlace dunes = (lon, lat) -> dark.at(lon, lat) > SURE && canyon.at(lon, lat) < 1 - SURE
                && polar.at(lon, lat) < 1 - SURE;
        return List.of(
                new Zone("the south", (lon, lat) -> open.holds(lon, lat) && north.at(lon, lat) < 1 - SURE,
                        MarsBiomes.SOUTHERN_HIGHLANDS.key(), List.of(MarsBiomes.OXIDE_PLAINS.key(),
                        MarsBiomes.LAKEBED.key(), MarsBiomes.DARK_DUNES.key())),
                new Zone("the north", (lon, lat) -> open.holds(lon, lat) && north.at(lon, lat) > SURE,
                        MarsBiomes.NORTHERN_PLAINS.key(), List.of(MarsBiomes.DARK_DUNES.key())),
                new Zone("a dark region of the north", (lon, lat) -> dunes.holds(lon, lat)
                        && north.at(lon, lat) > SURE, MarsBiomes.DARK_DUNES.key(),
                        List.of(MarsBiomes.NORTHERN_PLAINS.key())),
                new Zone("a dark region of the south", (lon, lat) -> dunes.holds(lon, lat)
                        && north.at(lon, lat) < 1 - SURE, MarsBiomes.DARK_DUNES.key(),
                        List.of(MarsBiomes.SOUTHERN_HIGHLANDS.key())),
                new Zone("a canyon", (lon, lat) -> canyon.at(lon, lat) > SURE && dark.at(lon, lat) < 1 - SURE
                        && polar.at(lon, lat) < 1 - SURE, MarsBiomes.CHAOS_TERRAIN.key(),
                        List.of(MarsBiomes.LAKEBED.key())));
    }

    /* The Moon's zones with patches, by its maps of the maria, its young craters and its poles. */
    private static List<Zone> moonZones() {
        final RoundMap maria = map("moon/maria");
        final RoundMap young = map("moon/young");
        final RoundMap polar = map("moon/polar");
        final IPlace plain = (lon, lat) -> young.at(lon, lat) < 1 - SURE && polar.at(lon, lat) < 1 - SURE;
        return List.of(
                new Zone("the maria", (lon, lat) -> plain.holds(lon, lat) && maria.at(lon, lat) > SURE,
                        MoonBiomes.MARE.key(), List.of(MoonBiomes.RILLE.key(), MoonBiomes.LAVA_TUBE_SKYLIGHT.key(),
                        MoonBiomes.YOUNG_CRATER.key())),
                new Zone("the highlands", (lon, lat) -> plain.holds(lon, lat) && maria.at(lon, lat) < 1 - SURE,
                        MoonBiomes.HIGHLANDS.key(), List.of(MoonBiomes.YOUNG_CRATER.key())));
    }

    private static double share(final Map<ResourceKey<Biome>, AtomicInteger> counts, final ResourceKey<Biome> biome,
                                final double total) {
        final AtomicInteger count = counts.get(biome);
        return count == null ? 0 : count.get() / total;
    }

    private static String shares(final Map<ResourceKey<Biome>, AtomicInteger> counts, final double total) {
        final StringBuilder out = new StringBuilder();
        counts.forEach((biome, count) -> out.append(biome.location().getPath()).append(' ')
                .append(Math.round(count.get() * 1000 / total) / 10.0).append("% "));
        return out.toString().trim();
    }

    private static Holder<Biome> biomeAt(final ServerLevel level, final int x, final int z) {
        return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                QuartPos.fromBlock(BIOME_Y), QuartPos.fromBlock(z), level.getChunkSource().randomState().sampler());
    }

    private static double perDegree(final ServerLevel level) {
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(level.dimension()).orElseThrow();
        return (bounds.maxX() - bounds.minX()) / 360;
    }

    private static RoundMap map(final String which) {
        return RoundMaps.get(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, which)).orElseThrow();
    }

    /* Mars and the Moon, made from their dimensions' files under their own names: the test server makes neither. */
    private static ServerLevel mars(final GameTestHelper helper) {
        final ResourceKey<Level> key = MarsWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }

    private static ServerLevel moon(final GameTestHelper helper) {
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }

    /* Whether a place, by its longitude and latitude, is in a zone. */
    @FunctionalInterface
    private interface IPlace {

        boolean holds(double lon, double lat);
    }

    /*
     * A zone of a world: where its maps lay it, the biome it is its own, and the biomes that lie in patches inside it.
     */
    private record Zone(String name, IPlace where, ResourceKey<Biome> own, List<ResourceKey<Biome>> patches) {
    }
}
