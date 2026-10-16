/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

/**
 * The climate of a biome in real values from the game's own: its mean temperature in degrees Celsius from the game's
 * temperature (plains 0.8 is a mild 14 °C, a snowy plain a frozen -10 °C, a desert 26 °C), its rain in millimetres a
 * year from its downfall, how far the seasons swing it, the season's swing at a day of the year, and the air cooling
 * with height. Pure.
 */
public final class ClimateMath {

    /** How much cooler the air is each block above the sea, as it is about 6.5 °C a kilometre. */
    public static final double LAPSE = 0.0065;
    /** The sea's level the game keeps, and the most rain a biome's downfall is: a rainforest's 2000 mm. */
    public static final int SEA_LEVEL = 63;
    public static final double MOST_RAIN = 2000.0;

    private static final double MILD = 0.8;
    private static final double MILD_CELSIUS = 14.0;
    private static final double COLDEST_CELSIUS = -10.0;
    private static final double WARM_STEP = 10.0;
    /* How far the seasons swing a place: little in the wet tropics, much where it is cold. */
    private static final double TROPICAL_SWING = 2.0;
    private static final double TEMPERATE_SWING = 10.0;
    private static final double COLD_SWING = 14.0;
    private static final double TROPICAL = 0.95;
    private static final double WET = 0.8;
    private static final double COLD = 0.15;
    /* How far into the year the seasons' swing starts rising: the warmest day falls three eighths of the way in. */
    private static final double SEASON_LAG = 0.125;

    private ClimateMath() {
    }

    /** A biome's mean temperature in degrees Celsius from the game's {@code temperature}. */
    public static double celsius(final double temperature) {
        if (temperature <= MILD) {
            return COLDEST_CELSIUS + (MILD_CELSIUS - COLDEST_CELSIUS) * temperature / MILD;
        }
        return MILD_CELSIUS + WARM_STEP * (temperature - MILD);
    }

    /** The game's temperature for a biome of mean {@code celsius}, as {@link #celsius} reads it back. */
    public static double gameTemperature(final double celsius) {
        if (celsius <= MILD_CELSIUS) {
            return (celsius - COLDEST_CELSIUS) / (MILD_CELSIUS - COLDEST_CELSIUS) * MILD;
        }
        return MILD + (celsius - MILD_CELSIUS) / WARM_STEP;
    }

    /** A biome's rain in millimetres a year from the game's {@code downfall}, 0 to 1. */
    public static double rain(final double downfall) {
        return Math.clamp(downfall, 0.0, 1.0) * MOST_RAIN;
    }

    /** The game's downfall for a biome of {@code millimetres} of rain a year. */
    public static double downfall(final double millimetres) {
        return Math.clamp(millimetres / MOST_RAIN, 0.0, 1.0);
    }

    /** How far the seasons swing a biome of the game's {@code temperature} and {@code downfall}, in °C either way. */
    public static double seasons(final double temperature, final double downfall) {
        if (temperature >= TROPICAL && downfall >= WET) {
            return TROPICAL_SWING;
        }
        return temperature <= COLD ? COLD_SWING : TEMPERATE_SWING;
    }

    /**
     * The temperature at {@code y} on {@code day} of a year {@code daysAYear} long, of a place of {@code mean} °C
     * swung {@code swing} °C by the seasons: the year opening with spring, warmest in the middle of summer and coolest
     * in the middle of winter, as the warmth comes weeks after the longest day, and cooler the higher above the sea.
     */
    public static double temperature(final double mean, final double swing, final int y, final double day,
                                     final double daysAYear) {
        final double season = swing * Math.sin(2 * Math.PI * (day / daysAYear - SEASON_LAG));
        return mean + season - Math.max(0, y - SEA_LEVEL) * LAPSE;
    }

    /** The temperature at {@code y} of a place of {@code mean} °C over the whole year, as the world is made. */
    public static double temperature(final double mean, final int y) {
        return mean - Math.max(0, y - SEA_LEVEL) * LAPSE;
    }
}
