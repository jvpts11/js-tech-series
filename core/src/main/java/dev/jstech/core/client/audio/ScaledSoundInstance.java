/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * A sound as the game asked for it, played at a share of its volume: the volume the player gave its channel. It is
 * the sound in every other respect, where it comes from and what it plays included, so the game cannot tell it apart
 * from the one it asked for except by how loud it is.
 */
class ScaledSoundInstance implements SoundInstance {

    private final SoundInstance sound;
    private final float share;

    ScaledSoundInstance(final SoundInstance sound, final float share) {
        this.sound = sound;
        this.share = share;
    }

    /** The same sound at that share of its volume, still ticking when the one asked for ticks. */
    static SoundInstance of(final SoundInstance sound, final float share) {
        return sound instanceof TickableSoundInstance ticking
                ? new Ticking(ticking, share) : new ScaledSoundInstance(sound, share);
    }

    /** The sound the game asked for. */
    SoundInstance original() {
        return sound;
    }

    @Override
    public ResourceLocation getLocation() {
        return sound.getLocation();
    }

    @Nullable
    @Override
    public WeighedSoundEvents resolve(final SoundManager manager) {
        return sound.resolve(manager);
    }

    @Override
    public Sound getSound() {
        return sound.getSound();
    }

    @Override
    public SoundSource getSource() {
        return sound.getSource();
    }

    @Override
    public boolean isLooping() {
        return sound.isLooping();
    }

    @Override
    public boolean isRelative() {
        return sound.isRelative();
    }

    @Override
    public int getDelay() {
        return sound.getDelay();
    }

    @Override
    public float getVolume() {
        return sound.getVolume() * share;
    }

    @Override
    public float getPitch() {
        return sound.getPitch();
    }

    @Override
    public double getX() {
        return sound.getX();
    }

    @Override
    public double getY() {
        return sound.getY();
    }

    @Override
    public double getZ() {
        return sound.getZ();
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return sound.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return sound.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return sound.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(final SoundBufferLibrary buffers, final Sound played,
                                                    final boolean looping) {
        return sound.getStream(buffers, played, looping);
    }

    /** The same, for a sound that moves or changes as it plays and so is ticked by the game. */
    private static final class Ticking extends ScaledSoundInstance implements TickableSoundInstance {

        private final TickableSoundInstance ticking;

        Ticking(final TickableSoundInstance ticking, final float share) {
            super(ticking, share);
            this.ticking = ticking;
        }

        @Override
        public boolean isStopped() {
            return ticking.isStopped();
        }

        @Override
        public void tick() {
            ticking.tick();
        }
    }
}
