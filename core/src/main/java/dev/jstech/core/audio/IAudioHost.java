/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Something that plays sound through hardware of its own: a computer with its sound card, its volume and the speakers
 * wired to it. {@code Audio.cue} given a host plays a cue through it: from each of its speakers, at its volume,
 * picked by its context with its device in it, and not at all when its device makes no sound. A block that is one
 * says so through {@link AudioHosts#CAPABILITY}.
 */
public interface IAudioHost {

    /** The device its sound comes out of, which decides what it can play; {@link AudioDevices#NONE} for none. */
    AudioDevice audioDevice();

    /** How loud it plays, from 0 to 1: its own volume. */
    float audioVolume();

    /** Where its sound comes from: its speakers, or itself when it has none wired. Never empty. */
    List<Vec3> audioOutputs();

    /**
     * Where its sound comes from and how each place plays it: which side of a stereo recording, and what it can
     * reproduce. A host whose speakers all play a recording whole needs say no more than {@link #audioOutputs()}.
     */
    default List<AudioOutput> outputs() {
        return audioOutputs().stream().map(AudioOutput::at).toList();
    }

    /** What its sounds are picked by (its era, its system's family); its device is added to it when it plays. */
    SoundContext soundContext();
}
