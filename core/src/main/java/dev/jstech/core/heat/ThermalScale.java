/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * How warm a thing looks through a thermal view, against the air around it: 0 at the air's temperature, rising to 1
 * at {@value #HOTTEST} K and falling to -1 at {@value #COLDEST} K, by the ratio of the temperatures and steepest near
 * the air's, so a body a few tens of kelvin warmer than the air already stands out, as a thermal camera shows a
 * person against a room. Pure.
 */
public final class ThermalScale {

    /** The hottest a thermal view tells apart, in kelvin; anything hotter looks as hot. */
    public static final double HOTTEST = 1_500.0;
    /** The coldest it tells apart, in kelvin: liquid nitrogen. */
    public static final double COLDEST = 77.0;
    /** How far from the air's temperature, in kelvin, a thing must be to be shown apart from it. */
    public static final double APART = 3.0;

    private ThermalScale() {
    }

    /** How warm a thing at {@code kelvin} looks where the air is at {@code air}, from -1 to 1. */
    public static double warmth(final double kelvin, final double air) {
        if (!(kelvin > 0.0) || !(air > 0.0)) {
            return 0.0;
        }
        final double hot = Math.max(HOTTEST, air * 1.01);
        final double cold = Math.min(COLDEST, air * 0.99);
        final double share = kelvin >= air ? Math.log(kelvin / air) / Math.log(hot / air)
                : -Math.log(air / kelvin) / Math.log(air / cold);
        // Steepest near the air's temperature: what is only a little warmer still shows.
        return Math.signum(share) * Math.sqrt(Math.min(1.0, Math.abs(share)));
    }

    /** Whether a thing at {@code kelvin} is shown apart from the air at {@code air}. */
    public static boolean apart(final double kelvin, final double air) {
        return Math.abs(kelvin - air) >= APART;
    }
}
