/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A command an agent was let run, run as the player it acts for: on the server's own thread, with what the command
 * answers kept and handed back, not shown in anyone's chat.
 */
public final class GameCommands {

    /** The level of a player who may cheat in a world of their own. */
    public static final int CHEATS = 2;

    private GameCommands() {
    }

    /**
     * Runs {@code command} as {@code player}, at the level {@code level} or the player's own when it is less than
     * one; what it answers, and how many times it succeeded.
     */
    public static CompletableFuture<McpResult> run(final ServerPlayer player, final String command, final int level) {
        final CompletableFuture<McpResult> done = new CompletableFuture<>();
        player.server.execute(() -> {
            final List<String> said = new CopyOnWriteArrayList<>();
            final CommandSource heard = new CommandSource() {
                @Override
                public void sendSystemMessage(final Component message) {
                    said.add(message.getString());
                }

                @Override
                public boolean acceptsSuccess() {
                    return true;
                }

                @Override
                public boolean acceptsFailure() {
                    return true;
                }

                @Override
                public boolean shouldInformAdmins() {
                    return false;
                }
            };
            CommandSourceStack source = player.createCommandSourceStack().withSource(heard);
            if (level > 0) {
                source = source.withPermission(level);
            }
            try {
                player.server.getCommands().performPrefixedCommand(source, command);
                final JsonObject out = new JsonObject();
                out.addProperty("command", command);
                final JsonArray lines = new JsonArray();
                said.forEach(lines::add);
                out.add("said", lines);
                done.complete(McpResult.json(out));
            } catch (final RuntimeException failed) {
                done.complete(McpResult.failed(McpWords.WENT_WRONG.with(String.valueOf(failed.getMessage()))));
            }
        });
        return done;
    }
}
