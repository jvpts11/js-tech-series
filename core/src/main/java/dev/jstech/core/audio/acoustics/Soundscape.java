/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

/**
 * One sound of a place's ambience: the wind in a forest, rain on a roof, a cave's drips, the hum of a factory. It runs
 * while its conditions hold where the listener is, fading in and out, heard all round the listener rather than from
 * a place. Soundscapes are resource pack files, {@code assets/<namespace>/soundscapes/<path>.json}, so a pack changes
 * them.
 *
 * @param id         its id, from its file
 * @param sound      the sound it plays, looping
 * @param volume     how loud, from 0 to 1
 * @param conditions where and when it is heard
 */
public record Soundscape(String id, String sound, float volume, SoundConditions conditions) {

    public Soundscape {
        if (volume < 0 || volume > 1) {
            throw new IllegalArgumentException("the soundscape " + id + " plays at " + volume);
        }
    }
}
