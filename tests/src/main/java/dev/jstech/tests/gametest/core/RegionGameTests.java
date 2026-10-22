/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.serialization.Codec;
import dev.jstech.core.region.AwayOwners;
import dev.jstech.core.region.Box;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.core.region.RegionIndex;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's world by where things are: a region index kept in a dimension, and chunks kept loaded for an owner and a
 * reason, up to the owner's limit and the server's budget and no further, let go of while the owner is away when the
 * server's rule says, and let go of again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RegionGameTests {

    private static final String ARENA = "empty";
    private static final RegionIndex<String> NOTES = RegionIndex.declare(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "region_notes"), Codec.STRING);
    /** Far from where the tests run, so the chunks it loads are not loaded for another reason. */
    private static final int FAR = 60_000;
    /* How far from the others the test of an owner away keeps its chunks, so it never shares a source. */
    private static final int AWAY_APART = 512;
    private static final TextKey REASON = TextKey.of("jstests.chunks.reason", "A test keeps it loaded");
    private static final TextKey OTHER_REASON = TextKey.of("jstests.chunks.other_reason", "A test keeps it for more");

    private RegionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void regionIndex_findsWhatIsKeptNearAPlace(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos here = helper.absolutePos(new BlockPos(1, 2, 1));
        final String key = "note-" + here.asLong();
        NOTES.put(level, key, Box.around(here.getX(), here.getY(), here.getZ(), 2), "kept here");
        helper.assertTrue("kept here".equals(NOTES.nearest(level, here.east(6), 8).value()),
                "the note is found from six blocks away");
        helper.assertTrue(NOTES.containing(level, here.above()).size() == 1, "the box holds the block above");
        NOTES.remove(level, key);
        helper.assertTrue(NOTES.get(level, key) == null, "and is gone once removed");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chunkLoaders_letGoOfAnOwnerAwayAndLoadAgainAsTheyComeBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID owner = UUID.randomUUID();
        final BlockPos source = new BlockPos(FAR + AWAY_APART, 64, FAR);
        final ChunkPos first = new ChunkPos(source);
        final ChunkPos second = new ChunkPos(first.x + 1, first.z);
        try {
            helper.assertTrue(ChunkLoaders.load(level, source, owner, REASON, first, true)
                    == ChunkLoaders.LoadOutcome.LOADED, "the chunk is loaded for an owner who is not here");
            AwayOwners.left(level.getServer(), owner);
            helper.assertTrue(!AwayOwners.letGo(level.getServer(), owner) && !AwayOwners.letGoOf(owner),
                    "by default, chunks stay loaded while their owner is away");
            ChunkLoadingBalance.setKeepWhileAway(false);
            helper.assertTrue(AwayOwners.letGo(level.getServer(), owner) && AwayOwners.letGoOf(owner),
                    "a server that keeps nothing while the owner is away lets them go");
            ChunkLoadingBalance.setKeepWhileAway(true);
            helper.assertTrue(ChunkLoaders.load(level, source, owner, REASON, second, true)
                    == ChunkLoaders.LoadOutcome.OWNER_AWAY && ChunkLoaders.loadedBy(level.getServer(), owner) == 2,
                    "what the machine asks for meanwhile is noted, waiting for the owner");
            AwayOwners.back(level.getServer(), owner);
            helper.assertTrue(!AwayOwners.letGoOf(owner), "the owner is back");
        } catch (final RuntimeException failed) {
            ChunkLoadingBalance.setKeepWhileAway(true);
            AwayOwners.back(level.getServer(), owner);
            ChunkLoaders.releaseAll(level, source);
            throw failed;
        }
        helper.runAfterDelay(150, () -> ChunkLoaders.releaseAll(level, source));
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getChunkSource().hasChunk(second.x, second.z),
                    "and both chunks are loaded again, the one asked for meanwhile too");
            ChunkLoaders.releaseAll(level, source);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void chunkLoaders_keepChunksForAnOwnerUpToTheLimit(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID owner = UUID.randomUUID();
        final BlockPos source = new BlockPos(FAR, 64, FAR);
        final int limit = ChunkLoadingBalance.limit();
        ChunkLoadingBalance.setLimit(2);
        try {
            final ChunkPos first = new ChunkPos(source);
            final ChunkPos second = new ChunkPos(first.x + 1, first.z);
            final ChunkPos third = new ChunkPos(first.x + 2, first.z);
            helper.assertTrue(ChunkLoaders.load(level, source, owner, REASON, first, true)
                    == ChunkLoaders.LoadOutcome.LOADED, "the first chunk is loaded");
            helper.assertTrue(ChunkLoaders.load(level, source, owner, REASON, first, true)
                    == ChunkLoaders.LoadOutcome.ALREADY, "asking again changes nothing");
            ChunkLoaders.load(level, source, owner, REASON, second, true);
            helper.assertTrue(ChunkLoaders.load(level, source, owner, REASON, third, true)
                    == ChunkLoaders.LoadOutcome.LIMIT_REACHED, "a third goes past the owner's limit of two");
            helper.assertTrue(ChunkLoaders.loadedBy(level.getServer(), owner) == 2, "the owner keeps two");
            final Runnable restore = () -> {
                ChunkLoaders.releaseAll(level, source);
                ChunkLoadingBalance.setLimit(limit);
            };
            // Safety net: the wait below retries until the test times out, and a timeout would otherwise leave
            // the lowered limit and the held chunks behind for every later test. The restore is idempotent.
            helper.runAfterDelay(150, restore);
            helper.succeedWhen(() -> {
                helper.assertTrue(level.getChunkSource().hasChunk(first.x, first.z), "the chunk has loaded");
                restore.run();
                helper.assertTrue(ChunkLoaders.loadedBy(level.getServer(), owner) == 0,
                        "everything is let go of");
            });
        } catch (final RuntimeException failed) {
            ChunkLoaders.releaseAll(level, source);
            ChunkLoadingBalance.setLimit(limit);
            throw failed;
        }
    }

    @GameTest(template = ARENA)
    public static void chunkLoaders_sayWhyEachSourceKeepsItsChunks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos source = new BlockPos(FAR + 32, 64, FAR + 32);
        final ChunkPos chunk = new ChunkPos(source);
        final UUID owner = UUID.randomUUID();
        try {
            ChunkLoaders.load(level, source, owner, REASON, chunk, false);
            helper.assertTrue(REASON.equals(ChunkLoaders.reasons(level).get(source)),
                    "the source keeps its chunk for the reason it gave");
            helper.assertTrue(ChunkLoaders.load(level, source, owner, OTHER_REASON, chunk, false)
                    == ChunkLoaders.LoadOutcome.ALREADY, "asking again for another reason loads nothing new");
            helper.assertTrue(OTHER_REASON.equals(ChunkLoaders.reasons(level).get(source)),
                    "but the source now says the new reason");
        } finally {
            ChunkLoaders.releaseAll(level, source);
        }
        helper.assertTrue(!ChunkLoaders.reasons(level).containsKey(source), "a source let go of says nothing");
        helper.succeed();
    }

    /* A batch of its own: the server's budget is every owner's, and lowering it would starve the tests beside it. */
    @GameTest(template = ARENA, batch = "jscore_chunk_budget")
    public static void chunkLoaders_stopAtTheServersBudget(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos source = new BlockPos(FAR + 64, 64, FAR);
        final ChunkPos chunk = new ChunkPos(source);
        final int budget = ChunkLoadingBalance.serverBudget();
        try {
            ChunkLoadingBalance.setServerBudget(ChunkLoaders.loadedOnServer(level.getServer()));
            helper.assertTrue(ChunkLoaders.load(level, source, UUID.randomUUID(), REASON, chunk, true)
                    == ChunkLoaders.LoadOutcome.SERVER_FULL, "an owner with room is refused by a full server");
            helper.assertTrue(ChunkLoaders.loadedFrom(level, source).isEmpty(), "and nothing is kept");
            ChunkLoadingBalance.setServerBudget(ChunkLoaders.loadedOnServer(level.getServer()) + 1);
            helper.assertTrue(ChunkLoaders.load(level, source, UUID.randomUUID(), REASON, chunk, true)
                    == ChunkLoaders.LoadOutcome.LOADED, "one more in the budget lets it in");
            helper.assertTrue(ChunkLoaders.kept(level).getOrDefault(source, Set.of()).contains(chunk),
                    "the server knows which source keeps it");
        } finally {
            ChunkLoaders.releaseAll(level, source);
            ChunkLoadingBalance.setServerBudget(budget);
        }
        helper.succeed();
    }
}
