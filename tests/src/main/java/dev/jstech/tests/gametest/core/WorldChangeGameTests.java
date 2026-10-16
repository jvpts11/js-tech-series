/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.world.live.LiveChanges;
import dev.jstech.core.worldgen.retro.Retrogen;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldChanges;
import dev.jstech.tests.TestWorldGen;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world changing: generation laid into a chunk made before it, once, and only when its neighbours are loaded; a
 * chunk made with it marked as having had it; blue ice thawing under the sky where it is warm, water gathering in a
 * basin and not where one side is open; and a place's biome changed, with its climate.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldChangeGameTests {

    private static final String ARENA = "empty";
    private static final int AT = 2;
    private static final int LONE = 40;
    private static final double FREEZING = 273.15;

    private WorldChangeGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void retrogen_laysItsWorkIntoOldChunksOnceTheirNeighboursAreLoaded(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy(TestWorldChanges.RETRO_DIMENSIONS);
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    land.getChunk(AT + dx, AT + dz);
                }
            }
            final LevelChunk chunk = land.getChunk(AT, AT);
            helper.assertTrue(Retrogen.done(chunk).containsAll(List.of(TestWorldChanges.BEACON,
                    TestWorldChanges.OLD_MOSS)), "a chunk made now is marked as having had the work");
            helper.assertTrue(!Retrogen.look(land, chunk), "so it waits for nothing");
            final int x = chunk.getPos().getMiddleBlockX();
            final int z = chunk.getPos().getMiddleBlockZ();
            final BlockPos top = new BlockPos(x, land.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
            helper.assertTrue(!land.getBlockState(top).is(Blocks.LODESTONE), "and has no beacon");
            int mossBefore = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final LevelChunk old = land.getChunk(AT + dx, AT + dz);
                    mossBefore += moss(land, old);
                    Retrogen.forget(old, TestWorldChanges.OLD_MOSS);
                    Retrogen.look(land, old);
                }
            }
            Retrogen.forget(chunk, TestWorldChanges.BEACON);
            helper.assertTrue(Retrogen.look(land, chunk) && Retrogen.waiting(land) >= 1,
                    "a chunk that lacks the work waits for it");
            helper.assertTrue(Retrogen.work(land, Integer.MAX_VALUE) >= 1, "and is generated");
            helper.assertTrue(land.getBlockState(top).is(Blocks.LODESTONE), "the old chunk gets its beacon");
            helper.assertTrue(Retrogen.done(chunk).containsAll(Retrogen.declared()), "and is marked as having had it");
            int mossAfter = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    mossAfter += moss(land, land.getChunk(AT + dx, AT + dz));
                }
            }
            helper.assertTrue(mossAfter > mossBefore, "a feature is laid into old chunks as generation lays it, "
                    + mossBefore + " blocks then " + mossAfter);
            helper.assertTrue(!Retrogen.look(land, chunk) && Retrogen.work(land, Integer.MAX_VALUE) == 0,
                    "the work runs once");
            final LevelChunk lone = land.getChunk(LONE, LONE);
            Retrogen.forget(lone, TestWorldChanges.BEACON);
            Retrogen.look(land, lone);
            Retrogen.work(land, Integer.MAX_VALUE);
            helper.assertTrue(!Retrogen.done(lone).contains(TestWorldChanges.BEACON),
                    "a chunk whose neighbours are not loaded waits for them");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void liveChanges_thawIceAndFillABasinAsTheirConditionsSay(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RandomSource random = RandomSource.create(1);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.BLUE_ICE);
        final BlockPos ice = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(HeatClimate.ambient(level, ice) > FREEZING, "the test world is above freezing");
        helper.assertTrue(LiveChanges.column(level, ice.getX(), ice.getZ(), random), "the ice changes");
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(1, 1, 1));
        basin(helper, 2, false);
        final BlockPos basin = helper.absolutePos(new BlockPos(5, 2, 2));
        helper.assertTrue(LiveChanges.column(level, basin.getX(), basin.getZ(), random), "the basin fills");
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(5, 2, 2));
        basin(helper, 6, true);
        final BlockPos open = helper.absolutePos(new BlockPos(5, 2, 6));
        helper.assertTrue(!LiveChanges.column(level, open.getX(), open.getZ(), random),
                "a basin open on one side stays dry");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 6));
        helper.setBlock(new BlockPos(1, 1, 5), Blocks.STONE);
        final BlockPos stone = helper.absolutePos(new BlockPos(1, 1, 5));
        helper.assertTrue(!LiveChanges.column(level, stone.getX(), stone.getZ(), random),
                "and a block no change names stays as it is");
        helper.assertTrue(LiveChanges.all(level).containsKey(TestWorldChanges.THAW)
                && LiveChanges.all(level).containsKey(TestWorldChanges.BASIN), "the changes are declared");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void setBiome_changesThePlaceAndItsClimate(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy("biome_");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final Holder<Biome> frost = land.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(TestWorldGen.FROST.key());
            final Holder<Biome> steppe = land.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(TestWorldGen.STEPPE.key());
            final BlockPos middle = new ChunkPos(AT, AT).getMiddleBlockPosition(64);
            final BlockPos far = middle.east(160);
            land.getChunk(far);
            final Holder<Biome> farBefore = land.getBiome(far);
            final int changed = LiveChanges.setBiome(land, new BoundingBox(middle.getX() - 8, 0, middle.getZ() - 8,
                    middle.getX() + 8, 127, middle.getZ() + 8), steppe);
            helper.assertTrue(changed >= 1 && land.getBiome(middle).is(TestWorldGen.STEPPE.key()),
                    "the place is of the new biome");
            LiveChanges.setBiome(land, new BoundingBox(middle.getX() - 8, 0, middle.getZ() - 8, middle.getX() + 8,
                    127, middle.getZ() + 8), frost);
            helper.assertTrue(land.getBiome(middle).is(TestWorldGen.FROST.key()), "and can change again");
            helper.assertTrue(HeatClimate.ambient(land, middle) < FREEZING, "its climate is the new biome's");
            helper.assertTrue(land.getBiome(far).equals(farBefore), "a place outside keeps its own");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    /* A floor of waxed copper three wide at {@code z}, walled round its middle, the wall open on the west if asked. */
    private static void basin(final GameTestHelper helper, final int z, final boolean open) {
        for (int x = 4; x <= 6; x++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(new BlockPos(x, 1, z + dz), Blocks.WAXED_COPPER_BLOCK);
                final boolean middle = x == 5 && dz == 0;
                final boolean gap = open && x == 4 && dz == 0;
                helper.setBlock(new BlockPos(x, 2, z + dz), middle || gap ? Blocks.AIR : Blocks.STONE);
            }
        }
    }

    private static int moss(final ServerLevel land, final LevelChunk chunk) {
        int count = 0;
        final ChunkPos at = chunk.getPos();
        for (final BlockPos pos : BlockPos.betweenClosed(at.getMinBlockX(), land.getMinBuildHeight(),
                at.getMinBlockZ(), at.getMaxBlockX(), land.getMaxBuildHeight() - 1, at.getMaxBlockZ())) {
            count += chunk.getBlockState(pos).is(Blocks.MOSSY_COBBLESTONE) ? 1 : 0;
        }
        return count;
    }

    private static ResourceKey<Level> copy(final String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                name + UUID.randomUUID().toString().substring(0, 8)));
    }
}
