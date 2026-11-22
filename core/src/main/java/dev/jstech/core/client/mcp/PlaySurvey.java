/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * What blocks lie round the player, for an agent playing the game: every block of the chunks in a square round the
 * player's, of a mod or whose id holds some words, counted by its id with the first place it was found, the most
 * first. The chunks are read from the world the game runs, made where never made before, as walking there would.
 */
final class PlaySurvey {

    private static final String RADIUS = "radius";
    private static final String MOD = "mod";
    private static final String WORDS = "words";
    private static final String MOST = "most";
    /* How many chunks round the player's own the square reaches at most, and unless asked. */
    private static final int MOST_CHUNKS = 8;
    private static final int SOME_CHUNKS = 2;
    private static final int MOST_KINDS = 500;
    private static final int SOME_KINDS = 50;
    private static final int SECTION = 16;

    private PlaySurvey() {
    }

    /** The tools. */
    static List<McpTool> all() {
        return List.of(new McpTool("blocks_survey", "The blocks of the chunks in a square round the player's chunk, "
                + "of a mod or whose id holds some words, counted by their id, each with the first place it was found "
                + "and how many there are, the most first. Chunks never made before are made as reading them, as "
                + "walking there would: a wide square takes a while. For finding where ores and other blocks lie.",
                McpSchema.object()
                        .whole(RADIUS, "How many chunks round the player's own the square reaches.", 0, MOST_CHUNKS,
                                false)
                        .words(MOD, "Only the blocks of this mod, by its id.", false)
                        .words(WORDS, "Only the blocks whose id holds these words.", false)
                        .whole(MOST, "At most this many kinds of block.", 1, MOST_KINDS, false).build(),
                McpPermission.READ, PlaySurvey::survey));
    }

    private static CompletableFuture<McpResult> survey(final McpCall call) {
        final Minecraft game = Minecraft.getInstance();
        final MinecraftServer server = game.getSingleplayerServer();
        if (server == null || game.player == null) {
            return CompletableFuture.completedFuture(McpResult.failed(McpTexts.SURVEY_OWN_WORLD_ONLY.text()));
        }
        final UUID id = game.player.getUUID();
        final int radius = (int) (long) call.whole(RADIUS).orElse((long) SOME_CHUNKS);
        final Optional<String> mod = call.text(MOD).map(words -> words.toLowerCase(Locale.ROOT));
        final Optional<String> words = call.text(WORDS).map(text -> text.toLowerCase(Locale.ROOT));
        final int most = (int) (long) call.whole(MOST).orElse((long) SOME_KINDS);
        return server.submit(() -> {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                return McpResult.failed(McpTexts.SURVEY_OWN_WORLD_ONLY.text());
            }
            final ServerLevel level = player.serverLevel();
            final int middleX = SectionPos.blockToSectionCoord(player.getBlockX());
            final int middleZ = SectionPos.blockToSectionCoord(player.getBlockZ());
            // Whether each kind of block is one asked about, worked out once a kind, as the same few fill the land.
            final Map<Block, Boolean> asked = new IdentityHashMap<>();
            final Map<Block, Found> found = new IdentityHashMap<>();
            for (int x = middleX - radius; x <= middleX + radius; x++) {
                for (int z = middleZ - radius; z <= middleZ + radius; z++) {
                    count(level.getChunk(x, z), state -> asked.computeIfAbsent(state.getBlock(),
                            block -> asked(block, mod, words)), found);
                }
            }
            final List<Found> kinds = new ArrayList<>(found.values());
            kinds.sort(Comparator.comparingLong(Found::count).reversed());
            final JsonArray blocks = new JsonArray();
            kinds.stream().limit(most).forEach(kind -> blocks.add(kind.told()));
            final JsonObject out = new JsonObject();
            out.addProperty("dimension", level.dimension().location().toString());
            out.addProperty("chunks", (2 * radius + 1) * (2 * radius + 1));
            out.addProperty("kinds", kinds.size());
            out.add("blocks", blocks);
            return McpResult.json(out);
        });
    }

    /* Whether {@code block} is one the agent asked about: not air, of {@code mod}, its id holding {@code words}. */
    private static boolean asked(final Block block, final Optional<String> mod, final Optional<String> words) {
        final ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return !block.defaultBlockState().isAir() && (mod.isEmpty() || key.getNamespace().equals(mod.get()))
                && (words.isEmpty() || key.toString().contains(words.get()));
    }

    /* Counts into {@code found} the blocks of {@code chunk} that {@code asked} takes, passing sections without any. */
    private static void count(final LevelChunk chunk, final Predicate<BlockState> asked,
                              final Map<Block, Found> found) {
        final LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            final LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(asked)) {
                continue;
            }
            final int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < SECTION; y++) {
                for (int z = 0; z < SECTION; z++) {
                    for (int x = 0; x < SECTION; x++) {
                        final BlockState state = section.getBlockState(x, y, z);
                        if (!asked.test(state)) {
                            continue;
                        }
                        Found kind = found.get(state.getBlock());
                        if (kind == null) {
                            kind = new Found(state.getBlock(), chunk.getPos().getBlockAt(x, bottom + y, z));
                            found.put(state.getBlock(), kind);
                        }
                        kind.count++;
                    }
                }
            }
        }
    }

    /* A kind of block found: where first, and how many. */
    private static final class Found {

        private final Block block;
        private final BlockPos first;
        private long count;

        private Found(final Block block, final BlockPos first) {
            this.block = block;
            this.first = first;
        }

        private long count() {
            return count;
        }

        private JsonObject told() {
            final JsonObject one = new JsonObject();
            one.addProperty("id", BuiltInRegistries.BLOCK.getKey(block).toString());
            one.addProperty("count", count);
            one.addProperty("x", first.getX());
            one.addProperty("y", first.getY());
            one.addProperty("z", first.getZ());
            return one;
        }
    }
}
