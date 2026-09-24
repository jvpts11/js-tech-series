/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * The shapes a synthesised tone can have. A PC speaker has only the square; a sound card of the eighties adds the
 * narrow pulses, the triangle and noise; later ones play anything, the sine and the sawtooth among them. Each has a
 * number of its own, which is how a tone travels to the clients that play it.
 */
public enum Waveform implements IStableId {

    SQUARE(0),

    PULSE_25(1),

    PULSE_12(2),

    TRIANGLE(3),

    SAWTOOTH(4),

    SINE(5),

    NOISE(6);

    private static final StableIds<Waveform> IDS = StableIds.of(Waveform.class);

    private final int id;

    Waveform(final int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    /** The value of this wave at {@code phase} (0 to 1 over one period), from -1 to 1; noise takes {@code random}. */
    public double at(final double phase, final double random) {
        return switch (this) {
            case SQUARE -> phase < 0.5 ? 1.0 : -1.0;
            case PULSE_25 -> phase < 0.25 ? 1.0 : -1.0;
            case PULSE_12 -> phase < 0.125 ? 1.0 : -1.0;
            case TRIANGLE -> phase < 0.5 ? 4.0 * phase - 1.0 : 3.0 - 4.0 * phase;
            case SAWTOOTH -> 2.0 * phase - 1.0;
            case SINE -> Math.sin(2.0 * Math.PI * phase);
            case NOISE -> random * 2.0 - 1.0;
        };
    }

    /** The shape with that number; a number no shape has reads as the square, which every speaker can play. */
    public static Waveform byId(final int id) {
        return IDS.byId(id, SQUARE);
    }
}
