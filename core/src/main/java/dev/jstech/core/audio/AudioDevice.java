/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The hardware a machine's sound comes out of, and so what it can play: a PC speaker beeps square waves one at a time
 * and plays no recording; a sound card of the eighties adds a few voices and more shapes; a later one plays anything.
 * Mods register their devices in {@link AudioDevices}; a device is also a dimension of the context a cue's sound is
 * picked by ({@link SoundContext#DEVICE}), so a cue can sound different through each.
 *
 * @param id      what it is known by, {@code namespace:path}
 * @param name    what a player reads it as
 * @param waves   the shapes it can synthesise; none for a device that synthesises nothing
 * @param samples  whether it plays recordings, and not only notes
 * @param voices   how many sounds it plays at once
 * @param response what it keeps of a recording on the way out, before any speaker: an early card's coarse sampling
 * @param stereo   whether it plays a recording in two channels; one that does not mixes them before its speakers
 * @param timbre   the voice its notes have when nothing asks for a shape: the square of a PC speaker, the metallic
 *                 note of an FM chip, the rounder one of a wavetable
 */
public record AudioDevice(String id, TextKey name, Set<Waveform> waves, boolean samples, int voices,
                          FrequencyResponse response, boolean stereo, Waveform timbre) {

    /** A device that plays a recording as it was made, in stereo, its notes in the first shape it has. */
    public AudioDevice(final String id, final TextKey name, final Set<Waveform> waves, final boolean samples,
                       final int voices) {
        this(id, name, waves, samples, voices, FrequencyResponse.FULL, true);
    }

    /** A device whose notes take the first shape it has. */
    public AudioDevice(final String id, final TextKey name, final Set<Waveform> waves, final boolean samples,
                       final int voices, final FrequencyResponse response, final boolean stereo) {
        this(id, name, waves, samples, voices, response, stereo, firstOf(waves));
    }

    public AudioDevice {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty() || id.length() > SoundContext.MAX_LENGTH) {
            throw new IllegalArgumentException("a device id is 1 to " + SoundContext.MAX_LENGTH
                    + " characters: " + id);
        }
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(timbre, "timbre");
        waves = Collections.unmodifiableSet(waves.isEmpty() ? EnumSet.noneOf(Waveform.class) : EnumSet.copyOf(waves));
        if (voices < 0) {
            throw new IllegalArgumentException("a device plays no fewer than no sounds: " + voices);
        }
    }

    /** The notes of a tune, each in this device's own voice. */
    public List<Tone> voiced(final List<Tone> tones) {
        final List<Tone> out = new ArrayList<>(tones.size());
        for (final Tone tone : tones) {
            out.add(new Tone(timbre, tone.frequency(), tone.millis(), tone.volume()));
        }
        return adapt(out);
    }

    /** Whether it can make any sound at all. */
    public boolean audible() {
        return voices > 0 && (samples || !waves.isEmpty());
    }

    /**
     * The notes as this device plays them: a shape it cannot make becomes the first it can, as a PC speaker plays
     * every tune as square waves, and a device that synthesises nothing plays none of them.
     */
    public List<Tone> adapt(final List<Tone> tones) {
        if (waves.isEmpty() || voices == 0) {
            return List.of();
        }
        final Waveform fallback = waves.contains(timbre) ? timbre : waves.iterator().next();
        final List<Tone> out = new ArrayList<>(tones.size());
        for (final Tone tone : tones) {
            out.add(waves.contains(tone.wave()) ? tone
                    : new Tone(fallback, tone.frequency(), tone.millis(), tone.volume()));
        }
        return out;
    }

    private static Waveform firstOf(final Set<Waveform> waves) {
        return waves.isEmpty() ? Waveform.SQUARE : EnumSet.copyOf(waves).iterator().next();
    }
}
