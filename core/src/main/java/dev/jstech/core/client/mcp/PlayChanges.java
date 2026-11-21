/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jstech.core.developer.mcp.IMcpHost;
import dev.jstech.core.developer.mcp.McpChanges;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The player's game glimpsed round every MCP call ({@link IMcpHost#glimpse}), so the call's answer tells what went by
 * and what changed: the game's tick, whether the game is paused or its ticking frozen, the screen open, what the
 * crosshair is on, what the player rides, the world and the block they stand in, the slot held and what is in it,
 * their health, and the newest line of chat. Each reading has the name the tool player_state gives it.
 */
final class PlayChanges {

    /** How long a glimpse waits for the game's own thread, in seconds; past it the call is told nothing. */
    private static final long WAITS = 2;

    private PlayChanges() {
    }

    /** The game now, read on its own thread; empty with no world open, or when the game does not answer in time. */
    static JsonObject glimpse() {
        try {
            return Minecraft.getInstance().submit(PlayChanges::read).get(WAITS, TimeUnit.SECONDS);
        } catch (final InterruptedException stopped) {
            Thread.currentThread().interrupt();
            return new JsonObject();
        } catch (final ExecutionException | TimeoutException failed) {
            return new JsonObject();
        }
    }

    private static JsonObject read() {
        final Minecraft game = Minecraft.getInstance();
        final LocalPlayer player = game.player;
        final JsonObject out = new JsonObject();
        if (player == null || game.level == null) {
            return out;
        }
        out.addProperty(McpChanges.TICK, game.level.getGameTime());
        out.addProperty("paused", game.isPaused());
        out.addProperty("frozen", game.level.tickRateManager().isFrozen());
        out.add(PlaySeeing.SCREEN, orNull(PlaySeeing.screen(game.screen)));
        out.add(PlaySeeing.LOOKING_AT_BLOCK, orNull(PlaySeeing.lookedAtBlock(game, player)));
        out.add(PlaySeeing.LOOKING_AT_ENTITY, orNull(PlaySeeing.lookedAtEntity(game)));
        out.add("riding", player.getVehicle() == null ? JsonNull.INSTANCE : new JsonPrimitive(
                BuiltInRegistries.ENTITY_TYPE.getKey(player.getVehicle().getType()).toString()));
        out.addProperty("dimension", player.level().dimension().location().toString());
        out.add(Play.PLACE, Play.place(player.blockPosition()));
        out.addProperty("slot", player.getInventory().selected);
        out.add("held", player.getMainHandItem().isEmpty() ? JsonNull.INSTANCE : Play.stack(player.getMainHandItem()));
        out.addProperty("health", player.getHealth());
        final List<ChatLines.Line> lines = ChatLines.lines();
        out.add("chat_newest", lines.isEmpty() ? JsonNull.INSTANCE : PlaySeeing.chatText(lines.getLast()));
        return out;
    }

    private static JsonElement orNull(final Optional<JsonObject> reading) {
        return reading.isPresent() ? reading.get() : JsonNull.INSTANCE;
    }
}
