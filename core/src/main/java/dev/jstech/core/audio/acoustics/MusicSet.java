/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import java.util.List;

/**
 * Music that follows the moment: layered tracks played together from the same start, each layer coming in and going
 * out with its own conditions, so the music grows as a moment does (the drums when night falls on a new world) rather
 * than cutting to another song. While a set plays the game's own music waits. Music sets are resource pack files,
 * {@code assets/<namespace>/music_sets/<path>.json}.
 *
 * @param id         its id, from its file
 * @param priority   which set plays when several could, higher first
 * @param conditions where and when it plays at all
 * @param layers     its tracks, played together
 */
public record MusicSet(String id, int priority, SoundConditions conditions, List<Layer> layers) {

    public MusicSet {
        layers = List.copyOf(layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("the music set " + id + " has no layer");
        }
    }

    /**
     * One track of a set.
     *
     * @param sound      the sound it plays, looping, in step with the set's other tracks
     * @param volume     how loud it plays while heard, from 0 to 1
     * @param conditions when it is heard, within its set's
     */
    public record Layer(String sound, float volume, SoundConditions conditions) {

        public Layer {
            if (volume < 0 || volume > 1) {
                throw new IllegalArgumentException("the layer " + sound + " plays at " + volume);
            }
        }
    }
}
