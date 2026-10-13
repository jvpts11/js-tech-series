/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.pcm.AudioDecoders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The part of the sound system only a client has: the decoders that live in the game's client, Ogg Vorbis's, and the
 * cues' bindings, which come from the resource packs.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AudioClientSetup {

    private AudioClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        AudioDecoders.register("ogg", new OggDecoder());
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SoundCueBindings());
    }
}
