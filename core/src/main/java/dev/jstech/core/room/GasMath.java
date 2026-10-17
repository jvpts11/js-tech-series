/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

/**
 * The arithmetic of the gas in rooms, without the game, as real gases behave: the pressure of an amount of gas in a
 * volume, as an ideal gas's; gas flowing through an opening in proportion to the difference in pressure and the size of
 * the opening, so a hole of a block empties a small room in seconds and a crack in minutes; water pushing in through a
 * hole as fast as its depth pushes it; and what an air does to whoever breathes it, and to fire.
 */
public final class GasMath {

    /** The gas constant, in joules a mole a kelvin. */
    public static final double GAS_CONSTANT = 8.314_462;
    /** The warmth a sealed room is kept at, in kelvin: 20 °C. */
    public static final double TEMPERED = 293.15;
    /** A block's volume, in cubic metres. */
    public static final double BLOCK = 1.0;
    /** One atmosphere, in kilopascals. */
    public static final double ATMOSPHERE = 101.325;
    /**
     * How fast gas crosses an opening, in cubic metres a second for each square metre of it: a room of eight blocks
     * with a hole of one block in its wall loses most of its air in about five seconds.
     */
    public static final double FLOW = 4.0;
    /**
     * How fast two airs joined by an open door mix, in the same measure: their pressures even out at once, but their
     * gases mix over time, two small rooms through a door in about a minute.
     */
    public static final double MIX = 0.05;
    /** The pull of the overworld, in metres a second a second, and the weight of a metre of water, in kilopascals. */
    public static final double GRAVITY = 9.806_65;
    public static final double WATER_PER_METRE = 9.806_65;
    /* How much of the ideal rush of water a real hole lets through. */
    private static final double DISCHARGE = 0.6;
    /** Carbon dioxide gives a headache and nausea above this share of the air, and is dangerous above the next. */
    public static final double CARBON_DIOXIDE_HEADACHE = 0.01;
    public static final double CARBON_DIOXIDE_DANGER = 0.05;
    /** Oxygen poisons, slowly, above this partial pressure, in kilopascals: pure oxygen at full pressure does. */
    public static final double OXYGEN_POISONS = 50.0;
    /** A fire burns on at least this partial pressure of oxygen, in kilopascals, and nothing burns below it. */
    public static final double FIRE_NEEDS = 12.0;
    /** Air mostly of oxygen, this share or more with fire's oxygen too, turns any flame into an inferno. */
    public static final double INFERNO_SHARE = 0.5;
    /** The least pressure a plant grows in, in kilopascals: in a vacuum nothing grows. */
    public static final double PLANTS_NEED = 5.0;
    /**
     * The least share of a face a leak lets through to be shown as one: glass and a shut door leak less, and are not
     * a leak anyone hunts for.
     */
    public static final double NOTICEABLE = 0.01;

    private GasMath() {
    }

    /** The pressure, in kilopascals, of {@code moles} of gas in {@code volume} cubic metres at {@code kelvin}. */
    public static double pressure(final double moles, final double volume, final double kelvin) {
        if (volume <= 0) {
            return 0;
        }
        return Math.max(0, moles) * GAS_CONSTANT * kelvin / volume / 1_000.0;
    }

    /** How many moles of gas fill {@code volume} cubic metres at {@code kilopascals} and {@code kelvin}. */
    public static double moles(final double kilopascals, final double volume, final double kelvin) {
        if (kelvin <= 0) {
            return 0;
        }
        return Math.max(0, kilopascals) * 1_000.0 * Math.max(0, volume) / (GAS_CONSTANT * kelvin);
    }

    /**
     * What is left of a difference after {@code seconds} of a flow of {@code rate} a second: it falls away as a room's
     * pressure falls towards the pressure beyond its leaks, quickly at first and slower as it nears it.
     */
    public static double kept(final double rate, final double seconds) {
        return Math.exp(-Math.max(0, rate) * Math.max(0, seconds));
    }

    /**
     * The rate a room of {@code volume} cubic metres loses its difference through openings of {@code area} square
     * metres, each square metre passing {@code flow} cubic metres a second.
     */
    public static double rate(final double area, final double flow, final double volume) {
        if (volume <= 0) {
            return 0;
        }
        return Math.max(0, area) * flow / volume;
    }

    /**
     * How many moles of a gas pass between two rooms joined by openings of {@code area}, in {@code seconds}, from the
     * first to the second (a negative amount the other way): the gas's amounts in each, {@code first} in {@code
     * firstVolume} and {@code second} in {@code secondVolume}, move towards the same amount in each cubic metre.
     */
    public static double between(final double first, final double firstVolume, final double second,
                                 final double secondVolume, final double area, final double flow,
                                 final double seconds) {
        if (firstVolume <= 0 || secondVolume <= 0) {
            return 0;
        }
        final double even = (first + second) / (firstVolume + secondVolume);
        final double rate = Math.max(0, area) * flow * (1.0 / firstVolume + 1.0 / secondVolume);
        final double firstAfter = even * firstVolume + (first - even * firstVolume) * kept(rate, seconds);
        return first - firstAfter;
    }

    /** The pressure of water {@code depth} metres down, under {@code surface} kilopascals at its top. */
    public static double underWater(final double surface, final double depth) {
        return surface + WATER_PER_METRE * Math.max(0, depth);
    }

    /**
     * How many cubic metres of water a second rush in through {@code area} square metres of holes, pushed by {@code
     * difference} kilopascals: as fast as a fall from the height that difference is worth.
     */
    public static double flooding(final double area, final double difference) {
        if (difference <= 0 || area <= 0) {
            return 0;
        }
        final double height = difference / WATER_PER_METRE;
        return area * DISCHARGE * Math.sqrt(2 * GRAVITY * height);
    }

    /** Whether a block rated to {@code rating} kilopascals bursts, {@code inside} on one side and the rest beyond. */
    public static boolean bursts(final double rating, final double inside, final double outside) {
        return Math.abs(inside - outside) > rating;
    }

    /** Whether air with {@code share} of carbon dioxide gives a headache. */
    public static boolean headache(final double share) {
        return share > CARBON_DIOXIDE_HEADACHE;
    }

    /** Whether air with {@code share} of carbon dioxide is dangerous to breathe. */
    public static boolean stifling(final double share) {
        return share > CARBON_DIOXIDE_DANGER;
    }

    /** Whether oxygen at {@code partial} kilopascals poisons, slowly. */
    public static boolean poisons(final double partial) {
        return partial > OXYGEN_POISONS;
    }

    /** Whether a fire burns in oxygen at {@code partial} kilopascals. */
    public static boolean burns(final double partial) {
        return partial >= FIRE_NEEDS;
    }

    /** Whether a fire in air with {@code share} of oxygen, {@code partial} kilopascals of it, turns into an inferno. */
    public static boolean inferno(final double share, final double partial) {
        return share >= INFERNO_SHARE && burns(partial);
    }
}
