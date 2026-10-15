/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.state;

import dev.jstech.core.state.ClientStates;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Lets every state's value go when the player leaves a server, so the next server's start from their defaults. */
public final class ClientStateEvents {

    private ClientStateEvents() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, ClientStateEvents::onLoggingOut);
    }

    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStates.clear();
    }
}
