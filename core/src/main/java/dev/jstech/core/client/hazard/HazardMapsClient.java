/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hazard;

import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.hazard.HazardMapPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The player's map of hazards in the dimension they are in, as the server told it: the rate read at each chunk that
 * was measured, by hazard, and when. Only touched on the game's thread.
 */
public final class HazardMapsClient {

    private static final Map<ResourceLocation, Map<Long, HazardMap.Reading>> READINGS = new HashMap<>();
    private static ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");

    private HazardMapsClient() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> READINGS.clear());
    }

    /** What the server told of the player's map. */
    public static void told(final HazardMapPayload payload) {
        if (payload.replace() || !payload.dimension().equals(dimension)) {
            READINGS.clear();
            dimension = payload.dimension();
        }
        for (final HazardMapPayload.Entry entry : payload.readings()) {
            READINGS.computeIfAbsent(entry.kind(), kind -> new HashMap<>()).put(entry.chunk(),
                    new HazardMap.Reading(entry.value(), entry.time()));
        }
    }

    /** The reading of the hazard {@code kind} at {@code chunk}, the chunk's place packed as the game packs it. */
    public static Optional<HazardMap.Reading> reading(final ResourceLocation kind, final long chunk) {
        return Optional.ofNullable(READINGS.getOrDefault(kind, Map.of()).get(chunk));
    }

    /** How many chunks of the hazard {@code kind} the map knows here. */
    public static int known(final ResourceLocation kind) {
        return READINGS.getOrDefault(kind, Map.of()).size();
    }
}
