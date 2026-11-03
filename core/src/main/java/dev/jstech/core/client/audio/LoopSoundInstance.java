/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.moving.MovingStructures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A running sound the director keeps: it fades in when it starts, follows the volume and pitch its source asks for,
 * is muffled by the walls in the way, and fades out rather than stopping dead when it is no longer wanted.
 */
final class LoopSoundInstance extends AbstractTickableSoundInstance {

    private final SoundKey sound;
    private Vec3 place;
    private float wantedVolume;
    private float wantedPitch;
    private float muffle = 1.0F;
    private boolean leaving;

    /** How far the volume moves toward what is wanted in one tick, as a share of the way. */
    private static final float EASE = 0.15F;
    /** Below this the sound is as good as silent, and a sound on its way out stops. */
    private static final float SILENT = 0.002F;

    /**
     * A sound running from {@code place}, as its source gives it: a place among a moving structure's blocks is
     * followed every tick to where the structure carries it.
     */
    LoopSoundInstance(final SoundKey sound, final Vec3 place, final float volume, final float pitch) {
        super(sound.event().get(), sound.spec().channel().source(), SoundInstance.createUnseededRandom());
        this.sound = sound;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
        this.volume = 0.0F;
        this.pitch = pitch;
        this.place = place;
        follow();
        this.wantedVolume = volume;
        this.wantedPitch = pitch;
    }

    /** The sound it plays. */
    SoundKey sound() {
        return sound;
    }

    /** What its source asks for now: where it is, how loud and how high. */
    void retarget(final Vec3 at, final float volume, final float pitch) {
        this.place = at;
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
        follow();
        final float target = leaving ? 0.0F : wantedVolume * muffle;
        volume += (target - volume) * EASE;
        pitch += (wantedPitch - pitch) * EASE;
        if (leaving && volume < SILENT) {
            stop();
        }
    }

    /* Stands where its place is in the world now. */
    private void follow() {
        final Level level = Minecraft.getInstance().level;
        final Vec3 at = level == null ? this.place : MovingStructures.inWorld(level, this.place);
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
    }
}
