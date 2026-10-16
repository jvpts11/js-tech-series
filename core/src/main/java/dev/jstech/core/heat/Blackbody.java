/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * The light a hot body gives off, as a black body does: its colour at a temperature, from Planck's law weighed by the
 * eye's colour matching functions and turned into the screen's colours, and how strongly it glows, nothing below the
 * Draper point, where anything starts to glow a dull red, up to the whole of its light once it is white hot. Pure.
 */
public final class Blackbody {

    /** Where anything starts to glow, in kelvin: a dull red, seen in the dark. */
    public static final double DRAPER_POINT = 798.0;
    /** Where a body glows with the whole of its light, in kelvin: yellow going white. */
    public static final double FULL_GLOW = 1_500.0;

    /* The temperatures the colours are worked out at, in kelvin, and how far apart; between them they are blended. */
    private static final double COOLEST = 500.0;
    private static final double HOTTEST = 20_000.0;
    private static final double STEP = 25.0;
    private static final int[] TABLE = table();
    /* The second radiation constant, h c / k, in metre kelvins; and the visible wavelengths, in nanometres. */
    private static final double SECOND_RADIATION = 1.438_776_877e-2;
    private static final double SHORTEST = 380.0;
    private static final double LONGEST = 780.0;
    private static final double WAVELENGTH_STEP = 5.0;
    private static final double NANOMETRE = 1.0e-9;
    private static final int OPAQUE = 0xFF << 24;
    private static final int CHANNEL = 255;

    private Blackbody() {
    }

    /** The colour a black body at {@code kelvin} glows, opaque, as bright as its brightest channel allows. */
    public static int colour(final double kelvin) {
        final double at = (Math.clamp(kelvin, COOLEST, HOTTEST) - COOLEST) / STEP;
        final int below = (int) Math.floor(at);
        final int above = Math.min(TABLE.length - 1, below + 1);
        return blend(TABLE[below], TABLE[above], at - below);
    }

    /** How strongly a body at {@code kelvin} glows, from 0 below the Draper point to 1 once it glows wholly. */
    public static double glow(final double kelvin) {
        return Math.clamp((kelvin - DRAPER_POINT) / (FULL_GLOW - DRAPER_POINT), 0.0, 1.0);
    }

    /** Whether a body at {@code kelvin} glows at all. */
    public static boolean glows(final double kelvin) {
        return kelvin > DRAPER_POINT;
    }

    /* Every colour of the table, worked out once. */
    private static int[] table() {
        final int count = (int) ((HOTTEST - COOLEST) / STEP) + 1;
        final int[] colours = new int[count];
        for (int i = 0; i < count; i++) {
            colours[i] = worked(COOLEST + i * STEP);
        }
        return colours;
    }

    /* The colour at {@code kelvin}: its spectrum to the eye's X, Y and Z, those to the screen's red, green and blue. */
    private static int worked(final double kelvin) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (double nm = SHORTEST; nm <= LONGEST; nm += WAVELENGTH_STEP) {
            final double radiance = planck(nm * NANOMETRE, kelvin);
            x += radiance * matchX(nm);
            y += radiance * matchY(nm);
            z += radiance * matchZ(nm);
        }
        // From the eye's colours to the screen's linear red, green and blue, white as daylight is.
        final double r = Math.max(0.0, 3.2406 * x - 1.5372 * y - 0.4986 * z);
        final double g = Math.max(0.0, -0.9689 * x + 1.8758 * y + 0.0415 * z);
        final double b = Math.max(0.0, 0.0557 * x - 0.2040 * y + 1.0570 * z);
        final double brightest = Math.max(r, Math.max(g, b));
        if (!(brightest > 0.0)) {
            return OPAQUE;
        }
        return OPAQUE | encoded(r / brightest) << 16 | encoded(g / brightest) << 8 | encoded(b / brightest);
    }

    /* Planck's law at a wavelength of {@code metres}, leaving out the constants that only scale it. */
    private static double planck(final double metres, final double kelvin) {
        return 1.0 / (Math.pow(metres, 5) * Math.expm1(SECOND_RADIATION / (metres * kelvin)));
    }

    /*
     * The eye's colour matching functions, as sums of lobes either side of a peak (Wyman, Sloan and Shirley's fit of
     * the 1931 observer); good to a few hundredths across the visible light.
     */
    private static double matchX(final double nm) {
        return 1.056 * lobe(nm, 599.8, 37.9, 31.0) + 0.362 * lobe(nm, 442.0, 16.0, 26.7)
                - 0.065 * lobe(nm, 501.1, 20.4, 26.2);
    }

    private static double matchY(final double nm) {
        return 0.821 * lobe(nm, 568.8, 46.9, 40.5) + 0.286 * lobe(nm, 530.9, 16.3, 31.1);
    }

    private static double matchZ(final double nm) {
        return 1.217 * lobe(nm, 437.0, 11.8, 36.0) + 0.681 * lobe(nm, 459.0, 26.0, 13.8);
    }

    /* A bell about {@code peak}, as wide as {@code below} short of it and {@code above} past it. */
    private static double lobe(final double nm, final double peak, final double below, final double above) {
        final double t = (nm - peak) / (nm < peak ? below : above);
        return Math.exp(-0.5 * t * t);
    }

    /* A linear share of light as the screen shows it, from 0 to 255. */
    private static int encoded(final double linear) {
        final double shown = linear <= 0.003_130_8 ? 12.92 * linear : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
        return (int) Math.round(Math.clamp(shown, 0.0, 1.0) * CHANNEL);
    }

    /* Between two colours, {@code share} of the way from the first. */
    private static int blend(final int from, final int to, final double share) {
        return OPAQUE | channel(from, to, 16, share) << 16 | channel(from, to, 8, share) << 8
                | channel(from, to, 0, share);
    }

    /* One channel, {@code shift} bits up, between two colours. */
    private static int channel(final int from, final int to, final int shift, final double share) {
        final int a = from >> shift & CHANNEL;
        final int b = to >> shift & CHANNEL;
        return (int) Math.round(a + (b - a) * share);
    }
}
