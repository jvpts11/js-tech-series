/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.media;

import com.mojang.logging.LogUtils;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.audio.SoundSpace;
import dev.jstech.core.audio.media.MediaPlace;
import dev.jstech.core.audio.media.MediaPlayPayload;
import dev.jstech.core.audio.pcm.AudioDecoders;
import dev.jstech.core.audio.pcm.IPcmOpener;
import dev.jstech.core.audio.pcm.IPcmSource;
import dev.jstech.core.audio.pcm.PcmSkip;
import dev.jstech.core.audio.pcm.PcmTap;
import dev.jstech.core.audio.pcm.ResponseFilter;
import dev.jstech.core.audio.pcm.Spectrum;
import dev.jstech.core.audio.pcm.StereoSelect;
import dev.jstech.core.client.audio.AudioEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.slf4j.Logger;

/**
 * The recordings playing near this player, as the server says: each out of every place it is heard from, from the
 * point it had reached, and further in by however long fetching it took. A recording replaced or stopped before it
 * arrived is not played when it does.
 */
public final class MediaPlayer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, Playing> PLAYING = new HashMap<>();
    /** How often each key was started or stopped, so a recording that arrives late knows it is no longer wanted. */
    private static final Map<String, Integer> TURNS = new HashMap<>();
    /** What each key is being heard as, listened in on at its first place; opened on the sound engine's thread. */
    private static final Map<String, PcmTap> TAPS = new ConcurrentHashMap<>();
    /** The stretch of samples the analyser reads, kept to be filled again every frame. */
    private static final ThreadLocal<float[]> WINDOW = ThreadLocal.withInitial(() -> new float[0]);

    private MediaPlayer() {
    }

    /** A recording starts near the player, or starts again from another point or out of other places. */
    public static void onPlay(final MediaPlayPayload payload) {
        stop(payload.key());
        final int turn = TURNS.merge(payload.key(), 1, Integer::sum);
        final SoundKey sound = SoundKeys.find(payload.sound());
        if (sound == null || !sound.spec().made() || sound.spec().space() != SoundSpace.WORLD) {
            LOGGER.warn("{} is not a sound a recording can be played as in the world", payload.sound());
            return;
        }
        final long receivedAt = Util.getMillis();
        MediaCache.fetch(payload.media(), file -> {
            if (TURNS.getOrDefault(payload.key(), 0) != turn) {
                return;
            }
            final long offset = payload.offsetMillis() + (Util.getMillis() - receivedAt);
            final List<SoundInstance> sounds = new ArrayList<>();
            boolean tapped = false;
            for (final MediaPlace place : payload.places()) {
                // The first place is listened in on, for a screen that shows what is being heard.
                sounds.add(AudioEngine.playMade(sound,
                        opener(payload.key(), file, payload.media().fileName(), offset, place, !tapped),
                        place.x(), place.y(), place.z(), payload.volume() * place.gain()));
                tapped = true;
            }
            PLAYING.put(payload.key(), new Playing(sounds));
        });
    }

    /** A recording stops for this player. */
    public static void onStop(final String key) {
        TURNS.merge(key, 1, Integer::sum);
        stop(key);
    }

    /** The keys of what is playing for this player, for a test. */
    public static Set<String> playing() {
        return Set.copyOf(PLAYING.keySet());
    }

    /** Whether what plays under the key is still being heard from any of its places. */
    public static boolean heard(final String key) {
        final Playing playing = PLAYING.get(key);
        return playing != null && playing.sounds.stream().anyMatch(AudioEngine::isPlaying);
    }

    /** The player left the server: everything stops. */
    public static void clear() {
        for (final String key : List.copyOf(PLAYING.keySet())) {
            stop(key);
        }
        TURNS.clear();
    }

    private static void stop(final String key) {
        TAPS.remove(key);
        final Playing playing = PLAYING.remove(key);
        if (playing != null) {
            playing.sounds.forEach(AudioEngine::stop);
        }
    }

    /**
     * How loud what plays under the key is being heard in each band from the bass to the treble, into {@code bands},
     * each from 0 to 1; see {@link Spectrum}.
     *
     * @return whether it is being heard at all
     */
    public static boolean levels(final String key, final float[] bands) {
        final PcmTap tap = TAPS.get(key);
        if (tap == null || !heard(key) || !tap.heard(window(tap.rate()))) {
            Arrays.fill(bands, 0.0F);
            return false;
        }
        Spectrum.bands(WINDOW.get(), tap.rate(), bands);
        return true;
    }

    /* The recording's samples from that point, as that place plays them: its side, and what it keeps of them. */
    private static IPcmOpener opener(final String key, final Path file, final String name, final long offset,
                                     final MediaPlace place, final boolean tap) {
        return () -> {
            final IPcmSource heard = ResponseFilter.of(StereoSelect.of(PcmSkip.from(AudioDecoders.open(name,
                    Files.newInputStream(file)), offset), place.side()), place.response());
            if (!tap) {
                return heard;
            }
            final PcmTap listening = new PcmTap(heard, Util::getMillis);
            TAPS.put(key, listening);
            return listening;
        };
    }

    /* A stretch of about a twentieth of a second, which is short enough to follow the beat. */
    private static float[] window(final int rate) {
        final int length = Math.max(256, Integer.highestOneBit(Math.max(1, rate / 20)));
        float[] window = WINDOW.get();
        if (window.length != length) {
            window = new float[length];
            WINDOW.set(window);
        }
        return window;
    }

    /** One recording's sounds, one for each place it is heard from. */
    private record Playing(List<SoundInstance> sounds) {
    }
}
