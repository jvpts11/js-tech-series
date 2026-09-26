/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;

import java.util.Objects;

/**
 * A sound card: what a Vintage or Legacy computer plays its system's sounds, its music and its programs through,
 * beyond the beeps of the speaker inside its case. It sits only on a board of its own era, in a slot of the bus
 * that board has; a Standard board has its sound built in and takes none.
 *
 * @param era        the age of machine it goes into
 * @param bus        the slot it takes
 * @param tdpWatts   what it draws
 * @param synthesis  how it makes notes
 * @param voices     how many notes it plays at once
 * @param sampleBits how finely it plays a recording, in bits per sample
 * @param stereo     whether it plays a recording in two channels rather than one
 * @param rate       how often it samples a recording
 */
public record SoundCardSpec(HardwareEra era, PcieGeneration bus, int tdpWatts, Synthesis synthesis, int voices,
                            int sampleBits, boolean stereo, SampleRate rate) implements IExpansionCardSpec {

    public SoundCardSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(synthesis, "synthesis must not be null");
        Objects.requireNonNull(rate, "rate must not be null");
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
        if (voices < 1) {
            throw new IllegalArgumentException("voices must be >= 1; got " + voices);
        }
        if (sampleBits != 8 && sampleBits != 16) {
            throw new IllegalArgumentException("sampleBits must be 8 or 16; got " + sampleBits);
        }
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.SOUND;
    }

    /** How a card makes the notes a program asks it for. */
    public enum Synthesis {
        /** Frequency modulation: a few operators bending one another, the sound of the early cards. */
        FM,
        /** Recorded instruments played back at the pitch asked for. */
        WAVETABLE
    }

    /** How often a card samples a recording: the two rates the cards of those ages played at. */
    public enum SampleRate {
        KHZ_22(22_050),
        KHZ_44(44_100);

        private final int hertz;

        SampleRate(final int hertz) {
            this.hertz = hertz;
        }

        public int hertz() {
            return hertz;
        }
    }
}
