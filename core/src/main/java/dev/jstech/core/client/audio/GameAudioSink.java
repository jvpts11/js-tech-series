/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/** The game's own sound manager, which is where every sound goes outside a test. */
final class GameAudioSink implements IAudioSink {

    @Override
    public void play(final SoundInstance sound) {
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @Override
    public void stop(final SoundInstance sound) {
        Minecraft.getInstance().getSoundManager().stop(sound);
    }
}
