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
import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The caves of the Moon and Mars, each kind where it lies: the landform nearest a real place that the world lays
 * there, its chunks made whole, a good volume of rock hollowed and some of it open to the sky. The Moon's lava tubes
 * under the maria and their skylights, the pits of Mare Tranquillitatis, its caves and its highlands' pits; Mars' lava
 * tubes in Tharsis, the grottos of Valles Marineris, the tunnels in the water ice of its north cap, its caves and its
 * pits.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceCavesGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_caves";
    /* How many of a landform's cells either way of a place it is looked for in, and how many of the nearest tried. */
    private static final int CELLS = 8;
    private static final int TRIES = 4;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /* The chunks made round a landform's middle, either way of its own. */
    private static final int AROUND = 2;
    /* The least a region's cave or tube hollows round its middle, and a pit. */
    private static final int LEAST_CAVE = 300;
    private static final int LEAST_PIT = 80;
    /* How many blocks of its roof an ice cave has in the cap's water ice, at the least. */
    private static final int LEAST_ICE_ROOF = 20;
    /* The real places: the Marius Hills' pit, Mare Tranquillitatis' pit and Apollo 16's highlands on the Moon. */
    private static final double[] MARIUS_HILLS = {-56.8, 14.1};
    private static final double[] TRANQUILLITATIS_PIT = {33.2, 8.3};
    private static final double[] DESCARTES = {15.5, -9.0};
    /* Arsia Mons' flank, Valles Marineris' floor among the chaos' blocks, the north cap and Jezero crater on Mars. */
    private static final double[] ARSIA_MONS = {-120.5, -9.0};
    private static final double[] VALLES = {-75, -10};
    private static final double[] NORTH_CAP = {0, 86};
    private static final double[] JEZERO = {77.5, 18.4};

    private SpaceCavesGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 2400)
    public static void moon_tubesCavesAndPitsAreHollowAndOpenToTheSky(final GameTestHelper helper) {
        final ServerLevel moon = world(helper, MoonWorld.world());
        helper.runAfterDelay(1, () -> hollowAndOpen(helper, moon, MoonWorld.LAVA_TUBES, MARIUS_HILLS, LEAST_CAVE));
        helper.runAfterDelay(2, () -> hollowAndOpen(helper, moon, MoonWorld.MARIA_PITS, TRANQUILLITATIS_PIT,
                LEAST_PIT));
        helper.runAfterDelay(3, () -> hollowAndOpen(helper, moon, MoonWorld.CAVES, DESCARTES, LEAST_CAVE));
        helper.runAfterDelay(4, () -> {
            hollowAndOpen(helper, moon, MoonWorld.HIGHLAND_PITS, DESCARTES, LEAST_PIT);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 2400)
    public static void mars_tubesGrottosIceCavesCavesAndPitsAreHollowAndOpenToTheSky(final GameTestHelper helper) {
        final ServerLevel mars = world(helper, MarsWorld.world());
        helper.runAfterDelay(1, () -> hollowAndOpen(helper, mars, MarsWorld.LAVA_TUBES, ARSIA_MONS, LEAST_CAVE));
        helper.runAfterDelay(2, () -> hollowAndOpen(helper, mars, MarsWorld.CANYON_GROTTOS, VALLES, LEAST_PIT));
        helper.runAfterDelay(3, () -> {
            final LandformField.Instance one = hollowAndOpen(helper, mars, MarsWorld.ICE_CAVES, NORTH_CAP,
                    LEAST_CAVE);
            final int roofed = iceOver(mars, one);
            helper.assertTrue(roofed >= LEAST_ICE_ROOF, "the ice cave winds through the cap's water ice, its roof"
                    + " of it: " + roofed + " blocks of roof");
        });
        helper.runAfterDelay(4, () -> hollowAndOpen(helper, mars, MarsWorld.CAVES, JEZERO, LEAST_CAVE));
        helper.runAfterDelay(5, () -> {
            hollowAndOpen(helper, mars, MarsWorld.PITS, JEZERO, LEAST_PIT);
            helper.succeed();
        });
    }

    /*
     * Finds the landforms of {@code spec} nearest {@code at}, makes their chunks, and asserts one of them hollowed at
     * least {@code least} blocks of rock round its middle, some of them open to the sky, over them or beside them.
     */
    private static LandformField.Instance hollowAndOpen(final GameTestHelper helper, final ServerLevel world,
                                                        final Landforms.LandformSpec spec, final double[] at,
                                                        final int least) {
        final StringBuilder seen = new StringBuilder();
        for (final LandformField.Instance one : landforms(world, spec, at)) {
            int hollow = 0;
            int open = 0;
            for (final LevelChunk chunk : chunksRound(world, one)) {
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                    for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                        for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight(); y++) {
                            if (chunk.getBlockState(new BlockPos(x, y, z)).is(Blocks.CAVE_AIR)) {
                                hollow++;
                                if (open(world, x, y, z)) {
                                    open++;
                                }
                            }
                        }
                    }
                }
            }
            if (hollow >= least && open > 0) {
                return one;
            }
            seen.append(one.x()).append(", ").append(one.z()).append(": ").append(hollow).append(" hollow, ")
                    .append(open).append(" open; ");
        }
        throw new GameTestAssertException(spec.id() + " near " + at[0] + ", " + at[1] + " hollows at least " + least
                + " blocks of rock, some open to the sky: " + seen);
    }

    /* Whether a block of cave air is open to the sky, over it or beside it, through a hollow open over it. */
    private static boolean open(final ServerLevel world, final int x, final int y, final int z) {
        if (free(world, x, z) <= y) {
            return true;
        }
        for (final int[] side : SIDES) {
            if (free(world, x + side[0], z + side[1]) <= y
                    && world.getBlockState(new BlockPos(x + side[0], y, z + side[1])).isAir()) {
                return true;
            }
        }
        return false;
    }

    /* The first free block over a column, its chunk made first. */
    private static int free(final ServerLevel world, final int x, final int z) {
        return world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(
                Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
    }

    /* How many blocks of the cap's water ice lie right over the cave air round a cave's middle. */
    private static int iceOver(final ServerLevel world, final LandformField.Instance one) {
        int roofed = 0;
        for (final LevelChunk chunk : chunksRound(world, one)) {
            for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                    for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight() - 1; y++) {
                        if (chunk.getBlockState(new BlockPos(x, y, z)).is(Blocks.CAVE_AIR) && chunk.getBlockState(
                                new BlockPos(x, y + 1, z)).is(SpaceRocks.WATER_ICE.get())) {
                            roofed++;
                        }
                    }
                }
            }
        }
        return roofed;
    }

    /* The chunks round a landform's middle, made whole. */
    private static List<LevelChunk> chunksRound(final ServerLevel world, final LandformField.Instance one) {
        final int middleX = SectionPos.blockToSectionCoord(one.x());
        final int middleZ = SectionPos.blockToSectionCoord(one.z());
        final List<LevelChunk> chunks = new ArrayList<>();
        for (int cx = middleX - AROUND; cx <= middleX + AROUND; cx++) {
            for (int cz = middleZ - AROUND; cz <= middleZ + AROUND; cz++) {
                chunks.add(world.getChunk(cx, cz));
            }
        }
        return chunks;
    }

    /*
     * The landforms of {@code spec} nearest the place {@code at}, by longitude and latitude, nearest first, that the
     * world lays: their middles in the biomes their feature keeps to, read at the ground as the feature reads them,
     * and in the region of a map it keeps to.
     */
    private static List<LandformField.Instance> landforms(final ServerLevel world, final Landforms.LandformSpec spec,
                                                          final double[] at) {
        final LandformConfig config = (LandformConfig) world.registryAccess().registryOrThrow(
                Registries.CONFIGURED_FEATURE).getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                spec.id())).config();
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(world.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / 360;
        final int px = (int) Math.round(at[0] * perDegree);
        final int pz = (int) Math.round(-at[1] * perDegree);
        final int reach = spec.cell() * CELLS;
        final ChunkGenerator generator = world.getChunkSource().getGenerator();
        final RandomState random = world.getChunkSource().randomState();
        final List<LandformField.Instance> found = config.field().reaching(world.getSeed(), px - reach, pz - reach,
                        px + reach, pz + reach).stream()
                .sorted(Comparator.comparingDouble(each -> each.distance(px, pz)))
                .filter(each -> {
                    final int base = generator.getBaseHeight(each.x(), each.z(), Heightmap.Types.OCEAN_FLOOR_WG,
                            world, random) - 1;
                    return config.biomes().map(biomes -> biomes.contains(generator.getBiomeSource().getNoiseBiome(
                            QuartPos.fromBlock(each.x()), QuartPos.fromBlock(base), QuartPos.fromBlock(each.z()),
                            random.sampler()))).orElse(true) && config.mayLieAt(each.x(), each.z());
                })
                .limit(TRIES)
                .toList();
        if (found.isEmpty()) {
            throw new GameTestAssertException("no " + spec.id() + " near " + px + ", " + pz);
        }
        return found;
    }

    /* A world, made from its dimension's file under its own name: the test server makes only its flat preset's. */
    private static ServerLevel world(final GameTestHelper helper, final SpaceWorld world) {
        final ResourceKey<Level> key = world.dimension();
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }
}
