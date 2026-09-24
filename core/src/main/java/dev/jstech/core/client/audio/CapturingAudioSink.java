/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * A sink that plays nothing and writes down everything it was asked to play, after the mixer had its say, so a test
 * can check which sounds would have been heard, how loud, and that a muted one was not.
 */
public final class CapturingAudioSink implements IAudioSink {

    private final List<SoundInstance> played = new ArrayList<>();
    private final List<SoundInstance> stopped = new ArrayList<>();

    /*
     * As the game's engine does: the mixer first, then the sound picks which of its files plays, which is also what
     * gives it a volume to read.
     */
    @Override
    public synchronized void play(final SoundInstance sound) {
        final SoundInstance mixed = AudioMixer.mix(sound);
        if (mixed != null) {
            mixed.resolve(Minecraft.getInstance().getSoundManager());
            played.add(mixed);
        }
    }

    @Override
    public synchronized void stop(final SoundInstance sound) {
        stopped.add(sound);
    }

    /** What would have been heard, in order, as the mixer let it through. */
    public synchronized List<SoundInstance> played() {
        return List.copyOf(played);
    }

    /** What was asked to stop, in order. */
    public synchronized List<SoundInstance> stopped() {
        return List.copyOf(stopped);
    }
}
