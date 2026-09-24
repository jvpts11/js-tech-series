/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Where the audio engine sends what it plays: the game's own sound manager, or, in a test, something that only writes
 * down what would have been heard, so a test can say which sounds played without a speaker.
 */
public interface IAudioSink {

    /** Starts a sound. */
    void play(SoundInstance sound);

    /** Stops a sound that is playing; nothing happens when it is not. */
    void stop(SoundInstance sound);
}
