/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A biome's climate in real values: how warm it is over the year, how far the seasons swing it, and how much rain
 * falls on it, as a biome's entry of the data map {@code data/jscore/data_maps/worldgen/biome/climate.json} says, in
 * any pack, or as the game's own temperature and downfall for it give it.
 *
 * <pre>{@code
 * { "values": { "mymod:salt_steppe": { "temperature": 9.5, "seasons": 16, "rain": 240 } } }
 * }</pre>
 *
 * @param temperature its mean temperature over the year, in degrees Celsius
 * @param seasons     how far the seasons swing it either way, in degrees Celsius
 * @param rain        how much rain, or snow, falls on it a year, in millimetres
 */
public record BiomeClimate(double temperature, double seasons, double rain) {

    private static final double ABSOLUTE_ZERO = -273.15;
    private static final double HOTTEST = 10_000.0;
    private static final double WIDEST_SWING = 200.0;
    private static final double MOST_RAIN = 20_000.0;
    private static final double TEMPERATE_SWING = 10.0;
    private static final double TEMPERATE_RAIN = 800.0;
    public static final Codec<BiomeClimate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(ABSOLUTE_ZERO, HOTTEST).fieldOf("temperature").forGetter(BiomeClimate::temperature),
            Codec.doubleRange(0.0, WIDEST_SWING).optionalFieldOf("seasons", TEMPERATE_SWING)
                    .forGetter(BiomeClimate::seasons),
            Codec.doubleRange(0.0, MOST_RAIN).optionalFieldOf("rain", TEMPERATE_RAIN).forGetter(BiomeClimate::rain))
            .apply(instance, BiomeClimate::new));

    /** The climate the game's own {@code temperature} and {@code downfall} for a biome give it. */
    public static BiomeClimate fromGame(final double temperature, final double downfall) {
        return new BiomeClimate(ClimateMath.celsius(temperature), ClimateMath.seasons(temperature, downfall),
                ClimateMath.rain(downfall));
    }

    /** How warm it is at {@code y} on {@code day} of a year {@code daysAYear} long, in degrees Celsius. */
    public double celsius(final int y, final double day, final double daysAYear) {
        return ClimateMath.temperature(this.temperature, this.seasons, y, day, daysAYear);
    }

    /** How warm it is at {@code y} over the whole year, as the world is made, in degrees Celsius. */
    public double celsius(final int y) {
        return ClimateMath.temperature(this.temperature, y);
    }

    /** The game's temperature for the biome, which says where it snows and how its grass is coloured. */
    public float gameTemperature() {
        return (float) ClimateMath.gameTemperature(this.temperature);
    }

    /** The game's downfall for the biome, which says how its grass and leaves are coloured. */
    public float gameDownfall() {
        return (float) ClimateMath.downfall(this.rain);
    }
}
