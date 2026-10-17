/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.voicechat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.audio.acoustics.VoiceAcoustics;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Simple Voice Chat's voices through the Core's world: a voice from a player, or from a place, comes muffled through
 * walls, weaker in thin air and not at all through a vacuum, while a voice from no place (a group, a radio) is left as
 * it is. Simple Voice Chat finds this by its annotation and makes it only where it is installed, so nothing of it is
 * ever loaded without it.
 */
@ForgeVoicechatPlugin
public final class VoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return JsCore.MODID;
    }

    @Override
    public void registerEvents(final EventRegistration registration) {
        // What a player hears is changed on their own game; a server has no voices to hear.
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class,
                event -> event.setRawAudio(VoiceAcoustics.fromEntity(event.getId(), event.getRawAudio())));
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class,
                event -> event.setRawAudio(VoiceAcoustics.fromPlace(event.getId(), event.getPosition().getX(),
                        event.getPosition().getY(), event.getPosition().getZ(), event.getRawAudio())));
    }
}
