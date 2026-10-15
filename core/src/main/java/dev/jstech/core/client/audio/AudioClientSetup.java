/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.pcm.AudioDecoders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * The part of the sound system only a client has: the decoders that live in the game's client, Ogg Vorbis's, and the
 * cues' bindings, which come from the resource packs.
 */
public final class AudioClientSetup {

    private AudioClientSetup() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, AudioClientSetup::onClientSetup);
        modBus.addListener(RegisterClientReloadListenersEvent.class, AudioClientSetup::onRegisterReloadListeners);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        AudioDecoders.register("ogg", new OggDecoder());
    }

    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SoundCueBindings());
    }
}
