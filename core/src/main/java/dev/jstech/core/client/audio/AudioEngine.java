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
import dev.jstech.core.audio.ToneSoundPayload;
import dev.jstech.core.audio.pcm.IPcmOpener;
import dev.jstech.core.audio.pcm.SynthSource;
import java.util.Locale;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * How a client plays the series' sounds itself: a program answering a click, a screen opening, a tune a program
 * plays, a recording from a disk. Whatever the server plays arrives here too when it belongs to the interface or is
 * made as it plays. Everything goes to one sink, which is the game's sound manager outside a test and a recording one
 * inside it.
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
     * @throws IllegalArgumentException when the sound belongs to the world, or is made as it plays
     */
    public static void playOnScreen(final SoundKey sound, final float volume, final float pitch) {
        if (sound.spec().made()) {
            throw new IllegalArgumentException(sound.id() + " is made as it plays, so it is played from samples");
        }
        requireSpace(sound, SoundSpace.INTERFACE);
        sink.play(SimpleSoundInstance.forUI(sound.event().get(), pitch, volume));
    }

    /** Plays the declared sound of the interface with that id, as the server asked; an unknown id is ignored. */
    public static void playOnScreen(final ResourceLocation id, final float volume, final float pitch) {
        final SoundKey sound = SoundKeys.find(id);
        if (sound != null && !sound.spec().made() && sound.spec().space() == SoundSpace.INTERFACE) {
            playOnScreen(sound, volume, pitch);
        }
    }

    /**
     * Plays samples as a declared sound made as it plays, from that point in the world: a speaker playing a
     * recording, a machine's chime. A stereo recording is heard in mono, which is how a sound is placed in the world.
     *
     * @return the sound as it plays, which {@link #stop} stops before its end
     * @throws IllegalArgumentException when the sound is not a sound of the world made as it plays
     */
    public static SoundInstance playMade(final SoundKey sound, final IPcmOpener samples, final double x,
                                         final double y, final double z, final float volume) {
        requireMade(sound);
        requireSpace(sound, SoundSpace.WORLD);
        final SoundInstance playing = new MadeSoundInstance(sound, samples, volume, false, x, y, z);
        sink.play(playing);
        return playing;
    }

    /**
     * Plays samples as a declared sound made as it plays, from the player's own screen.
     *
     * @return the sound as it plays, which {@link #stop} stops before its end
     * @throws IllegalArgumentException when the sound is not a sound of the interface made as it plays
     */
    public static SoundInstance playMadeOnScreen(final SoundKey sound, final IPcmOpener samples, final float volume) {
        requireMade(sound);
        requireSpace(sound, SoundSpace.INTERFACE);
        final SoundInstance playing = new MadeSoundInstance(sound, samples, volume, true, 0, 0, 0);
        sink.play(playing);
        return playing;
    }

    /** Synthesises the notes the server sent; a sound this client does not know as one made as it plays is ignored. */
    public static void playTones(final ToneSoundPayload payload) {
        final SoundKey sound = SoundKeys.find(payload.sound());
        if (sound == null || !sound.spec().made() || payload.tones().isEmpty()) {
            return;
        }
        final IPcmOpener notes = () -> new SynthSource(payload.tones());
        if (payload.onScreen() && sound.spec().space() == SoundSpace.INTERFACE) {
            playMadeOnScreen(sound, notes, 1.0F);
        } else if (!payload.onScreen() && sound.spec().space() == SoundSpace.WORLD) {
            playMade(sound, notes, payload.x(), payload.y(), payload.z(), 1.0F);
        }
    }

    /** Stops a sound this engine started. */
    public static void stop(final SoundInstance sound) {
        sink.stop(sound);
    }

    /** Starts a sound the engine's own parts made, the director's running sounds, through the same sink. */
    static void play(final SoundInstance sound) {
        sink.play(sound);
    }

    /** Sends everything to that sink instead of the game's, for a test; {@link #restoreSink()} puts the game's back. */
    public static void useSink(final IAudioSink testSink) {
        sink = testSink;
    }

    /** Puts the game's own sound manager back as the sink. */
    public static void restoreSink() {
        sink = GAME;
    }

    private static void requireMade(final SoundKey sound) {
        if (!sound.spec().made()) {
            throw new IllegalArgumentException(sound.id() + " plays its own files, not samples it is handed");
        }
    }

    private static void requireSpace(final SoundKey sound, final SoundSpace space) {
        if (sound.spec().space() != space) {
            throw new IllegalArgumentException(sound.id() + " is a sound of the "
                    + sound.spec().space().name().toLowerCase(Locale.ROOT) + ", not of the "
                    + space.name().toLowerCase(Locale.ROOT));
        }
    }
}
