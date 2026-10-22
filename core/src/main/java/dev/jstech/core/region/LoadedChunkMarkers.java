/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import dev.jstech.core.JsCore;
import dev.jstech.core.map.MapMarkers;
import dev.jstech.core.map.Marker;
import dev.jstech.core.map.MarkerLayer;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * The map of the chunks kept loaded: each source's chunks are an area on the maps of its owner's team, kept up to date
 * as it loads and lets go of them; and an operator sees every source of every dimension, with whose it is, on their
 * own maps, as things are when they ask.
 */
@TextHolder
public final class LoadedChunkMarkers {

    public static final TextKey LAYER_NAME = TextKey.of("jscore.map.layer.loaded_chunks", "Chunks kept loaded");
    public static final TextKey KEPT = TextKey.of("jscore.map.loaded_chunks.kept", "Kept loaded: %s");
    public static final TextKey KEPT_BY = TextKey.of("jscore.map.loaded_chunks.kept_by", "Kept loaded by %s: %s");
    /** The layer the chunks kept loaded are drawn in, as areas. */
    public static final MarkerLayer LAYER = MapMarkers.layer(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "loaded_chunks"), LAYER_NAME, 0x55FFFF, true);

    private static final String OWN = "loaded/";
    private static final String EVERY = "loaded_every/";

    private LoadedChunkMarkers() {
    }

    /**
     * Shows every source of every dimension and whose it is on {@code operator}'s own maps, as things are now.
     *
     * @return how many sources were shown
     */
    public static int showAll(final ServerPlayer operator) {
        final MinecraftServer server = operator.getServer();
        if (server == null) {
            return 0;
        }
        hideAll(operator);
        int shown = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            final Map<BlockPos, UUID> owners = ChunkLoaders.sources(level);
            final Map<BlockPos, TextKey> reasons = ChunkLoaders.reasons(level);
            for (final Map.Entry<BlockPos, Set<ChunkPos>> source : ChunkLoaders.kept(level).entrySet()) {
                final UUID owner = owners.get(source.getKey());
                final String name = owner == null ? "" : CoreTeams.nameOf(server, owner).orElse(owner.toString());
                final TextKey reason = reasons.getOrDefault(source.getKey(), ChunkLoaders.UNSAID);
                MapMarkers.show(operator, Marker.area(id(EVERY, level, source.getKey()), LAYER.id(),
                        level.dimension(), List.copyOf(source.getValue()),
                        GameText.component(KEPT_BY.with(name, reason))));
                shown++;
            }
        }
        return shown;
    }

    /** Takes what {@link #showAll} showed off {@code operator}'s maps; how many markers it took off. */
    public static int hideAll(final ServerPlayer operator) {
        final MinecraftServer server = operator.getServer();
        if (server == null) {
            return 0;
        }
        int hidden = 0;
        for (final Marker marker : MapMarkers.of(server, operator.getUUID())) {
            if (marker.id().startsWith(EVERY)) {
                MapMarkers.hide(server, operator.getUUID(), marker.id());
                hidden++;
            }
        }
        return hidden;
    }

    /** The id of the marker of the source at {@code source} of {@code level} on its owner's team's maps. */
    public static String ownId(final ServerLevel level, final BlockPos source) {
        return id(OWN, level, source);
    }

    /* The source at {@code source} keeps {@code chunks} now: its marker on its owner's team's maps follows. */
    static void changed(final ServerLevel level, final BlockPos source, final UUID owner, final TextKey reason,
                        final LongSet chunks) {
        final String id = id(OWN, level, source);
        if (chunks.isEmpty()) {
            MapMarkers.hide(level.getServer(), owner, id);
            return;
        }
        final List<ChunkPos> area = new ArrayList<>();
        chunks.forEach(chunk -> area.add(new ChunkPos(chunk)));
        MapMarkers.share(level.getServer(), owner, Marker.area(id, LAYER.id(), level.dimension(), area,
                GameText.component(KEPT.with(reason))));
    }

    private static String id(final String prefix, final ServerLevel level, final BlockPos source) {
        return prefix + level.dimension().location() + "/" + source.getX() + "_" + source.getY() + "_"
                + source.getZ();
    }
}
