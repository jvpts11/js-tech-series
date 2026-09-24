/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.audio.SoundSpace;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * How a client plays the series' sounds itself: a program answering a click, a screen opening. Whatever the server
 * plays arrives here too when it belongs to the interface. Everything goes to one sink, which is the game's sound
 * manager outside a test and a recording one inside it.
 */
public final class AudioEngine {

    private static final IAudioSink GAME = new GameAudioSink();

    private static volatile IAudioSink sink = GAME;

    private AudioEngine() {
    }

    /** Plays a sound of the interface, from the player's own screen, as loud and as high as it was recorded. */
    public static void playOnScreen(final SoundKey sound) {
        playOnScreen(sound, 1.0F, 1.0F);
    }

    /**
     * Plays a sound of the interface from the player's own screen.
     *
     * @throws IllegalArgumentException when the sound belongs to the world and not to the interface
     */
    public static void playOnScreen(final SoundKey sound, final float volume, final float pitch) {
        if (sound.spec().space() != SoundSpace.INTERFACE) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the world, not of the interface");
        }
        sink.play(SimpleSoundInstance.forUI(sound.event().get(), pitch, volume));
    }

    /** Plays the declared sound of the interface with that id, as the server asked; an unknown id is ignored. */
    public static void playOnScreen(final ResourceLocation id, final float volume, final float pitch) {
        final SoundKey sound = SoundKeys.find(id);
        if (sound != null && sound.spec().space() == SoundSpace.INTERFACE) {
            playOnScreen(sound, volume, pitch);
        }
    }

    /** Stops a sound this engine started. */
    public static void stop(final SoundInstance sound) {
        sink.stop(sound);
    }

    /** Sends everything to that sink instead of the game's, for a test; {@link #restoreSink()} puts the game's back. */
    public static void useSink(final IAudioSink testSink) {
        sink = testSink;
    }

    /** Puts the game's own sound manager back as the sink. */
    public static void restoreSink() {
        sink = GAME;
    }
}
