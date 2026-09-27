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
import dev.jstech.core.audio.pcm.PcmSkip;
import dev.jstech.core.audio.pcm.ResponseFilter;
import dev.jstech.core.audio.pcm.StereoSelect;
import dev.jstech.core.client.audio.AudioEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            for (final MediaPlace place : payload.places()) {
                sounds.add(AudioEngine.playMade(sound, opener(file, payload.media().fileName(), offset, place),
                        place.x(), place.y(), place.z(), payload.volume() * place.gain()));
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
        final Playing playing = PLAYING.remove(key);
        if (playing != null) {
            playing.sounds.forEach(AudioEngine::stop);
        }
    }

    /* The recording's samples from that point, as that place plays them: its side, and what it keeps of them. */
    private static IPcmOpener opener(final Path file, final String name, final long offset, final MediaPlace place) {
        return () -> ResponseFilter.of(StereoSelect.of(PcmSkip.from(AudioDecoders.open(name,
                Files.newInputStream(file)), offset), place.side()), place.response());
    }

    /** One recording's sounds, one for each place it is heard from. */
    private record Playing(List<SoundInstance> sounds) {
    }
}
