/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.DuckEnvelope;
import dev.jstech.core.audio.RecentSounds;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The one place every sound passes on its way to the speakers, the game's own included: a sound the player turned off
 * is dropped here, a sound of the series is played at the volume the player gave its channel, and while an alert
 * plays the series' other channels are lowered so it is heard over them. A preview from the Sound Mixer passes as it
 * is, even a sound turned off.
 *
 * <p>It stands in the game's own path rather than beside the series' calls, so a sound reaches it however it was
 * started: from a machine, from the server's packet, from a command a player typed, from another mod. It also keeps
 * what was heard lately, for the player looking for the sound that has just annoyed them.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AudioMixer {

    private static final DuckEnvelope DUCK = DuckEnvelope.standard();
    private static final Set<SoundInstance> ALERTS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final int RECENT_CAPACITY = 256;
    private static final RecentSounds RECENT = new RecentSounds(RECENT_CAPACITY);
    private static final String ALERTS_CHANNEL = AudioChannels.ALERTS.id().toString();

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

    /* Down while an alert plays, back up after; the sounds already playing are told only when the level moves. */
    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final SoundManager sounds = Minecraft.getInstance().getSoundManager();
        final boolean alerting;
        synchronized (ALERTS) {
            ALERTS.removeIf(alert -> !sounds.isActive(alert));
            alerting = !ALERTS.isEmpty();
        }
        final float before = DUCK.factor();
        if (DUCK.tick(alerting && AudioPrefsStore.prefs().ducking()) != before) {
            refreshVolumes();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        RECENT.clear();
        lastHeard = null;
        synchronized (ALERTS) {
            ALERTS.clear();
        }
    }

    /**
     * The sound as it should be heard: nothing when the player turned it off, played at its channel's volume when it
     * is the series', and the sound itself otherwise.
     */
    @Nullable
    public static SoundInstance mix(final SoundInstance sound) {
        if (sound instanceof PreviewSoundInstance) {
            return sound;
        }
        final ResourceLocation id = sound.getLocation();
        if (AudioPrefsStore.prefs().isMuted(id.toString())) {
            return null;
        }
        RECENT.heard(id.toString(), Util.getMillis());
        if (isHeardFromOutside(sound)) {
            lastHeard = id;
        }
        if (sound instanceof ScaledSoundInstance already) {
            return watched(already);
        }
        final SoundKey key = SoundKeys.find(id);
        return key == null ? sound : watched(ScaledSoundInstance.of(sound, key.spec().channel().id().toString()));
    }

    /** How much of its volume a channel keeps now: all of it for the alerts, less for the others while one plays. */
    public static float duck(final String channel) {
        return ALERTS_CHANNEL.equals(channel) ? 1.0F : DUCK.factor();
    }

    /**
     * The last sound let through that came from the world around the player, which is the one a player means when
     * they ask to turn off what they just heard: not their own footsteps, not a click of the screen in front of them.
     */
    @Nullable
    public static ResourceLocation lastHeard() {
        return lastHeard;
    }

    /** The sounds heard in the last {@code millis}, each once, newest first. */
    public static List<String> recent(final long millis) {
        return RECENT.since(Util.getMillis() - millis);
    }

    /**
     * Has every sound already playing take up its volume again, after a channel's volume or the lowering under an
     * alert changed: the game asks a sound how loud it is when it starts, and again only when told to.
     */
    public static void refreshVolumes() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().updateSourceVolume(SoundSource.BLOCKS,
                minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS));
    }

    /* An alert is kept while it plays, since the other channels stay lowered until the last one ends. */
    private static SoundInstance watched(final ScaledSoundInstance sound) {
        if (ALERTS_CHANNEL.equals(sound.channel())) {
            synchronized (ALERTS) {
                ALERTS.add(sound);
            }
        }
        return sound;
    }

    /* The player's own sounds and the interface's are what they caused, not what they are hearing around them. */
    private static boolean isHeardFromOutside(final SoundInstance sound) {
        final SoundSource source = sound.getSource();
        return source != SoundSource.PLAYERS && !(source == SoundSource.MASTER && sound.isRelative());
    }
}
