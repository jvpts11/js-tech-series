/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

/**
 * How a particle moves, from the place it is in rather than from curves drawn by hand: the dimension pulls it down;
 * the air it moves through drags it along with the wind, the harder the denser the air; what is lighter than the air
 * (steam, smoke) rises by as much as the air is dense, so nothing rises in a vacuum; and a plume of gas widens as the
 * air thins, huge where there is none. A block is a metre, a second twenty ticks. Pure.
 */
public final class EffectPhysics {

    /** The overworld's pull, in metres a second a second. */
    public static final double EARTH_GRAVITY = 9.81;
    /** The air's density at sea level, at 15 °C, in kilograms a cubic metre. */
    public static final double SEA_AIR = 1.225;
    /** Water's density, in kilograms a cubic metre. */
    public static final double WATER = 1000.0;
    /** How much more a plume widens at most, where there is no air at all. */
    public static final double MOST_WIDENING = 6.0;

    private static final double AIR_MOLAR_MASS = 0.02896;
    private static final double GAS_CONSTANT = 8.314;
    private static final double PASCALS_A_KILOPASCAL = 1000.0;
    private static final double LEAST_AIR = 0.001;

    private EffectPhysics() {
    }

    /** The density of air at {@code kiloPascals} and {@code kelvin}, in kilograms a cubic metre; none in a vacuum. */
    public static double airDensity(final double kiloPascals, final double kelvin) {
        if (kiloPascals <= 0 || kelvin <= 0) {
            return 0;
        }
        return kiloPascals * PASCALS_A_KILOPASCAL * AIR_MOLAR_MASS / (GAS_CONSTANT * kelvin);
    }

    /**
     * Moves {@code velocity} (metres a second, x y z) on by {@code seconds} in {@code place}, as {@code moves} says
     * acts on it.
     *
     * @param gravity  how much of the place's pull acts on it: 1 falls as a stone does, 0 floats
     * @param drag     how quickly the medium brings it to the medium's own speed in air at sea level, a second
     * @param buoyancy how hard it is lifted in air at sea level, in metres a second a second, for what is lighter;
     *                 against the pull and the drag it settles rising at {@code (buoyancy - pull) / drag}
     */
    public static void step(final double[] velocity, final Place place, final Moves moves, final double gravity,
                            final double drag, final double buoyancy, final double seconds) {
        final double share = place.density() / SEA_AIR;
        double up = moves.gravity() ? -place.gravity() * gravity : 0;
        double pull = 0;
        if (moves.air() && share > 0) {
            // Lift and drag both grow with the medium's density, so what rises settles at one speed in air and water.
            up += buoyancy * share;
            pull = drag * share * seconds;
        }
        final double windX = moves.air() && moves.wind() ? place.windX() : 0;
        final double windZ = moves.air() && moves.wind() ? place.windZ() : 0;
        // Worked out at the step's end, so a dense medium brings it to the medium's speed and never past it.
        velocity[0] = (velocity[0] + pull * windX) / (1 + pull);
        velocity[1] = (velocity[1] + up * seconds) / (1 + pull);
        velocity[2] = (velocity[2] + pull * windZ) / (1 + pull);
    }

    /**
     * The speed something falling in {@code place} settles at, metres a second, for its {@code gravity} and
     * {@code drag}; without air it never settles.
     */
    public static double terminalSpeed(final Place place, final double gravity, final double drag) {
        final double share = place.density() / SEA_AIR;
        if (share <= 0 || drag <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return place.gravity() * gravity / (drag * share);
    }

    /** How much wider a plume of gas grows in {@code place} than at sea level: as the air thins, up to six times. */
    public static double widening(final Place place) {
        final double density = Math.max(place.density(), LEAST_AIR);
        return Math.clamp(Math.sqrt(SEA_AIR / density), 1.0, MOST_WIDENING);
    }

    /**
     * Where a particle is: what pulls it, how dense what it moves through is, and the wind there.
     *
     * @param gravity the pull, in metres a second a second
     * @param density what it moves through, kilograms a cubic metre: air, water, or none
     * @param windX   the wind towards east, metres a second
     * @param windZ   the wind towards south, metres a second
     */
    public record Place(double gravity, double density, double windX, double windZ) {

        /** Still air at sea level in the overworld. */
        public static final Place SEA_LEVEL = new Place(EARTH_GRAVITY, SEA_AIR, 0, 0);
    }

    /**
     * What acts on a particle.
     *
     * @param gravity whether the pull does
     * @param air     whether what it moves through drags and lifts it
     * @param wind    whether the wind carries it
     */
    public record Moves(boolean gravity, boolean air, boolean wind) {

        /** Everything acts on it. */
        public static final Moves ALL = new Moves(true, true, true);
    }
}
