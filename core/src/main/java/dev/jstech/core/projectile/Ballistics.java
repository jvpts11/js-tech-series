/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

/**
 * The sums a projectile flies by: how dense the air is, how much of its speed the air or the water takes over a step,
 * and how much of it is left after getting through a block. Speeds in metres a second, masses in kilograms. Pure: no
 * part of the game.
 */
public final class Ballistics {

    /** The density of the air at sea level, in kilograms a cubic metre. */
    public static final double SEA_LEVEL_AIR = 1.225;
    /** The density of water, in kilograms a cubic metre. */
    public static final double WATER = 1000.0;
    private static final double SEA_LEVEL_PRESSURE = 101.325;
    private static final double SEA_LEVEL_KELVIN = 288.15;
    private static final double ZERO_CELSIUS = 273.15;
    /* Colder than this, in kelvin, the sums would divide by nearly nothing; no air is that cold. */
    private static final double COLDEST = 1.0;

    private Ballistics() {
    }

    /** How dense the air is at {@code kilopascals} and {@code celsius}, as an ideal gas: none in a vacuum. */
    public static double airDensity(final double kilopascals, final double celsius) {
        if (kilopascals <= 0.0) {
            return 0.0;
        }
        final double kelvin = Math.max(COLDEST, celsius + ZERO_CELSIUS);
        return SEA_LEVEL_AIR * (kilopascals / SEA_LEVEL_PRESSURE) * (SEA_LEVEL_KELVIN / kelvin);
    }

    /**
     * What share of a speed of {@code speed} through a fluid of {@code density} is left after {@code seconds}, for a
     * body of {@code mass} and {@code dragArea}. The drag grows with the speed squared, and the share is its exact
     * sum over the step, so no step is so long that the speed turns round.
     */
    public static double dragScale(final double speed, final double density, final double dragArea,
                                   final double mass, final double seconds) {
        if (mass <= 0.0 || speed <= 0.0 || density <= 0.0 || dragArea <= 0.0) {
            return 1.0;
        }
        final double k = 0.5 * density * dragArea / mass;
        return 1.0 / (1.0 + k * speed * seconds);
    }

    /**
     * What share of its speed a projectile keeps getting through a block, having had {@code before} of hardness to get
     * through and {@code after} left: its energy falls as it uses up what it gets through.
     */
    public static double throughScale(final double before, final double after) {
        if (before <= 0.0) {
            return 0.0;
        }
        return Math.sqrt(Math.clamp(after / before, 0.0, 1.0));
    }
}
