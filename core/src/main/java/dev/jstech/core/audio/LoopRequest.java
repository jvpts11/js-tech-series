/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A running sound a source wants heard right now: its fan, its disk, its hum. The source says it again each time it is
 * asked, with the volume and pitch its state calls for, so a fan that speeds up with the load is a fan asked for at a
 * higher pitch; the director carries the running sound from one to the next without starting it again.
 *
 * @param sound  the looping sound
 * @param volume how loud, as a share of the sound's own level
 * @param pitch  how high, 1 being the sound as recorded
 * @param field  the ambient field it belongs to, so that many of it close together are heard as one room; or null
 */
public record LoopRequest(SoundKey sound, float volume, float pitch, @Nullable ResourceLocation field) {

    public LoopRequest {
        if (!sound.spec().loop()) {
            throw new IllegalArgumentException(sound.id() + " does not loop, so it cannot be kept running");
        }
    }

    /** A loop at its own volume and pitch, heard on its own however many there are. */
    public static LoopRequest of(final SoundKey sound) {
        return new LoopRequest(sound, 1.0F, 1.0F, null);
    }
}
