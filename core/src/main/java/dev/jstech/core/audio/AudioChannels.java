/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * The channels a sound of the series can be mixed in, the series' own and any an addon declares.
 *
 * <p>Machines are what runs: fans, disks, the hum of a rack. Devices are what a player works: a drive's tray, a disc
 * going in, a rail taking a server. The interface is what a screen says back to whoever uses it. Alerts are what
 * asks to be heard over the rest. Ambience is the room itself, music is music, and voice is people and radios.
 */
@TextHolder
public final class AudioChannels {

    private static final TextKey MACHINES_NAME = TextKey.of("jscore.audio_channel.machines", "Machines");
    private static final TextKey DEVICES_NAME = TextKey.of("jscore.audio_channel.devices", "Devices");
    private static final TextKey INTERFACE_NAME = TextKey.of("jscore.audio_channel.interface", "Interface");
    private static final TextKey ALERTS_NAME = TextKey.of("jscore.audio_channel.alerts", "Alerts");
    private static final TextKey AMBIENCE_NAME = TextKey.of("jscore.audio_channel.ambience", "Ambience");
    private static final TextKey MUSIC_NAME = TextKey.of("jscore.audio_channel.music", "Music");
    private static final TextKey VOICE_NAME = TextKey.of("jscore.audio_channel.voice", "Voice");

    private static final Map<ResourceLocation, AudioChannel> CHANNELS = new LinkedHashMap<>();

    public static final AudioChannel MACHINES = register(channel("machines", SoundSource.BLOCKS, MACHINES_NAME));
    public static final AudioChannel DEVICES = register(channel("devices", SoundSource.BLOCKS, DEVICES_NAME));
    public static final AudioChannel INTERFACE = register(channel("interface", SoundSource.MASTER, INTERFACE_NAME));
    public static final AudioChannel ALERTS = register(channel("alerts", SoundSource.BLOCKS, ALERTS_NAME));
    public static final AudioChannel AMBIENCE = register(channel("ambience", SoundSource.AMBIENT, AMBIENCE_NAME));
    public static final AudioChannel MUSIC = register(channel("music", SoundSource.MUSIC, MUSIC_NAME));
    public static final AudioChannel VOICE = register(channel("voice", SoundSource.VOICE, VOICE_NAME));

    private AudioChannels() {
    }

    /**
     * Adds a channel, which a sound may then be declared in and a player may then set the volume of. The same id twice
     * is refused, so two mods cannot quietly share a slider.
     */
    public static synchronized AudioChannel register(final AudioChannel channel) {
        if (CHANNELS.putIfAbsent(channel.id(), channel) != null) {
            throw new IllegalStateException("the audio channel " + channel.id() + " is declared twice");
        }
        return channel;
    }

    /** Every channel, in the order they were declared. */
    public static synchronized Collection<AudioChannel> all() {
        return Collections.unmodifiableCollection(CHANNELS.values());
    }

    /** The channel with that id, or null when nothing declared one. */
    @Nullable
    public static synchronized AudioChannel find(final ResourceLocation id) {
        return CHANNELS.get(id);
    }

    private static AudioChannel channel(final String path, final SoundSource source, final TextKey name) {
        return new AudioChannel(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), source, name);
    }
}
