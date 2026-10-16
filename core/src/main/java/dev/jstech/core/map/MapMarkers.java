/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.map;

import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import dev.jstech.core.state.TeamState;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.worldgen.place.Landmark;
import dev.jstech.core.worldgen.place.Landmarks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The markers on the maps: each player's own, and their team's, which every member sees; kept with the world and sent
 * to each player as they change. The map mods show them through the Core's adapters, each layer a group the map shows
 * or hides; with no map mod, the series' own maps show them. Landmarks a player or their team discovered are markers
 * of the {@link #LANDMARKS} layer by themselves.
 *
 * <pre>{@code
 * public static final MarkerLayer RUINS = MapMarkers.layer(rl("ruins"), RUINS_NAME, 0xC08040, false);
 * MapMarkers.show(player, Marker.point("ruin_1", RUINS.id(), level.dimension(), pos, name));    // the player's
 * MapMarkers.share(server, player.getUUID(), Marker.area("coverage", COVERAGE.id(), dim, chunks, label));  // team
 * }</pre>
 */
@TextHolder
public final class MapMarkers {

    public static final TextKey LANDMARKS_NAME = TextKey.of("jscore.map.layer.landmarks", "Landmarks");
    public static final TextKey FOUND_NAME = TextKey.of("jscore.map.layer.found", "Places found");

    private static final Map<ResourceLocation, MarkerLayer> LAYERS = new ConcurrentHashMap<>();
    private static final List<MarkerLayer> IN_ORDER = new CopyOnWriteArrayList<>();
    /** Landmarks discovered, a marker each. */
    public static final MarkerLayer LANDMARKS = layer(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "landmarks"), LANDMARKS_NAME, 0xE8C060, false);
    /** Places an instrument found, until their finder takes them off. */
    public static final MarkerLayer FOUND = layer(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "found"),
            FOUND_NAME, 0x39D6C4, false);
    private static final PlayerState<List<Marker>> MINE = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "map_markers"), Marker.CODEC.listOf(),
            List.<Marker>of()).synced(Marker.STREAM_CODEC.apply(ByteBufCodecs.list())).player();
    private static final TeamState<List<Marker>> OURS = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "team_map_markers"), Marker.CODEC.listOf(),
            List.<Marker>of()).synced(Marker.STREAM_CODEC.apply(ByteBufCodecs.list())).team();

    private MapMarkers() {
    }

    /** Registers the markers' states, once, from the Core's constructor. */
    public static void register() {
        CoreStates.register(MINE);
        CoreStates.register(OURS);
    }

    /** Declares the layer {@code id}, called {@code name}, in {@code rgb}, of areas or of points. */
    public static MarkerLayer layer(final ResourceLocation id, final TextKey name, final int rgb, final boolean areas) {
        final MarkerLayer layer = new MarkerLayer(id, name, rgb, areas);
        if (LAYERS.putIfAbsent(id, layer) == null) {
            IN_ORDER.add(layer);
        }
        return LAYERS.get(id);
    }

    /** Every layer, in the order they were declared. */
    public static List<MarkerLayer> layers() {
        return List.copyOf(IN_ORDER);
    }

    /** The layer {@code id}, or {@link #FOUND} for a layer no mod declared. */
    public static MarkerLayer layerOf(final ResourceLocation id) {
        return LAYERS.getOrDefault(id, FOUND);
    }

    /** Shows {@code marker} to {@code player} alone, in place of their marker of the same id. */
    public static void show(final ServerPlayer player, final Marker marker) {
        final MinecraftServer server = player.getServer();
        if (server != null) {
            MINE.update(server, player.getUUID(), markers -> with(markers, marker));
        }
    }

    /** Shows {@code marker} to the whole team of {@code player}, in place of the team's marker of the same id. */
    public static void share(final MinecraftServer server, final UUID player, final Marker marker) {
        OURS.update(server, CoreTeams.teamOf(server, player), markers -> with(markers, marker));
    }

    /** Takes the marker {@code id} off the maps of {@code player} and of their team. */
    public static void hide(final MinecraftServer server, final UUID player, final String id) {
        MINE.update(server, player, markers -> without(markers, id));
        OURS.update(server, CoreTeams.teamOf(server, player), markers -> without(markers, id));
    }

    /** The markers {@code player} sees: their own, then their team's. */
    public static List<Marker> of(final MinecraftServer server, final UUID player) {
        final Map<String, Marker> seen = new LinkedHashMap<>();
        OURS.ofPlayer(server, player).forEach(marker -> seen.put(marker.id(), marker));
        MINE.get(server, player).forEach(marker -> seen.put(marker.id(), marker));
        return List.copyOf(seen.values());
    }

    /**
     * The markers a player's game holds of its player, as the server last said, on the side {@code level} is on:
     * their own, their team's, and a marker for each landmark they or their team discovered. Read on a player's game
     * by the map adapters and the series' own maps.
     */
    public static List<Marker> sent(final Level level) {
        final Map<String, Marker> seen = new LinkedHashMap<>();
        OURS.client().forEach(marker -> seen.put(marker.id(), marker));
        MINE.client().forEach(marker -> seen.put(marker.id(), marker));
        for (final ResourceLocation id : Landmarks.discoveredHere()) {
            final Optional<Landmark> landmark = Landmarks.get(level, id);
            landmark.ifPresent(found -> seen.putIfAbsent(id.toString(), Marker.point(id.toString(), LANDMARKS.id(),
                    found.dimension(), found.pos(), GameText.component(found.title().text()))));
        }
        return List.copyOf(seen.values());
    }

    /** The markers of {@link #sent} in {@code level}'s own dimension. */
    public static List<Marker> sentIn(final Level level) {
        final List<Marker> in = new ArrayList<>();
        for (final Marker marker : sent(level)) {
            if (marker.dimension().equals(level.dimension())) {
                in.add(marker);
            }
        }
        return in;
    }

    private static List<Marker> with(final List<Marker> markers, final Marker marker) {
        final List<Marker> next = new ArrayList<>(without(markers, marker.id()));
        next.add(marker);
        return List.copyOf(next);
    }

    private static List<Marker> without(final List<Marker> markers, final String id) {
        final List<Marker> next = new ArrayList<>(markers);
        next.removeIf(marker -> marker.id().equals(id));
        return next.size() == markers.size() ? markers : List.copyOf(next);
    }
}
