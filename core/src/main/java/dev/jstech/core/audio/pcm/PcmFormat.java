/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

/**
 * The shape of a stream of samples: how many a second, and how many channels side by side. Every sample is a signed
 * sixteen-bit number, which is what the game's speakers take, so a decoder turns whatever it reads into that.
 *
 * @param sampleRate how many samples a second each channel has
 * @param channels   one for mono, two for stereo
 */
public record PcmFormat(int sampleRate, int channels) {

    /** The rate the synthesiser plays at: enough for a beep, a chime or a tune, and light to make. */
    public static final PcmFormat SYNTH = new PcmFormat(22_050, 1);

    public PcmFormat {
        if (sampleRate < 1_000 || sampleRate > 192_000) {
            throw new IllegalArgumentException("a sample rate between 1 and 192 kHz: " + sampleRate);
        }
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("one or two channels: " + channels);
        }
    }
}
