/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.api;

import dev.jstech.core.audio.AudioChannel;
import dev.jstech.core.audio.AudioChannels;

/**
 * The groups J's Industrial's sounds are kept in, each mixed in one of J's Core's channels, so a player sets their
 * volume where they set every other machine's or alarm's. The sounds are real recordings chosen to fit, each in the
 * repository's register of assets with its source and its licence; a sound of an add-on joins the group it belongs to.
 *
 * <p>Two rules of the real world come with the groups: a machine drawing more power sounds louder, and an alarm carries
 * far, to warn in time.
 */
public enum IndustrialSoundGroup {

    /** Each machine like the real one: a crusher's jaws, a press's stroke, a turbine's whine. */
    MACHINES(AudioChannels.MACHINES, true, false),
    /** The hum of transformers, an arc's crack, a shock, a breaker. */
    POWER(AudioChannels.MACHINES, false, false),
    /** The hiss of compressed air, a compressor's thump, tanks bubbling, a leak, a relief valve's whistle. */
    AIR_AND_FLUIDS(AudioChannels.MACHINES, false, false),
    /** Sirens, the gas alarm, a Geiger counter's clicks, the dose alarm. */
    SAFETY(AudioChannels.ALERTS, false, true),
    /** The white noise of the fan filter units, the air shower's blast. */
    CLEAN_ROOM(AudioChannels.AMBIENCE, false, false),
    /** The radio-frequency hum of accelerators, the beeps of instruments. */
    SCIENCE(AudioChannels.MACHINES, false, false),
    /** The sonifications of space, for singularities and what lies past them. */
    TRANSCENDENCE(AudioChannels.AMBIENCE, false, false);

    private final AudioChannel channel;
    private final boolean followsPower;
    private final boolean carriesFar;

    IndustrialSoundGroup(final AudioChannel channel, final boolean followsPower, final boolean carriesFar) {
        this.channel = channel;
        this.followsPower = followsPower;
        this.carriesFar = carriesFar;
    }

    /** The channel of J's Core its sounds are mixed in. */
    public AudioChannel channel() {
        return this.channel;
    }

    /** Whether its sounds grow louder as what makes them draws more power. */
    public boolean followsPower() {
        return this.followsPower;
    }

    /** Whether its sounds carry far, to warn in time. */
    public boolean carriesFar() {
        return this.carriesFar;
    }
}
