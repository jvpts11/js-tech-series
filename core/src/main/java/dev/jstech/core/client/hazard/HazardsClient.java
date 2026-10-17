/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hazard;

import dev.jstech.core.hazard.HazardsPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The player's own hazards, as the server last told them: what has built up of each, and how fast each builds up where
 * they stand, for the instruments in their hands and the body's screen.
 */
public final class HazardsClient {

    private static volatile Map<ResourceLocation, HazardsPayload.Entry> told = Map.of();

    private HazardsClient() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> told = Map.of());
    }

    /** What the server told of the player's hazards. */
    public static void told(final HazardsPayload payload) {
        final Map<ResourceLocation, HazardsPayload.Entry> now = new LinkedHashMap<>();
        payload.hazards().forEach(entry -> now.put(entry.id(), entry));
        told = Map.copyOf(now);
    }

    /** What has built up of the hazard {@code id} in the player. */
    public static double level(final ResourceLocation id) {
        final HazardsPayload.Entry entry = told.get(id);
        return entry == null ? 0 : entry.level();
    }

    /** How fast the hazard {@code id} builds up where the player stands, in its unit a second. */
    public static double rate(final ResourceLocation id) {
        final HazardsPayload.Entry entry = told.get(id);
        return entry == null ? 0 : entry.rate();
    }

    /** How fast the hazard {@code id} comes at the player where they stand, before what they wear, a second. */
    public static double exposure(final ResourceLocation id) {
        final HazardsPayload.Entry entry = told.get(id);
        return entry == null ? 0 : entry.exposure();
    }

    /** Every hazard the player has some of, or takes some of. */
    public static Map<ResourceLocation, HazardsPayload.Entry> all() {
        return told;
    }
}
