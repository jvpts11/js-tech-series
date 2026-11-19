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
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The land a moving hold leaves is let go, not kept for good: the game serves the chunks' waiting tasks nearest first
 * and a chunk no longer loaded last of all, so while anything kept moving into new land, the tasks of the land it left
 * never ran, each holding the chunks round it, and the chunks loaded grew without end, every tick slower for them. A
 * hold is moved over a flat land as a ship waiting on its way moves, a step whenever the place ahead goes: the chunks
 * loaded stay round it. Flat land generates fast, so the hold moves fast and new tasks never stop coming.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChunkQueueGameTests {

    private static final String ARENA = "empty";
    /* A ship's hold on its way: going a little after it was last taken, its chunks ticking. */
    private static final TicketType<ChunkPos> PASSING = TicketType.create("jstests_chunk_queue",
            Comparator.comparingLong(ChunkPos::toLong), 40);
    private static final int REACH = 3;
    /* How far ahead of the hold the place it waits on stands, how far it moves each time, and how often it is held. */
    private static final double AHEAD = 6;
    private static final double STEP = 6;
    private static final double HELD_EVERY = 8;
    /*
     * How far the hold goes, in blocks: far enough for the land left behind, kept, to be twice the land round it; and
     * how many chunks may be loaded once it is there. A slow machine makes the land more slowly: the hold takes more
     * ticks, as many as it needs within the test's time.
     */
    private static final double FAR = 3_000;
    private static final int MOST_LOADED = 4_000;
    /* How many of its chunks may be left once the hold stops: none it held, its land empty as the test leaves it. */
    private static final int LEFT_LOADED = 50;
    private static final int TIME = 30_000;
    private static final int Y = 64;
    private static final double Z = 400_000;

    private ChunkQueueGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_chunk_queue", timeoutTicks = TIME)
    public static void movingHold_leavesNoLandWaitingForItsTasks(final GameTestHelper helper) {
        final ServerLevel land = RuntimeDimensions.getOrCreate(helper.getLevel().getServer(),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                        "chunk_queue_land")), TestWorldGen.HALF_G_LAND.id());
        final double[] x = {0};
        helper.startSequence().thenWaitUntil(() -> {
            // Held each tick, it moves on whenever the place ahead goes, till it is far enough.
            for (final double along : new double[] {0, HELD_EVERY}) {
                final ChunkPos at = new ChunkPos(BlockPos.containing(x[0] + along, Y, Z));
                land.getChunkSource().addRegionTicket(PASSING, at, REACH, at, true);
            }
            if (land.isPositionEntityTicking(BlockPos.containing(x[0] + AHEAD, Y, Z))) {
                x[0] += STEP;
            }
            helper.assertTrue(x[0] >= FAR, "the hold moves on into new land: " + x[0] + " blocks");
        }).thenExecute(() -> helper.assertTrue(land.getChunkSource().getLoadedChunksCount() <= MOST_LOADED,
                "the land it left is let go: " + land.getChunkSource().getLoadedChunksCount() + " chunks loaded, the"
                        + " hold " + x[0] + " blocks on")).thenWaitUntil(() -> helper.assertTrue(
                land.getChunkSource().getLoadedChunksCount() <= LEFT_LOADED, "and once it stops, all the land it held"
                        + " goes: " + land.getChunkSource().getLoadedChunksCount() + " chunks loaded")).thenSucceed();
    }
}
