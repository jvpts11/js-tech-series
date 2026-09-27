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
 *
 * <p>Two are the voices of whole kinds of sound card rather than plain shapes: {@link #FM}, the bright, metallic note
 * of frequency modulation, one sine bending the phase of another; and {@link #WAVETABLE}, the rounder note of the cards
 * that played from a table of recorded harmonics.
 */
public enum Waveform implements IStableId {

    SQUARE(0),

    PULSE_25(1),

    PULSE_12(2),

    TRIANGLE(3),

    SAWTOOTH(4),

    SINE(5),

    NOISE(6),

    FM(7),

    WAVETABLE(8);

    /* How far the modulator bends the carrier's phase, and how much faster it turns: the FM chips' bell-like voice. */
    private static final double FM_INDEX = 1.5;
    private static final double FM_RATIO = 2.0;
    /* The harmonics of the table, each after the one below it, and what they add up to at most. */
    private static final double[] HARMONICS = {1.0, 0.5, 0.3, 0.15, 0.08};
    private static final double HARMONICS_PEAK = 1.8;

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
            case FM -> Math.sin(2.0 * Math.PI * phase + FM_INDEX * Math.sin(2.0 * Math.PI * FM_RATIO * phase));
            case WAVETABLE -> table(phase);
        };
    }

    /* The table's harmonics at that point of the period, kept within -1 and 1. */
    private static double table(final double phase) {
        double sum = 0.0;
        for (int k = 0; k < HARMONICS.length; k++) {
            sum += HARMONICS[k] * Math.sin(2.0 * Math.PI * (k + 1) * phase);
        }
        return Math.max(-1.0, Math.min(1.0, sum / HARMONICS_PEAK));
    }

    /** The shape with that number; a number no shape has reads as the square, which every speaker can play. */
    public static Waveform byId(final int id) {
        return IDS.byId(id, SQUARE);
    }
}
