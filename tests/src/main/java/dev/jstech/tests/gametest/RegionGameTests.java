/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.serialization.Codec;
import dev.jstech.core.region.Box;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.region.ChunkLoadingBalance;
import dev.jstech.core.region.RegionIndex;
import dev.jstech.tests.JsTests;
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
 * The Core's world by where things are: a region index kept in a dimension, and chunks kept loaded for an owner, up
 * to the owner's limit and no further, and let go of again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RegionGameTests {

    private static final String ARENA = "empty";
    private static final RegionIndex<String> NOTES = RegionIndex.declare(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "region_notes"), Codec.STRING);
    /** Far from where the tests run, so the chunks it loads are not loaded for another reason. */
    private static final int FAR = 60_000;

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
            helper.assertTrue(ChunkLoaders.load(level, source, owner, first, true)
                    == ChunkLoaders.LoadOutcome.LOADED, "the first chunk is loaded");
            helper.assertTrue(ChunkLoaders.load(level, source, owner, first, true)
                    == ChunkLoaders.LoadOutcome.ALREADY, "asking again changes nothing");
            ChunkLoaders.load(level, source, owner, second, true);
            helper.assertTrue(ChunkLoaders.load(level, source, owner, third, true)
                    == ChunkLoaders.LoadOutcome.LIMIT_REACHED, "a third goes past the owner's limit of two");
            helper.assertTrue(ChunkLoaders.loadedBy(level.getServer(), owner) == 2, "the owner keeps two");
            helper.succeedWhen(() -> {
                helper.assertTrue(level.getChunkSource().hasChunk(first.x, first.z), "the chunk has loaded");
                ChunkLoaders.releaseAll(level, source);
                helper.assertTrue(ChunkLoaders.loadedBy(level.getServer(), owner) == 0,
                        "everything is let go of");
                ChunkLoadingBalance.setLimit(limit);
            });
        } catch (final RuntimeException failed) {
            ChunkLoaders.releaseAll(level, source);
            ChunkLoadingBalance.setLimit(limit);
            throw failed;
        }
    }
}
