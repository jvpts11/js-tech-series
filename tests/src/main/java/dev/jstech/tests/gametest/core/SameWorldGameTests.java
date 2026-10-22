/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The same world for the same seed, on any number of threads: the test land, made with every kind of generation the
 * Core has (its terrain and density functions, landforms, rock in layers, seas, veins, reservoirs), is worked out the
 * same at every point whether one thread or eight share the work; and a square of its chunks made in one world all at
 * once, as the game's threads like, is block for block the square made in another world one chunk at a time, in the
 * other order. A world made with C2ME or Distant Horizons is the world made without.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SameWorldGameTests {

    private static final String ARENA = "empty";
    private static final ResourceKey<Level> MANY = key("same_world_many");
    private static final ResourceKey<Level> ONE = key("same_world_one");
    private static final ResourceKey<Level> SAMPLED = key("same_world_sampled");
    /* The points the density is worked out at: a grid this wide and high, so far apart. */
    private static final int ACROSS = 24;
    private static final int UP = 24;
    private static final int APART = 23;
    private static final int[] THREADS = {2, 4, 8};
    /* The square of chunks made twice, far from where anything else is made. */
    private static final int FIRST = 3_000;
    private static final int SIDE = 4;

    private SameWorldGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_same_world", timeoutTicks = 600)
    public static void density_isTheSameOnAnyNumberOfThreads(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, SAMPLED, TestWorldGen.TEST_LAND.id());
        final DensityFunction density = land.getChunkSource().randomState().router().finalDensity();
        RuntimeDimensions.remove(server, SAMPLED);
        final double[] alone = new double[ACROSS * ACROSS * UP];
        for (int i = 0; i < alone.length; i++) {
            alone[i] = at(density, i);
        }
        for (final int threads : THREADS) {
            final double[] shared = new double[alone.length];
            final List<Integer> order = new ArrayList<>(IntStream.range(0, alone.length).boxed().toList());
            Collections.shuffle(order, new Random(threads));
            try (ForkJoinPool pool = new ForkJoinPool(threads)) {
                pool.submit(() -> order.parallelStream().forEach(i -> shared[i] = at(density, i))).join();
            }
            for (int i = 0; i < alone.length; i++) {
                if (Double.doubleToLongBits(alone[i]) != Double.doubleToLongBits(shared[i])) {
                    helper.fail("on " + threads + " threads, the density at point " + i + " is " + shared[i]
                            + ", on one " + alone[i]);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_same_world", timeoutTicks = 2_400)
    public static void chunks_areTheSameWhateverTheOrderTheyAreMadeIn(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerLevel many = RuntimeDimensions.getOrCreate(server, MANY, TestWorldGen.TEST_LAND.id());
        final ServerLevel one = RuntimeDimensions.getOrCreate(server, ONE, TestWorldGen.TEST_LAND.id());
        final List<ChunkPos> square = new ArrayList<>();
        for (int x = FIRST; x < FIRST + SIDE; x++) {
            for (int z = FIRST; z < FIRST + SIDE; z++) {
                square.add(new ChunkPos(x, z));
            }
        }
        // All at once in one world, as the game's threads like to make them.
        final List<CompletableFuture<ChunkResult<ChunkAccess>>> made = new ArrayList<>();
        for (final ChunkPos chunk : square) {
            made.add(many.getChunkSource().getChunkFuture(chunk.x, chunk.z, ChunkStatus.FULL, true));
        }
        // One at a time in the other, a tick each, the last first.
        final List<ChunkPos> backwards = new ArrayList<>(square);
        Collections.reverse(backwards);
        final int[] next = {0};
        helper.onEachTick(() -> {
            if (next[0] < backwards.size()) {
                final ChunkPos chunk = backwards.get(next[0]++);
                one.getChunk(chunk.x, chunk.z);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(next[0] >= backwards.size() && made.stream().allMatch(CompletableFuture::isDone),
                    "the chunks are still being made");
            for (final ChunkPos chunk : square) {
                final ChunkAccess a = many.getChunk(chunk.x, chunk.z);
                final ChunkAccess b = one.getChunk(chunk.x, chunk.z);
                final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
                for (int y = many.getMinBuildHeight(); y < many.getMaxBuildHeight(); y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            at.set(chunk.getMinBlockX() + x, y, chunk.getMinBlockZ() + z);
                            if (Block.getId(a.getBlockState(at)) != Block.getId(b.getBlockState(at))) {
                                helper.fail("at " + at.toShortString() + ", " + a.getBlockState(at)
                                        + " made all at once and " + b.getBlockState(at) + " made one at a time");
                            }
                        }
                    }
                }
            }
            RuntimeDimensions.remove(server, MANY);
            RuntimeDimensions.remove(server, ONE);
        });
    }

    /* The density at the {@code index}th point of the grid. */
    private static double at(final DensityFunction density, final int index) {
        final int x = index % ACROSS;
        final int z = index / ACROSS % ACROSS;
        final int y = index / (ACROSS * ACROSS);
        return density.compute(new DensityFunction.SinglePointContext(x * APART, -64 + y * 16, z * APART));
    }

    private static ResourceKey<Level> key(final String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, name));
    }
}
