/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * A player's own count of the chunks their machines keep loaded, against their quota, on their own game: what a
 * machine that keeps chunks loaded says in its tooltip before it is set down ("Your quota: 9 of 25 chunks"). The
 * server tells a player as they join and every time one of their machines takes or lets go of a chunk
 * ({@link ChunkLoaders}); the game keeps the last count it was told.
 *
 * <pre>{@code
 * int kept = ChunkQuotas.held();
 * int mayKeep = ChunkQuotas.limit();
 * }</pre>
 */
public final class ChunkQuotas {

    private static final String NETWORK_VERSION = "1";

    /* The count and the quota on this game, as the server last told them; nothing kept before it does. */
    private static volatile int held;
    private static volatile int limit = ChunkLoadingBalance.DEFAULT_LIMIT;

    private ChunkQuotas() {
    }

    /** How many chunks this game's player keeps loaded, across every dimension, as the server last told it. */
    public static int held() {
        return held;
    }

    /** How many chunks this game's player may keep loaded, as the server last told it. */
    public static int limit() {
        return limit;
    }

    /** Hands the buses what tells a player their count, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToClient(ChunkQuotaPayload.TYPE, ChunkQuotaPayload.STREAM_CODEC, (payload, context) -> {
                    held = payload.held();
                    limit = payload.limit();
                }));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                tell(player);
            }
        });
    }

    /* Tells {@code owner} their count again, when they are on the server: one of their machines just changed it. */
    static void changed(final MinecraftServer server, final UUID owner) {
        final ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            tell(player);
        }
    }

    /* A player with no connection, such as one a test put on the server, has no game to tell. */
    private static void tell(final ServerPlayer player) {
        if (player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new ChunkQuotaPayload(
                ChunkLoaders.loadedBy(player.server, player.getUUID()), ChunkLoadingBalance.limit()));
    }
}
