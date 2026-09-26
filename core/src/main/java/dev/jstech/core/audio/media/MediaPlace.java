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
 * recording it plays and what of the recording it reproduces.
 *
 * @param x        where it is
 * @param side     the side it plays
 * @param response what of a recording it plays
 */
public record MediaPlace(double x, double y, double z, StereoSide side, FrequencyResponse response) {

    public MediaPlace {
        side = side == null ? StereoSide.BOTH : side;
        response = response == null ? FrequencyResponse.FULL : response;
    }

    /** How far this place is from a point, squared. */
    public double distanceSquared(final double px, final double py, final double pz) {
        final double dx = px - x;
        final double dy = py - y;
        final double dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
