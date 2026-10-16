/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.field;

import dev.jstech.core.world.field.ChunkField;
import dev.jstech.core.world.field.ChunkFieldPayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The synced fields over chunks as the player's game holds them: each field's values round the player, as the server
 * last sent them, for a mod's overlay, instrument or sky to read.
 */
public final class ChunkFieldsClient {

    private static final Map<ResourceLocation, ChunkFieldPayload> VIEWS = new ConcurrentHashMap<>();

    private ChunkFieldsClient() {
    }

    /** Hands the game's bus the leaving of a world, once, from the Core's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> VIEWS.clear());
    }

    /** Keeps what the server sent of a field. */
    public static void receive(final ChunkFieldPayload payload) {
        VIEWS.put(payload.field(), payload);
    }

    /** The value of {@code field} at {@code chunk} as last sent; 0 where it has none, or too far to have been sent. */
    public static float value(final ChunkField field, final ChunkPos chunk) {
        return value(field.id(), chunk);
    }

    /** The same, by the field's id. */
    public static float value(final ResourceLocation field, final ChunkPos chunk) {
        final ChunkFieldPayload view = VIEWS.get(field);
        return view == null ? 0 : view.at(chunk.x - view.centreX(), chunk.z - view.centreZ());
    }
}
