/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.network;

import dev.jstech.core.network.transfer.BigPayloads;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** A player's game forgets the large values a server was still sending it as it leaves that server. */
public final class TransferClient {

    private TransferClient() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, TransferClient::onLoggingOut);
    }

    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        BigPayloads.forgetServer();
    }
}
