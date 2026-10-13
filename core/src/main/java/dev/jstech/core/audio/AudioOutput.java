/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * One place a host's sound comes out of, and how: which side of a stereo recording it plays, and what it can
 * reproduce. A monitor plays a recording whole; one speaker of a pair plays its side; a cheap speaker loses the bass
 * and the treble.
 *
 * @param position where the sound comes from
 * @param side     which side of a stereo recording it plays
 * @param response what it can reproduce
 */
public record AudioOutput(Vec3 position, StereoSide side, FrequencyResponse response) {

    public AudioOutput {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(response, "response");
    }

    /** A place that plays a recording whole, both sides together: a monitor, a machine's own case. */
    public static AudioOutput at(final Vec3 position) {
        return new AudioOutput(position, StereoSide.BOTH, FrequencyResponse.FULL);
    }

    /** Whether it plays a recording as it was made, both sides together and nothing taken away. */
    public boolean whole() {
        return side == StereoSide.BOTH && response.full();
    }
}
