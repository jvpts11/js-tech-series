/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.media;

import dev.jstech.core.JsCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Sends the uploads under way a tick at a time, and lets every recording go when the player leaves a server. */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class MediaClientEvents {

    private MediaClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        MediaUploader.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        MediaPlayer.clear();
        MediaCache.clear();
        MediaUploader.clear();
    }
}
