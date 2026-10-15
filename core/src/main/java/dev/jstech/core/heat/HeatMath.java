/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * The physics of heat between blocks, in real units: kelvin, joules, watts, seconds. A block is a cube of one metre;
 * two blocks face to face share a square metre and their centres are a metre apart. Pure, so its numbers are tested
 * without the game.
 *
 * <ul>
 *   <li>Conduction through a shared face: each half of each block resists by its half-metre over its conductivity, so
 *       the two together conduct {@code 2 k1 k2 / (k1 + k2)} watts a kelvin.</li>
 *   <li>Convection to the air: {@value #AIR_CONVECTION} watts a square metre a kelvin in the overworld's air, in
 *       proportion to how thick the air is, and none in a vacuum.</li>
 *   <li>Radiation: {@code e s A (T^4 - Te^4)} with Stefan and Boltzmann's constant, in air and in a vacuum alike, so
 *       in a vacuum it is the only way out.</li>
 * </ul>
 *
 * <p>A step never carries heat past the balance the two sides would reach, so a short step on a small body and a long
 * one on a large body are both stable.
 */
public final class HeatMath {

    /** Stefan and Boltzmann's constant, in watts a square metre a kelvin to the fourth. */
    public static final double STEFAN_BOLTZMANN = 5.670_374_419e-8;
    /** What still air at the overworld's pressure carries off a surface, in watts a square metre a kelvin. */
    public static final double AIR_CONVECTION = 10.0;
    /** The overworld's air pressure, in kilopascals. */
    public static final double SEA_LEVEL_KPA = 101.3;
    /** Zero degrees Celsius, in kelvin. */
    public static final double ZERO_CELSIUS = 273.15;
    /** The seconds in a tick. */
    public static final double TICK_SECONDS = 0.05;

    private HeatMath() {
    }

    /** Degrees Celsius in kelvin. */
    public static double kelvin(final double celsius) {
        return celsius + ZERO_CELSIUS;
    }

    /** Kelvin in degrees Celsius. */
    public static double celsius(final double kelvin) {
        return kelvin - ZERO_CELSIUS;
    }

    /** The heat capacity of {@code kilograms} of a material of {@code specificHeat} joules a kilogram a kelvin. */
    public static double capacity(final double kilograms, final double specificHeat) {
        return Math.max(0.0, kilograms) * Math.max(0.0, specificHeat);
    }

    /**
     * What two blocks face to face conduct, in watts a kelvin, from their materials' conductivities in watts a metre a
     * kelvin: half a metre of each, one after the other, through a square metre.
     */
    public static double conductance(final double conductivityA, final double conductivityB) {
        if (conductivityA <= 0.0 || conductivityB <= 0.0) {
            return 0.0;
        }
        return 2.0 * conductivityA * conductivityB / (conductivityA + conductivityB);
    }

    /**
     * What air carries off {@code area} square metres at {@code temperature} into air at {@code ambient}, in watts,
     * in proportion to how thick the air is: none in a vacuum.
     */
    public static double convection(final double temperature, final double ambient, final double pressureKpa,
                                    final double area) {
        final double thickness = Math.max(0.0, pressureKpa) / SEA_LEVEL_KPA;
        return AIR_CONVECTION * thickness * area * (temperature - ambient);
    }

    /**
     * What {@code area} square metres of {@code emissivity} at {@code temperature} radiate away to surroundings at
     * {@code surroundings}, in watts; negative when the surroundings are the hotter.
     */
    public static double radiation(final double temperature, final double surroundings, final double emissivity,
                                   final double area) {
        final double t2 = temperature * temperature;
        final double s2 = surroundings * surroundings;
        return STEFAN_BOLTZMANN * Math.max(0.0, Math.min(1.0, emissivity)) * area * (t2 * t2 - s2 * s2);
    }

    /**
     * The heat that flows from body A to body B in {@code seconds} through {@code conductance}, never more than takes
     * them to the temperature they would share: positive from A to B.
     */
    public static double exchange(final double temperatureA, final double capacityA, final double temperatureB,
                                  final double capacityB, final double conductance, final double seconds) {
        if (capacityA <= 0.0 || capacityB <= 0.0 || conductance <= 0.0 || seconds <= 0.0) {
            return 0.0;
        }
        final double difference = temperatureA - temperatureB;
        final double flow = conductance * difference * seconds;
        final double balance = difference * capacityA * capacityB / (capacityA + capacityB);
        return difference >= 0.0 ? Math.min(flow, balance) : Math.max(flow, balance);
    }

    /**
     * The heat a body of {@code capacity} at {@code temperature} loses in {@code seconds} at {@code watts}, never more
     * than takes it to {@code ambient}: positive when it cools.
     */
    public static double loss(final double temperature, final double capacity, final double ambient,
                              final double watts, final double seconds) {
        if (capacity <= 0.0 || seconds <= 0.0) {
            return 0.0;
        }
        final double lost = watts * seconds;
        final double toAmbient = (temperature - ambient) * capacity;
        return toAmbient >= 0.0 ? Math.max(0.0, Math.min(lost, toAmbient)) : Math.min(0.0, Math.max(lost, toAmbient));
    }
}
