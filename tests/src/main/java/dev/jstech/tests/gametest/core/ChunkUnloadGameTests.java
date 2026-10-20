/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import java.util.Comparator;
import java.util.function.BooleanSupplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The unloading of a chunk wanted again before it ran ends at once, rather than queueing itself again for as long as
 * the server has time: a chunk let go, its unloading queued with no time to run it, is wanted again at the edge of the
 * land round a chunk beside it, which lends it to its own generation; the server's next time for unloading, all it
 * has, finds the old unloading done in a few turns, and the chunk beside generates. The test drives the chunks' ticks
 * itself, for the time each has, so it runs in a batch of its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChunkUnloadGameTests {

    private static final String ARENA = "empty";
    private static final TicketType<ChunkPos> TICKET = TicketType.create("jstests_chunk_unload",
            Comparator.comparingLong(ChunkPos::toLong));
    /* The turns of unloading the server is given time for; spinning would take every one. */
    private static final int TURNS = 200_000;
    /* How many times the chunks are ticked with no time, so every chunk let go is queued to unload. */
    private static final int QUEUEING = 6;
    /* Far from the yard and from any land the tests load, so the chunk beside is new and must generate. */
    private static final int FAR = 200_000;
    private static final int SPREAD = 600_000;
    private static final int SETTLE = 20;

    private ChunkUnloadGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_chunk_unload", timeoutTicks = 1200)
    public static void unload_ofAChunkWantedAgainWhileItWaitedEndsAtOnce(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerChunkCache chunks = level.getChunkSource();
        final ChunkPos wanted = new ChunkPos(FAR + level.random.nextInt(SPREAD), FAR + level.random.nextInt(SPREAD));
        final ChunkPos beside = new ChunkPos(wanted.x + 1, wanted.z);
        final int[] turns = new int[1];
        chunks.addRegionTicket(TICKET, wanted, 0, wanted);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(chunks.getChunkNow(wanted.x, wanted.z) != null,
                        "the chunk to load"))
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    helper.assertTrue(chunks.getChunkNow(beside.x, beside.z) == null,
                            "the chunk beside is not loaded whole, so it generates when it is wanted");
                    chunks.removeRegionTicket(TICKET, wanted, 0, wanted);
                    for (int i = 0; i < QUEUEING; i++) {
                        chunks.tick(() -> false, false);
                    }
                    chunks.addRegionTicket(TICKET, beside, 0, beside);
                    chunks.tick(() -> false, false);
                    final BooleanSupplier time = () -> ++turns[0] < TURNS;
                    chunks.tick(time, false);
                    helper.assertTrue(turns[0] < TURNS, "the unloading ended with time to spare, not after "
                            + turns[0] + " turns");
                })
                .thenWaitUntil(() -> helper.assertTrue(chunks.getChunkNow(beside.x, beside.z) != null,
                        "the chunk beside to generate and load"))
                .thenExecute(() -> chunks.removeRegionTicket(TICKET, beside, 0, beside))
                .thenSucceed();
    }
}
