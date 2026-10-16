/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import java.util.List;

/**
 * Values over a particle's life from the keys its file gives, spread evenly from its birth to its death and blended
 * between: one key holds all its life, two go from the first to the second. Pure.
 */
public final class EffectCurves {

    private static final int CHANNEL = 0xFF;
    private static final int CHANNELS = 4;
    private static final int BITS = 8;

    private EffectCurves() {
    }

    /** The value of {@code keys} at {@code share} of the way through a life, 0 its birth and 1 its death. */
    public static double at(final List<Double> keys, final double share) {
        if (keys.size() == 1) {
            return keys.getFirst();
        }
        final double along = Math.clamp(share, 0.0, 1.0) * (keys.size() - 1);
        final int low = Math.min((int) along, keys.size() - 2);
        final double t = along - low;
        return keys.get(low) + (keys.get(low + 1) - keys.get(low)) * t;
    }

    /** The colour of {@code keys} ({@code 0xAARRGGBB}) at {@code share} of the way through a life. */
    public static int colourAt(final List<Integer> keys, final double share) {
        if (keys.size() == 1) {
            return keys.getFirst();
        }
        final double along = Math.clamp(share, 0.0, 1.0) * (keys.size() - 1);
        final int low = Math.min((int) along, keys.size() - 2);
        return blend(keys.get(low), keys.get(low + 1), along - low);
    }

    /** {@code share} of the way from colour {@code a} to {@code b}, each channel, alpha with them. */
    public static int blend(final int a, final int b, final double share) {
        int out = 0;
        for (int channel = 0; channel < CHANNELS; channel++) {
            final int shift = channel * BITS;
            final int from = a >>> shift & CHANNEL;
            final int to = b >>> shift & CHANNEL;
            out |= (int) Math.round(from + (to - from) * share) << shift;
        }
        return out;
    }
}
