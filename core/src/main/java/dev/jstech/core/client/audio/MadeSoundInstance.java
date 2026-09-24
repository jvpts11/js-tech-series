/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.pcm.IPcmOpener;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * A declared sound made as it plays, from samples handed over for this one playing: a synthesised tune, a recording
 * from a disk. To the game it is a streamed sound like its music, whose file it asks this instance to open, so it goes
 * through the mixer, the subtitles and the player's volumes as any other does.
 *
 * <p>The samples are opened only when the game is about to play them, off the game's thread as its own files are,
 * so a muted sound, or one that found no free speaker, never opens its file at all.
 */
final class MadeSoundInstance extends AbstractSoundInstance {

    private final SoundKey key;
    private final IPcmOpener samples;
    private final boolean onScreen;

    MadeSoundInstance(final SoundKey key, final IPcmOpener samples, final float volume, final boolean onScreen,
                      final double x, final double y, final double z) {
        super(key.id(), key.spec().channel().source(), SoundInstance.createUnseededRandom());
        this.key = key;
        this.samples = samples;
        this.onScreen = onScreen;
        this.volume = volume;
        if (onScreen) {
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        } else {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /* There is no entry in sounds.json to pick from: the one sound it has is made here, streamed, with its range. */
    @Override
    public WeighedSoundEvents resolve(final SoundManager manager) {
        this.sound = new Sound(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true,
                false, key.spec().range());
        return new WeighedSoundEvents(location, key.subtitle().key());
    }

    @Override
    public CompletableFuture<AudioStream> getStream(final SoundBufferLibrary buffers, final Sound played,
                                                    final boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new PcmAudioStream(samples.open(), !onScreen);
            } catch (final IOException unreadable) {
                throw new CompletionException(unreadable);
            }
        }, Util.nonCriticalIoPool());
    }
}
