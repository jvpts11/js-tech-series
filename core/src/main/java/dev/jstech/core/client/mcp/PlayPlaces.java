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
import dev.jstech.core.agent.AgentPlaces;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where things are, for an agent playing the game: the places every mod tells of in the player's dimension
 * ({@link AgentPlaces}), each with its distance from the player's eyes, its bearing as the game's yaw, how far up or
 * down it lies and how far the player turns to face it, the nearest first. Read from the world the game runs.
 */
final class PlayPlaces {

    private static final String KIND = "kind";
    private static final String WITHIN = "within";
    private static final String MOST = "most";
    private static final int MOST_PLACES = 200;
    private static final int SOME_PLACES = 30;
    private static final long FARTHEST = 100_000_000L;
    private static final double TENTHS = 10.0;

    private PlayPlaces() {
    }

    /** The tools. */
    static List<McpTool> all() {
        return List.of(new McpTool("places_near", "Where things are in the player's dimension, near and far, as the "
                + "mods tell them: the respawn point, the world's spawn, formed multiblocks, and each mod's own, such "
                + "as the low orbits of worlds, landing pads, runways and docks. Each with its kind, its name, where "
                + "it is, its distance from the player's eyes, its bearing as the game's yaw, its climb in degrees "
                + "(up when more than 0) and how far the player turns to face it (right when more than 0), the "
                + "nearest first.", McpSchema.object()
                .words(KIND, "Only places of this kind, or whose kind holds these words.", false)
                .whole(WITHIN, "Only places this near, in blocks.", 1, FARTHEST, false)
                .whole(MOST, "At most this many.", 1, MOST_PLACES, false).build(), McpPermission.READ,
                PlayPlaces::near));
    }

    private static CompletableFuture<McpResult> near(final McpCall call) {
        final Minecraft game = Minecraft.getInstance();
        final MinecraftServer server = game.getSingleplayerServer();
        if (server == null || game.player == null) {
            return CompletableFuture.completedFuture(McpResult.failed(McpTexts.PLACES_OWN_WORLD_ONLY.text()));
        }
        final UUID id = game.player.getUUID();
        final Optional<String> kind = call.text(KIND).map(words -> words.toLowerCase(Locale.ROOT));
        final double within = call.whole(WITHIN).orElse(FARTHEST);
        final int most = (int) (long) call.whole(MOST).orElse((long) SOME_PLACES);
        return server.submit(() -> {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                return McpResult.failed(McpTexts.PLACES_OWN_WORLD_ONLY.text());
            }
            final Vec3 eye = player.getEyePosition();
            final JsonArray places = new JsonArray();
            AgentPlaces.of(player.serverLevel(), player).stream()
                    .filter(found -> kind.isEmpty() || found.place().kind().contains(kind.get()))
                    .filter(found -> found.place().at().distanceTo(eye) <= within)
                    .sorted(Comparator.comparingDouble(found -> found.place().at().distanceTo(eye)))
                    .limit(most)
                    .forEach(found -> places.add(told(found, eye, player.getYRot())));
            final JsonObject out = new JsonObject();
            out.addProperty("dimension", player.serverLevel().dimension().location().toString());
            out.add("places", places);
            return McpResult.json(out);
        });
    }

    /* A place as the agent is told it, from {@code eye}, the player facing {@code yaw}. */
    private static JsonObject told(final AgentPlaces.Found found, final Vec3 eye, final float yaw) {
        final Vec3 at = found.place().at();
        final Vec3 way = at.subtract(eye);
        final double flat = Math.sqrt(way.x * way.x + way.z * way.z);
        final double bearing = Math.toDegrees(Math.atan2(-way.x, way.z));
        final JsonObject one = new JsonObject();
        one.addProperty("mod", found.modid());
        one.addProperty(KIND, found.place().kind());
        if (!found.place().name().isEmpty()) {
            one.addProperty("name", found.place().name());
        }
        one.addProperty("x", tenths(at.x));
        one.addProperty("y", tenths(at.y));
        one.addProperty("z", tenths(at.z));
        one.addProperty("distance", tenths(way.length()));
        one.addProperty("bearing", tenths(bearing));
        one.addProperty("climb", tenths(Math.toDegrees(Math.atan2(way.y, flat))));
        one.addProperty("turn", tenths(Mth.wrapDegrees(bearing - yaw)));
        return one;
    }

    private static double tenths(final double value) {
        return Math.round(value * TENTHS) / TENTHS;
    }
}
