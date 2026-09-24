/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The one place every sound passes on its way to the speakers, the game's own included: a sound the player turned off
 * is dropped here, and a sound of the series is played at the volume the player gave its channel.
 *
 * <p>It stands in the game's own path rather than beside the series' calls, so a sound reaches it however it was
 * started: from a machine, from the server's packet, from a command a player typed, from another mod.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AudioMixer {

    private static volatile @Nullable ResourceLocation lastHeard;

    private AudioMixer() {
    }

    @SubscribeEvent
    public static void onPlaySound(final PlaySoundEvent event) {
        final SoundInstance sound = event.getSound();
        if (sound != null) {
            event.setSound(mix(sound));
        }
    }

    /**
     * The sound as it should be heard: nothing when the player turned it off, a share of its volume when it is the
     * series' and its channel is turned down, and the sound itself otherwise.
     */
    @Nullable
    public static SoundInstance mix(final SoundInstance sound) {
        final ResourceLocation id = sound.getLocation();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        if (prefs.isMuted(id.toString())) {
            return null;
        }
        lastHeard = id;
        if (sound instanceof ScaledSoundInstance) {
            return sound;
        }
        final SoundKey key = SoundKeys.find(id);
        if (key == null) {
            return sound;
        }
        final float share = prefs.volume(key.spec().channel().id().toString());
        return share >= 1.0F ? sound : ScaledSoundInstance.of(sound, share);
    }

    /** The last sound let through, which is the one a player means when they ask to turn off what they just heard. */
    @Nullable
    public static ResourceLocation lastHeard() {
        return lastHeard;
    }
}
