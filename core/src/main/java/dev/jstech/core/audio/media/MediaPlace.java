/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.StereoSide;

/**
 * One place a recording is heard from: a monitor or a speaker of the machine playing it, with the side of a stereo
 * recording it plays, what of the recording it reproduces and how loud it plays next to the others.
 *
 * @param x        where it is
 * @param side     the side it plays
 * @param response what of a recording it plays
 * @param gain     how loud this place plays, from 0 to 1, on top of the recording's own volume: a player's balance
 *                 turning one side down, say
 */
public record MediaPlace(double x, double y, double z, StereoSide side, FrequencyResponse response, float gain) {

    public MediaPlace {
        side = side == null ? StereoSide.BOTH : side;
        response = response == null ? FrequencyResponse.FULL : response;
        gain = Math.clamp(gain, 0.0F, 1.0F);
    }

    /** A place playing at the recording's own volume. */
    public MediaPlace(final double x, final double y, final double z, final StereoSide side,
                      final FrequencyResponse response) {
        this(x, y, z, side, response, 1.0F);
    }

    /** How far this place is from a point, squared. */
    public double distanceSquared(final double px, final double py, final double pz) {
        final double dx = px - x;
        final double dy = py - y;
        final double dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The same place playing at that gain. */
    public MediaPlace withGain(final float value) {
        return new MediaPlace(x, y, z, side, response, value);
    }
}
