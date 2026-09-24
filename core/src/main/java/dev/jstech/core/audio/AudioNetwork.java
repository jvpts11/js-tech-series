/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.audio.AudioEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * What the sound system sends over the network: the sounds of a player's own screen, which only they hear, the
 * notes of a synthesised sound, which each client that hears them makes into samples, and cues, whose sound each
 * client picks from its own packs.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class AudioNetwork {

    private static final String VERSION = "1";

    private AudioNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(ScreenSoundPayload.TYPE, ScreenSoundPayload.STREAM_CODEC, AudioNetwork::onScreenSound);
        registrar.playToClient(ToneSoundPayload.TYPE, ToneSoundPayload.STREAM_CODEC, AudioNetwork::onToneSound);
        registrar.playToClient(CueSoundPayload.TYPE, CueSoundPayload.STREAM_CODEC, AudioNetwork::onCueSound);
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onScreenSound(final ScreenSoundPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> AudioEngine.playOnScreen(payload.sound(), payload.volume(), payload.pitch()));
    }

    /* Runs on a client only, as the one above. */
    private static void onToneSound(final ToneSoundPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> AudioEngine.playTones(payload));
    }

    /* Runs on a client only, as the ones above. */
    private static void onCueSound(final CueSoundPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> AudioEngine.playCue(payload));
    }
}
