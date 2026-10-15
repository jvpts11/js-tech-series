/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/** The payload the synced values of open menus travel in, registered once by the Core. */
public final class MenuSync {

    private static final String NETWORK_VERSION = "1";

    private MenuSync() {
    }

    /** Hands the mod's bus the payload, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToClient(MenuSyncPayload.TYPE, MenuSyncPayload.STREAM_CODEC, MenuSync::onSync));
    }

    /* Runs on a player's game: the values land in the menu they were written for, if it is still the open one. */
    private static void onSync(final MenuSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.containerMenu instanceof CoreMenu menu && menu.containerId == payload.containerId()) {
                menu.receive(payload, player.registryAccess());
            }
        });
    }
}
