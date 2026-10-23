/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

/**
 * The formulas of transcendence: the energy of matter meeting antimatter, E = mc², what an anti-element gives by its
 * mass, and the share of what it is fed a Singularity Reactor turns into energy (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class TranscendenceFormulas {

    /** The speed of light, in metres a second. */
    public static final double LIGHT = 299_792_458.0;
    /** The most of the mass it is fed a Singularity Reactor turns into energy, as a spinning black hole does. */
    public static final double SINGULARITY_MOST = 0.4;

    private TranscendenceFormulas() {
    }

    /**
     * The energy, in joules, {@code kilograms} of antimatter give meeting as much matter, the two gone into energy
     * together: a milligram of each, 180 GJ.
     */
    public static double annihilation(final double kilograms) {
        return 2.0 * Math.max(0.0, kilograms) * LIGHT * LIGHT;
    }

    /**
     * How many times antihydrogen's energy a unit of the anti-element of mass number {@code massNumber} gives: its
     * mass's, so antihelium four times, anti-iron fifty-six.
     */
    public static double antiElementFactor(final int massNumber) {
        return Math.max(0, massNumber);
    }

    /** The most energy, in joules, a Singularity Reactor fed {@code kilograms} gives: 40% of their mass's. */
    public static double singularity(final double kilograms) {
        return SINGULARITY_MOST * Math.max(0.0, kilograms) * LIGHT * LIGHT;
    }
}
