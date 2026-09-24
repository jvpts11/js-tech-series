/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * A sound played once for the player to recognise it, from the Sound Mixer's list: from their own screen, at its whole
 * volume, and heard even when the player has turned it off, since hearing it is how they decide.
 */
final class PreviewSoundInstance extends SimpleSoundInstance {

    PreviewSoundInstance(final ResourceLocation id) {
        super(id, SoundSource.MASTER, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
    }
}
