/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonObject;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.studio.TestRecorder;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.WorldTools;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Waiting, as a test waits: for some ticks, or until something is so (a screen opens, a block becomes another, a
 * component shows some words, the chat says something), within a time; and recording what an agent does as a client
 * test, as the studio's Record a test keeps what the player does.
 */
public final class PlayWaiting {

    private static final int MOST_TICKS = 1_200;
    private static final int DEFAULT_TICKS = 200;

    private PlayWaiting() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("wait_ticks", "Waits some ticks of the game, twenty a second.", McpSchema.object()
                        .whole("ticks", "How many.", 1, MOST_TICKS, true).build(), McpPermission.READ,
                        PlayWaiting::ticks),
                new McpTool("wait_until", "Waits until something is so, at most some ticks: a screen whose title or "
                        + "kind holds some words is open, a block is of an id, a component of the screen shows some "
                        + "words, or a line of chat holds them. Give one of them.", McpSchema.object()
                        .words("screen", "Words the screen's title or kind holds.", false)
                        .place("block_place", "Where the block is.", false)
                        .words("block", "The id it is to be.", false)
                        .words("component", "A component's name, on a screen declared in a file.", false)
                        .words("shows", "Words it is to show.", false)
                        .words("chat", "Words a line of chat is to hold.", false)
                        .whole("ticks", "How long to wait at most.", 1, MOST_TICKS, false).build(),
                        McpPermission.READ, PlayWaiting::until),
                new McpTool("recording_start", "Starts recording what the agent and the player do as a client test, "
                        + "as the studio's Record a test does, and closes the screen open.", McpSchema.object()
                        .build(), McpPermission.ACT, call -> Minecraft.getInstance().submit(() -> {
                            TestRecorder.start();
                            final JsonObject out = new JsonObject();
                            out.addProperty("recording", true);
                            return McpResult.json(out);
                        })),
                new McpTool("recording_stop", "Stops recording; with keep, writes what was recorded as a client test "
                        + "of the mod of the first block used, beside its resources, and answers with its file.",
                        McpSchema.object().flag("keep", "Whether to keep it.").build(), McpPermission.ACT,
                        call -> Minecraft.getInstance().submit(() -> stop(call))));
    }

    private static CompletableFuture<McpResult> ticks(final McpCall call) {
        final int ticks = (int) Math.max(1, Math.min(MOST_TICKS, call.whole("ticks").orElse(20L)));
        return PlayTasks.run(call, ticks + 20, (game, tick) -> {
            if (tick < ticks) {
                return Optional.empty();
            }
            final JsonObject out = new JsonObject();
            out.addProperty("waited", ticks);
            return Optional.of(McpResult.json(out));
        });
    }

    private static CompletableFuture<McpResult> until(final McpCall call) {
        final int most = (int) Math.max(1, Math.min(MOST_TICKS, call.whole("ticks").orElse((long) DEFAULT_TICKS)));
        final Optional<String> screen = call.text("screen").map(words -> words.toLowerCase(Locale.ROOT));
        final Optional<BlockPos> place = Play.place(call, "block_place");
        final Optional<String> block = call.text("block");
        final Optional<String> component = call.text("component");
        final Optional<String> shows = call.text("shows");
        final Optional<String> chat = call.text("chat");
        if (screen.isEmpty() && (place.isEmpty() || block.isEmpty()) && (component.isEmpty() || shows.isEmpty())
                && chat.isEmpty()) {
            return WorldTools.done(McpCall.wrong("screen", McpWords.SOME_WORDS.text()));
        }
        final int chatBefore = ChatLines.lines().size();
        return PlayTasks.run(call, most + 1, (game, tick) -> {
            final boolean so = screen.map(words -> game.screen != null && (game.screen.getTitle().getString()
                    .toLowerCase(Locale.ROOT).contains(words) || game.screen.getClass().getSimpleName()
                    .toLowerCase(Locale.ROOT).contains(words))).orElse(true)
                    && place.flatMap(at -> block.map(id -> game.level != null && BuiltInRegistries.BLOCK.getKey(
                            game.level.getBlockState(at).getBlock()).toString().equals(id))).orElse(true)
                    && component.flatMap(name -> shows.map(words -> game.screen instanceof IDataScreen data
                            && data.find(name).map(BuiltNode::component).map(found -> found.narration()
                            .getString().contains(words)).orElse(false))).orElse(true)
                    && chat.map(words -> ChatLines.lines().stream().skip(chatBefore).anyMatch(line -> line.text()
                            .contains(words))).orElse(true);
            if (so) {
                final JsonObject out = new JsonObject();
                out.addProperty("waited", tick);
                return Optional.of(McpResult.json(out));
            }
            return tick >= most ? Optional.of(McpResult.failed(McpWords.TOOK_TOO_LONG.with(most / 20)))
                    : Optional.empty();
        });
    }

    private static McpResult stop(final McpCall call) {
        if (!TestRecorder.recording()) {
            return McpResult.failed(McpTexts.NOT_RECORDING.text());
        }
        TestRecorder.stop();
        final JsonObject out = new JsonObject();
        out.addProperty("steps", TestRecorder.steps().size());
        if (call.flag("keep", false)) {
            final Optional<Path> kept = TestRecorder.keep();
            out.addProperty("file", kept.map(Path::toString).orElse(""));
        }
        return McpResult.json(out);
    }
}
