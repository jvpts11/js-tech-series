/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.SoundKey;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * A running sound the director keeps: it fades in when it starts, follows the volume and pitch its source asks for,
 * is muffled by the walls in the way, and fades out rather than stopping dead when it is no longer wanted.
 */
final class LoopSoundInstance extends AbstractTickableSoundInstance {

    private final SoundKey sound;
    private float wantedVolume;
    private float wantedPitch;
    private float muffle = 1.0F;
    private boolean leaving;

    /** How far the volume moves toward what is wanted in one tick, as a share of the way. */
    private static final float EASE = 0.15F;
    /** Below this the sound is as good as silent, and a sound on its way out stops. */
    private static final float SILENT = 0.002F;

    LoopSoundInstance(final SoundKey sound, final double x, final double y, final double z, final float volume,
                      final float pitch) {
        super(sound.event().get(), sound.spec().channel().source(), SoundInstance.createUnseededRandom());
        this.sound = sound;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
        this.volume = 0.0F;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.wantedVolume = volume;
        this.wantedPitch = pitch;
    }

    /** The sound it plays. */
    SoundKey sound() {
        return sound;
    }

    /** What its source asks for now: where it is, how loud and how high. */
    void retarget(final double atX, final double atY, final double atZ, final float volume, final float pitch) {
        this.x = atX;
        this.y = atY;
        this.z = atZ;
        this.wantedVolume = volume;
        this.wantedPitch = pitch;
        this.leaving = false;
    }

    /** How much of it gets through the walls between it and the listener now. */
    float muffled() {
        return muffle;
    }

    /** How much of it gets through the walls between it and the listener. */
    void muffle(final float share) {
        this.muffle = share;
    }

    /** Fades it out; it stops by itself once it is silent. */
    void leave() {
        this.leaving = true;
    }

    /** Whether it is on its way out. */
    boolean leaving() {
        return leaving;
    }

    /** Whether it has finished, faded out or dropped by the game, and so needs starting again to be heard. */
    boolean finished() {
        return isStopped();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        final float target = leaving ? 0.0F : wantedVolume * muffle;
        volume += (target - volume) * EASE;
        pitch += (wantedPitch - pitch) * EASE;
        if (leaving && volume < SILENT) {
            stop();
        }
    }
}
