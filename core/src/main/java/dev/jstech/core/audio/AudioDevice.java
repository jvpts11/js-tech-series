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
 * @param samples whether it plays recordings, and not only notes
 * @param voices  how many sounds it plays at once
 */
public record AudioDevice(String id, TextKey name, Set<Waveform> waves, boolean samples, int voices) {

    public AudioDevice {
        waves = Collections.unmodifiableSet(waves.isEmpty() ? EnumSet.noneOf(Waveform.class) : EnumSet.copyOf(waves));
        if (voices < 0) {
            throw new IllegalArgumentException("a device plays no fewer than no sounds: " + voices);
        }
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
        final Waveform fallback = waves.iterator().next();
        final List<Tone> out = new ArrayList<>(tones.size());
        for (final Tone tone : tones) {
            out.add(waves.contains(tone.wave()) ? tone
                    : new Tone(fallback, tone.frequency(), tone.millis(), tone.volume()));
        }
        return out;
    }
}
