/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.client.hazard.HazardMapsClient;
import dev.jstech.core.registry.CoreAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The maps of hazards: what each player knows of the land's hazards, chunk by chunk. An instrument carried in the
 * hotbar or the off hand reads the rate of its hazard at the player's chunk each second, as it came before what they
 * wear; a mod's sensor puts its own readings in ({@link #record}). A chunk is read again when the rate changed or the
 * last reading grew old. The player's game is told every reading of the dimension they arrive in, the nearest first up
 * to a limit, and each new one as it is read; what was never read the map leaves grey.
 */
public final class HazardMaps {

    private static final String VERSION = "1";
    /* How old a chunk's reading grows before it is read again though the rate is the same, in ticks. */
    private static final long FRESH = 600;
    /* How far a rate must move from the last reading, as a share of it, to be read again at once. */
    private static final double CHANGED = 0.1;
    /* Below this a rate counts as nothing, so the background's flicker is not read over and over. */
    private static final double NOTHING = 1.0E-4;
    /* The most readings a player's game is told at once, as they arrive in a dimension. */
    private static final int TOLD_AT_ONCE = 4096;

    private HazardMaps() {
    }

    /** Hands the buses the payload and what tells a player's game their map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(HazardMapPayload.TYPE, HazardMapPayload.STREAM_CODEC, HazardMaps::onMap));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                tellAll(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                tellAll(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                tellAll(player);
            }
        });
    }

    /** The declared hazards {@code stack} measures, if it is an instrument; none otherwise. */
    public static List<HazardKind> readBy(final ItemStack stack) {
        if (!(stack.getItem() instanceof IHazardInstrument instrument)) {
            return List.of();
        }
        return instrument.reads(stack).stream().map(Hazards::find).flatMap(Optional::stream).toList();
    }

    /** The mapped hazards {@code player}'s carried instruments measure: in the hotbar and the off hand. */
    public static Set<HazardKind> measured(final ServerPlayer player) {
        final Set<HazardKind> kinds = new LinkedHashSet<>();
        final Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            kinds.addAll(readBy(inventory.getItem(slot)));
        }
        kinds.addAll(readBy(player.getOffhandItem()));
        kinds.removeIf(kind -> !kind.mapped() || !kind.enabled());
        return kinds;
    }

    /** A second of {@code player}'s instruments: each reads its hazard at the chunk they stand in. */
    public static void second(final ServerPlayer player) {
        for (final HazardKind kind : measured(player)) {
            record(player, player.level().dimension(), player.chunkPosition(), kind,
                    kind.shownRate(Hazards.exposure(player, kind)));
        }
    }

    /**
     * Puts in {@code player}'s map a reading of {@code kind} at {@code chunk} of {@code dimension}, {@code shown} in
     * the unit its rate is shown in, as an instrument or a mod's sensor read it; kept and told only when the chunk had
     * no reading, the rate moved or the last reading grew old.
     */
    public static void record(final ServerPlayer player, final ResourceKey<Level> dimension, final ChunkPos chunk,
                              final HazardKind kind, final double shown) {
        final HazardMap map = player.getData(CoreAttachments.HAZARD_MAP);
        final long now = player.server.overworld().getGameTime();
        final float value = (float) (shown < NOTHING ? 0 : shown);
        final Optional<HazardMap.Reading> last = map.reading(dimension.location(), kind.id(), chunk.toLong());
        if (last.isPresent() && now - last.get().time() < FRESH
                && Math.abs(value - last.get().value()) <= CHANGED * Math.max(last.get().value(), NOTHING)) {
            return;
        }
        map.put(dimension.location(), kind.id(), chunk.toLong(), new HazardMap.Reading(value, now));
        if (player.connection != null && player.level().dimension() == dimension) {
            PacketDistributor.sendToPlayer(player, new HazardMapPayload(dimension.location(), false,
                    List.of(new HazardMapPayload.Entry(kind.id(), chunk.toLong(), value, now))));
        }
    }

    /** Tells {@code player}'s game what their map holds of the dimension they are in, the nearest chunks first. */
    public static void tellAll(final ServerPlayer player) {
        if (player.connection == null) {
            return;
        }
        final ChunkPos here = player.chunkPosition();
        final List<HazardMapPayload.Entry> entries = new ArrayList<>();
        final Map<ResourceLocation, Map<Long, HazardMap.Reading>> known = player.getData(CoreAttachments.HAZARD_MAP)
                .of(player.level().dimension().location());
        known.forEach((kind, chunks) -> chunks.forEach((chunk, reading) -> entries.add(new HazardMapPayload.Entry(
                kind, chunk, reading.value(), reading.time()))));
        entries.sort(Comparator.comparingLong(entry -> distance(here, entry.chunk())));
        PacketDistributor.sendToPlayer(player, new HazardMapPayload(player.level().dimension().location(), true,
                entries.subList(0, Math.min(entries.size(), TOLD_AT_ONCE))));
    }

    private static long distance(final ChunkPos here, final long chunk) {
        final long dx = ChunkPos.getX(chunk) - here.x;
        final long dz = ChunkPos.getZ(chunk) - here.z;
        return dx * dx + dz * dz;
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onMap(final HazardMapPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> HazardMapsClient.told(payload));
    }
}
