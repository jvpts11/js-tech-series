/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

/**
 * How loud a stretch of sound is in each of a few bands from the bass to the treble, the way a player's analyser
 * shows it: bands spaced by octaves rather than evenly, since that is how an ear hears them, each from 0 for silence
 * to 1 for as loud as a band gets.
 *
 * <p>Each band is measured at its middle with the Goertzel filter, which answers for one frequency at a cost that
 * grows only with the length of the stretch, so a handful of bars costs far less than a full transform.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class Spectrum {

    /** The lowest band's middle, in hertz. */
    public static final double LOWEST = 60.0;
    /** The highest band's middle, in hertz. */
    public static final double HIGHEST = 14_000.0;
    /* How far below full scale reads as nothing, in decibels. */
    private static final double FLOOR_DB = 60.0;

    private Spectrum() {
    }

    /**
     * Measures {@code samples}, from -1 to 1 and taken at {@code rate} a second, into {@code out.length} bands.
     * A band whose middle lies above half the rate hears nothing, since the samples cannot hold it.
     */
    public static void bands(final float[] samples, final int rate, final float[] out) {
        final int n = samples.length;
        // A window that tapers the ends, so the edges of the stretch do not ring in every band.
        final double[] tapered = new double[n];
        for (int i = 0; i < n; i++) {
            tapered[i] = samples[i] * (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / Math.max(1, n - 1)));
        }
        for (int b = 0; b < out.length; b++) {
            final double frequency = middleOf(b, out.length);
            if (n == 0 || frequency >= rate / 2.0) {
                out[b] = 0.0F;
                continue;
            }
            final double coefficient = 2.0 * Math.cos(2.0 * Math.PI * frequency / rate);
            double before = 0.0;
            double last = 0.0;
            for (int i = 0; i < n; i++) {
                final double now = tapered[i] + coefficient * last - before;
                before = last;
                last = now;
            }
            final double power = before * before + last * last - coefficient * before * last;
            // A full-scale sine through the window above reaches an amplitude of about a quarter of the length.
            final double amplitude = Math.sqrt(Math.max(0.0, power)) / (n / 4.0);
            final double db = 20.0 * Math.log10(Math.max(amplitude, 1e-9));
            out[b] = (float) Math.clamp((db + FLOOR_DB) / FLOOR_DB, 0.0, 1.0);
        }
    }

    /** The middle of band {@code band} of {@code count}, spaced evenly in octaves from the lowest to the highest. */
    public static double middleOf(final int band, final int count) {
        if (count <= 1) {
            return LOWEST;
        }
        return LOWEST * Math.pow(HIGHEST / LOWEST, band / (double) (count - 1));
    }
}
