/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

/**
 * What a recording is, read from its file without playing it: how long it runs, its rate and its channels, how many
 * kilobits a second of it take, and what it says about itself.
 *
 * @param millis     how long it runs, in milliseconds
 * @param sampleRate its samples a second, per channel
 * @param channels   1 for mono, 2 for stereo
 * @param kbps       the kilobits a second of it take, as a player of the time showed it
 * @param tags       what it says about itself
 */
public record MediaInfo(long millis, int sampleRate, int channels, int kbps, MediaTags tags) {

    public MediaInfo {
        millis = Math.max(0L, millis);
        tags = tags == null ? MediaTags.EMPTY : tags;
    }

    /** Whether it is in stereo. */
    public boolean stereo() {
        return channels >= 2;
    }
}
