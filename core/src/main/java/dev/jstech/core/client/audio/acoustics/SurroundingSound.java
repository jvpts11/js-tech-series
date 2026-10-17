/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.client.audio.AudioPrefsStore;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A sound all round the listener rather than from a place, running on: an ambience, or a layer of music. It fades in
 * when it starts and toward the volume asked of it as that changes, and fades out rather than stopping dead when it
 * is no longer wanted, at the volume the player gave its channel on the Sound Mixer.
 */
final class SurroundingSound extends AbstractTickableSoundInstance {

    private final String channel;
    private float wanted;
    private float level;

    /** How far the volume moves toward what is wanted in one tick, as a share of the way. */
    private static final float EASE = 0.04F;
    /** Below this the sound is as good as silent, and a sound on its way out stops. */
    private static final float SILENT = 0.002F;

    SurroundingSound(final ResourceLocation sound, final SoundSource source, final String channel,
                     final float volume) {
        super(SoundEvent.createVariableRangeEvent(sound), source, SoundInstance.createUnseededRandom());
        this.channel = channel;
        this.wanted = volume;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = SILENT;
    }

    /** Asks it to play at {@code volume}, toward which it fades; 0 to fade it out and stop it. */
    void want(final float volume) {
        this.wanted = volume;
    }

    /** Whether it is fading out, wanted no more. */
    boolean leaving() {
        return this.wanted <= 0;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        this.level += (this.wanted - this.level) * EASE;
        this.volume = Math.max(SILENT, this.level * AudioPrefsStore.prefs().volume(this.channel));
        if (this.wanted <= 0 && this.level < SILENT) {
            stop();
        }
    }
}
