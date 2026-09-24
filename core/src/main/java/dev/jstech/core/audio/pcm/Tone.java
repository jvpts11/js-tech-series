/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

/**
 * One note of a synthesised sound: a shape, a pitch, how long and how loud; a pitch of nothing is a rest.
 *
 * @param wave      the shape of the wave
 * @param frequency the pitch in hertz, or 0 for silence
 * @param millis    how long it lasts
 * @param volume    how loud, from 0 to 1
 */
public record Tone(Waveform wave, double frequency, int millis, float volume) {

    public Tone {
        if (frequency < 0 || frequency > 20_000) {
            throw new IllegalArgumentException("a pitch a speaker can make: " + frequency);
        }
        if (millis < 1 || millis > 60_000) {
            throw new IllegalArgumentException("a note of a millisecond to a minute: " + millis);
        }
        volume = Math.max(0.0F, Math.min(1.0F, volume));
    }

    /** A square beep, the only sound a PC speaker makes. */
    public static Tone beep(final double frequency, final int millis) {
        return new Tone(Waveform.SQUARE, frequency, millis, 0.5F);
    }

    /** A rest of that length. */
    public static Tone rest(final int millis) {
        return new Tone(Waveform.SQUARE, 0, millis, 0.0F);
    }
}
