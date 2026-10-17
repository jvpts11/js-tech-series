/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

/**
 * What a blast goes out through, as far as its shock wave and its fragments go. In air the wave travels at the speed of
 * sound, which the air's temperature sets, and is as strong as Sachs's scaling says for the air's pressure: thin air
 * carries a weaker wave. Under water it travels four times as fast and carries three times as far. In a vacuum there
 * is no wave at all, only the fragments, the heat and what a mod gives off itself.
 *
 * @param speed    how fast the shock wave goes, in metres a second; 0 for none
 * @param strength the share of the overpressure of air at sea level the wave has at the same scaled distance
 * @param carry    how many times as far as in air at sea level the wave carries a pressure
 * @param density  how dense the medium is, in kilograms a cubic metre, for the drag on the fragments
 */
public record BlastMedium(double speed, double strength, double carry, double density) {

    /** Below this pressure the air carries no wave, in kilopascals. */
    public static final double THINNEST = 1;
    /** How many times as far water carries the wave as air at sea level does. */
    public static final double WATER_CARRY = 3;
    /** Water's density, in kilograms a cubic metre. */
    public static final double WATER_DENSITY = 1_000;
    /** A vacuum: no wave, nothing slowing the fragments. */
    public static final BlastMedium VACUUM = new BlastMedium(0, 0, 1, 0);
    /** Water, as under the sea or in a pool. */
    public static final BlastMedium WATER = new BlastMedium(BlastMath.SOUND_IN_WATER, 1, WATER_CARRY, WATER_DENSITY);
    /* The speed of sound in air at 0 degrees Celsius, in metres a second, and that temperature in kelvin. */
    private static final double SOUND_AT_FREEZING = 331.3;
    private static final double ZERO_CELSIUS = 273.15;
    /* The air's density at sea level and 15 degrees Celsius, in kilograms a cubic metre. */
    private static final double AIR_DENSITY = 1.225;
    private static final double STANDARD_KELVIN = 288.15;
    /* The densest air a wave is worked out for, as a share of the sea level's: a world of crushing air. */
    private static final double DENSEST = 100;
    /* The coldest the air is taken as, in kelvin, so the speed of sound never falls to nothing. */
    private static final double COLDEST_KELVIN = 3;

    public BlastMedium {
        if (speed < 0 || strength < 0 || carry <= 0 || density < 0) {
            throw new IllegalArgumentException("not a medium: " + speed + " m/s, " + strength + " strong, carries "
                    + carry + ", " + density + " kg/m3");
        }
    }

    /** Air at {@code kilopascals} and {@code celsius}; a vacuum when it is thinner than {@link #THINNEST}. */
    public static BlastMedium air(final double kilopascals, final double celsius) {
        if (kilopascals < THINNEST) {
            return VACUUM;
        }
        final double share = Math.min(DENSEST, kilopascals / BlastMath.SEA_LEVEL);
        final double kelvin = Math.max(COLDEST_KELVIN, celsius + ZERO_CELSIUS);
        // Sachs: the overpressure, as a share of the air's own, is the same at the distance scaled by its cube root.
        return new BlastMedium(SOUND_AT_FREEZING * Math.sqrt(kelvin / ZERO_CELSIUS), share, 1 / Math.cbrt(share),
                AIR_DENSITY * share * STANDARD_KELVIN / kelvin);
    }

    /** Whether it carries a shock wave at all. */
    public boolean carriesWave() {
        return this.speed > 0 && this.strength > 0;
    }

    /** The same medium, its wave going at {@code metresASecond} instead, as laws of a place that slow it may say. */
    public BlastMedium paced(final double metresASecond) {
        return carriesWave() ? new BlastMedium(metresASecond, this.strength, this.carry, this.density) : this;
    }

    /** The peak overpressure {@code metres} from {@code tnt} kilograms of TNT in it, in kilopascals. */
    public double overpressure(final double tnt, final double metres) {
        if (!carriesWave()) {
            return 0;
        }
        return this.strength * BlastMath.overpressure(tnt, metres / this.carry);
    }

    /** How far from {@code tnt} kilograms of TNT the overpressure in it falls to {@code kilopascals}, in metres. */
    public double reach(final double tnt, final double kilopascals) {
        if (!carriesWave()) {
            return 0;
        }
        return BlastMath.reach(tnt, kilopascals / this.strength) * this.carry;
    }

    /** How many ticks its wave takes to go {@code metres}; never, as the largest int, when it carries none. */
    public int arrival(final double metres) {
        return carriesWave() ? BlastMath.arrival(metres, this.speed) : Integer.MAX_VALUE;
    }
}
