/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpWords;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * What an agent does over many ticks as the player, walking somewhere, breaking a block, waiting for something: each a
 * task the game's tick moves on, on the game's own thread, until it ends, runs out of time, or the player takes the
 * controls back, after which the keys it held are let go.
 */
public final class PlayTasks {

    private static final List<Running> RUNNING = new CopyOnWriteArrayList<>();

    private PlayTasks() {
    }

    /** Hands the game's bus its ticks, once, from the player's game. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick(Minecraft.getInstance()));
    }

    /** Runs {@code task} for {@code call}, for at most {@code limit} ticks; its result once it ends. */
    public static CompletableFuture<McpResult> run(final McpCall call, final int limit, final ITask task) {
        final Running running = new Running(call, limit, task, new CompletableFuture<>());
        Minecraft.getInstance().execute(() -> RUNNING.add(running));
        return running.done();
    }

    private static void tick(final Minecraft game) {
        for (final Running running : RUNNING) {
            final Optional<McpResult> ended;
            if (running.call().takenBack()) {
                ended = Optional.of(McpResult.failed(McpWords.TAKEN_BY_PLAYER.text()));
            } else if (running.ticks()[0]++ >= running.limit()) {
                ended = Optional.of(McpResult.failed(McpWords.TOOK_TOO_LONG.with(running.limit() / 20)));
            } else {
                Optional<McpResult> stepped;
                try {
                    stepped = running.task().step(game, running.ticks()[0]);
                } catch (final RuntimeException failed) {
                    stepped = Optional.of(McpResult.failed(McpWords.WENT_WRONG.with(String.valueOf(
                            failed.getMessage()))));
                }
                ended = stepped;
            }
            if (ended.isPresent()) {
                running.task().release(game);
                RUNNING.remove(running);
                running.done().complete(ended.get());
            }
        }
    }

    /** A thing done over ticks. */
    public interface ITask {

        /** One tick of it, the {@code tick}th; its result once it is done, none while it goes on. */
        Optional<McpResult> step(Minecraft game, int tick);

        /** Lets go whatever it holds; called once, however it ended. */
        default void release(final Minecraft game) {
        }
    }

    /* A task running, its call, how long it may take, how long it took, and its result to come. */
    private record Running(McpCall call, int limit, ITask task, CompletableFuture<McpResult> done, int[] ticks) {

        Running(final McpCall call, final int limit, final ITask task, final CompletableFuture<McpResult> done) {
            this(call, limit, task, done, new int[1]);
        }
    }
}
