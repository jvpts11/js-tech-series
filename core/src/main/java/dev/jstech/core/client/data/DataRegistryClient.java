/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.data;

import dev.jstech.core.data.DataRegistries;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** A player's game forgets what a server's data registries sent it as it leaves that server. */
public final class DataRegistryClient {

    private DataRegistryClient() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, DataRegistryClient::onLoggingOut);
    }

    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        DataRegistries.forgetClientValues();
    }
}
