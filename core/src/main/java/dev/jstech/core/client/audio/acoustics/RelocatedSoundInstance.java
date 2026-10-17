/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * A sound the game asked for, playing another sound in its place: a step on wool a pack gave a softer sound, a
 * launch heard from far off as its rumble. Where it comes from, how loud and how high stay the first sound's; only
 * what it plays changes.
 */
final class RelocatedSoundInstance implements SoundInstance {

    private final SoundInstance asked;
    private final ResourceLocation playing;
    private final RandomSource random = SoundInstance.createUnseededRandom();
    private @Nullable Sound sound;

    RelocatedSoundInstance(final SoundInstance asked, final ResourceLocation playing) {
        this.asked = asked;
        this.playing = playing;
    }

    @Override
    public ResourceLocation getLocation() {
        return this.playing;
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(final SoundManager manager) {
        // The sound asked for is resolved too, so its loudness and pitch can be read.
        this.asked.resolve(manager);
        final WeighedSoundEvents events = manager.getSoundEvent(this.playing);
        this.sound = events == null ? SoundManager.EMPTY_SOUND : events.getSound(this.random);
        return events;
    }

    @Override
    public Sound getSound() {
        return this.sound == null ? SoundManager.EMPTY_SOUND : this.sound;
    }

    @Override
    public SoundSource getSource() {
        return this.asked.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.asked.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.asked.isRelative();
    }

    @Override
    public int getDelay() {
        return this.asked.getDelay();
    }

    @Override
    public float getVolume() {
        return this.asked.getVolume();
    }

    @Override
    public float getPitch() {
        return this.asked.getPitch();
    }

    @Override
    public double getX() {
        return this.asked.getX();
    }

    @Override
    public double getY() {
        return this.asked.getY();
    }

    @Override
    public double getZ() {
        return this.asked.getZ();
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return this.asked.getAttenuation();
    }
}
