/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.media;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Sends the uploads under way a tick at a time, and lets every recording go when the player leaves a server. */
public final class MediaClientEvents {

    private MediaClientEvents() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, MediaClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, MediaClientEvents::onLoggingOut);
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        MediaUploader.tick();
    }

    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        MediaPlayer.clear();
        MediaCache.clear();
        MediaUploader.clear();
    }
}
