/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * A game that waits for its agent: held, its world stands frozen between the agent's calls, by the game's own tick
 * freeze, and runs only while a call runs, reading or acting. Nothing goes on while the agent thinks, a ship flying
 * on or a machine working, and a call that waits or acts for some ticks has the world run them with it, which its
 * answer tells ({@link McpChanges#TICKS_TAKEN}). Let go, the world runs as it did; it is let go as the MCP server
 * stops, and as the player takes the controls back.
 */
public final class McpHold {

    /** The tool's one argument. */
    public static final String ON = "on";
    /*
     * How many calls run now, over every client; the server held, or none; and whether its world stood frozen before
     * it was held, as it is left when let go.
     */
    private static final AtomicInteger RUNNING = new AtomicInteger();
    private static volatile @Nullable MinecraftServer held;
    private static volatile boolean frozenBefore;

    private McpHold() {
    }

    /** The tool that holds or lets go of the game {@code server} gives, none where this game runs no world. */
    public static McpTool tool(final Supplier<@Nullable MinecraftServer> server) {
        return new McpTool("game_hold", "Has the game wait for the agent, or run on its own again: held, the world "
                + "stands frozen, by the game's own tick freeze, between calls, and runs only while a call runs, "
                + "so nothing goes on while the agent thinks and a call that waits or acts for some ticks has the "
                + "world run them with it (game_ticks_taken tells how many). It is let go as the MCP server stops "
                + "or the player presses a key.", McpSchema.object().flag(ON, "Whether to hold it: true holds, false "
                + "lets it run.").build(), McpPermission.ACT, call -> {
                    final MinecraftServer world = server.get();
                    if (world == null) {
                        return CompletableFuture.completedFuture(McpResult.failed(McpWords.HOLD_OWN_WORLD_ONLY
                                .text()));
                    }
                    hold(world, call.flag(ON, true));
                    final JsonObject out = new JsonObject();
                    out.addProperty("held", held == world);
                    return CompletableFuture.completedFuture(McpResult.json(out));
                });
    }

    /** Holds the world {@code server} runs for its agent, or lets it run as it did before it was held. */
    public static void hold(final MinecraftServer server, final boolean on) {
        if (!on || held != server) {
            letGo();
        }
        if (on && held == null) {
            held = server;
            server.execute(() -> frozenBefore = server.tickRateManager().isFrozen());
        }
        settle(server);
    }

    /**
     * Lets the world held run as it did before it was held, frozen only if it was then, as the MCP server stops or
     * the player takes the controls back.
     */
    public static void letGo() {
        final MinecraftServer was = held;
        held = null;
        if (was != null) {
            was.execute(() -> {
                if (was.tickRateManager().isFrozen() != frozenBefore) {
                    was.tickRateManager().setFrozen(frozenBefore);
                }
            });
        }
    }

    /** Whether a world is held for its agent now. */
    public static boolean holding() {
        return held != null;
    }

    /** A call starts running ({@code true}) or has run: a world held runs only while a call does. */
    static void running(final boolean starts) {
        if (starts) {
            RUNNING.incrementAndGet();
        } else {
            RUNNING.updateAndGet(count -> Math.max(0, count - 1));
        }
        final MinecraftServer server = held;
        if (server != null) {
            settle(server);
        }
    }

    /*
     * Has {@code server}, while it is held, frozen or running as it is to be now, on its own thread: frozen while no
     * call runs. Worked out there, as it runs, so calls starting and ending at once on other threads leave it right;
     * one let go meanwhile is left as its letting go puts it.
     */
    private static void settle(final MinecraftServer server) {
        server.execute(() -> {
            final boolean frozen = RUNNING.get() == 0;
            if (held == server && server.tickRateManager().isFrozen() != frozen) {
                server.tickRateManager().setFrozen(frozen);
            }
        });
    }
}
