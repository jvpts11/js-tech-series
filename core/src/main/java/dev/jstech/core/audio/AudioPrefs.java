/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a player has asked of the series' sounds on their own machine: how loud each channel is, which sounds they
 * never want to hear, and whether alerts are also shown and walls muffle what is behind them.
 *
 * <p>It is the player's and not the world's, so it lives in a file of their own beside the game's options
 * ({@link AudioPrefsJson}). A volume is a share of the channel's sound, from nothing to all of it, and anything outside
 * that is held to the nearest end.
 */
public final class AudioPrefs {

    private final Map<String, Float> volumes = new TreeMap<>();
    private final Set<String> muted = new TreeSet<>();
    private boolean visualCues;
    private boolean occlusion = true;

    /** How loud a channel plays, from 0 to 1; 1 when the player has not said. */
    public synchronized float volume(final String channel) {
        return volumes.getOrDefault(channel, 1.0F);
    }

    /** Sets how loud a channel plays, held between 0 and 1. */
    public synchronized void setVolume(final String channel, final float volume) {
        final float held = Float.isNaN(volume) ? 1.0F : Math.max(0.0F, Math.min(1.0F, volume));
        if (held == 1.0F) {
            volumes.remove(channel);
        } else {
            volumes.put(channel, held);
        }
    }

    /** The channels the player turned down, with how far. */
    public synchronized Map<String, Float> volumes() {
        return Collections.unmodifiableMap(new TreeMap<>(volumes));
    }

    /** Whether the player never wants to hear that sound, by its id: any sound of the game, not only the series'. */
    public synchronized boolean isMuted(final String sound) {
        return muted.contains(sound);
    }

    /** Stops or starts letting that sound through. */
    public synchronized void setMuted(final String sound, final boolean mute) {
        if (mute) {
            muted.add(sound);
        } else {
            muted.remove(sound);
        }
    }

    /** The sounds the player has turned off, in order. */
    public synchronized Set<String> mutedSounds() {
        return Collections.unmodifiableSet(new TreeSet<>(muted));
    }

    /** Whether an alert is also shown, for a player who plays without sound. */
    public synchronized boolean visualCues() {
        return visualCues;
    }

    public synchronized void setVisualCues(final boolean show) {
        this.visualCues = show;
    }

    /** Whether walls muffle a sound on the other side of them. */
    public synchronized boolean occlusion() {
        return occlusion;
    }

    public synchronized void setOcclusion(final boolean muffle) {
        this.occlusion = muffle;
    }
}
