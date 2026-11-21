/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jstech.core.developer.mcp.IMcpHost;
import dev.jstech.core.developer.mcp.McpChanges;
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
 * their health, and the lines of chat that came. Each reading has the name the tool player_state gives it.
 */
final class PlayChanges {

    /** The lines of chat that came while a call ran, and how far the player went in blocks, as answers name them. */
    static final String CHAT_NEW = "chat_new";
    static final String MOVED = "moved";
    /** How long a glimpse waits for the game's own thread, in seconds; past it the call is told nothing. */
    private static final long WAITS = 2;
    /* How many lines of chat had come by a glimpse: what the lines that came during a call are counted from. */
    private static final String CHAT_HEARD = "chat_heard";
    private static final String DIMENSION = "dimension";

    private PlayChanges() {
    }

    /**
     * What a call's answer tells from the glimpse {@code before} it and the one {@code after} it: the tick and what
     * changed, how far the player went, under {@link #MOVED}, and the lines of chat that came between them, under
     * {@link #CHAT_NEW}, in place of how many came.
     */
    static JsonObject told(final JsonObject before, final JsonObject after) {
        final JsonObject told = McpChanges.told(before, after);
        moved(before, after).ifPresent(blocks -> told.addProperty(MOVED, blocks));
        if (before.has(CHAT_HEARD) && after.has(CHAT_HEARD)) {
            if (told.has(McpChanges.CHANGED)) {
                final JsonObject changed = told.getAsJsonObject(McpChanges.CHANGED);
                changed.remove(CHAT_HEARD);
                if (changed.isEmpty()) {
                    told.remove(McpChanges.CHANGED);
                }
            }
            final long heard = before.get(CHAT_HEARD).getAsLong();
            final JsonArray lines = new JsonArray();
            // Only the lines heard by the glimpse after the call: one that came since is the next call's.
            ChatLines.since(heard).stream().limit(Math.max(0L, after.get(CHAT_HEARD).getAsLong() - heard))
                    .forEach(line -> lines.add(PlaySeeing.chatText(line)));
            if (!lines.isEmpty()) {
                told.add(CHAT_NEW, lines);
            }
        }
        return told;
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
        out.addProperty(DIMENSION, player.level().dimension().location().toString());
        out.add(Play.PLACE, Play.place(player.blockPosition()));
        out.addProperty("slot", player.getInventory().selected);
        out.add("held", player.getMainHandItem().isEmpty() ? JsonNull.INSTANCE : Play.stack(player.getMainHandItem()));
        out.addProperty("health", player.getHealth());
        out.addProperty(CHAT_HEARD, ChatLines.added());
        return out;
    }

    /* How far the player went from the block of one glimpse to the other's, to a tenth; none in another world. */
    private static Optional<Double> moved(final JsonObject before, final JsonObject after) {
        if (!before.has(Play.PLACE) || !after.has(Play.PLACE) || before.get(Play.PLACE).equals(after.get(Play.PLACE))
                || !before.get(DIMENSION).equals(after.get(DIMENSION))) {
            return Optional.empty();
        }
        final JsonObject from = before.getAsJsonObject(Play.PLACE);
        final JsonObject to = after.getAsJsonObject(Play.PLACE);
        final double dx = to.get("x").getAsInt() - from.get("x").getAsInt();
        final double dy = to.get("y").getAsInt() - from.get("y").getAsInt();
        final double dz = to.get("z").getAsInt() - from.get("z").getAsInt();
        return Optional.of(Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz) * 10) / 10.0);
    }

    private static JsonElement orNull(final Optional<JsonObject> reading) {
        return reading.isPresent() ? reading.get() : JsonNull.INSTANCE;
    }
}
