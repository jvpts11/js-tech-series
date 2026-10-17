/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.Optional;

/**
 * What a dimension is like to stand in: how hard it pulls, whether its air can be breathed, how hot or cold it is,
 * what its weather does, how thick its atmosphere is, its own clock and how hard its wind blows. A dimension's rules
 * are a datapack file named after it, {@code data/<namespace>/dimension_rules/<path>.json} for the dimension
 * {@code <namespace>:<path>}; a dimension without one is like the overworld.
 *
 * <pre>{@code
 * {"gravity": 0.38, "breathable": false, "temperature": -60, "weather": "clear", "pressure": 0.6,
 *  "clock": {"day": 24660, "year": 669}, "wind": 2.5}
 * }</pre>
 *
 * @param gravity     how hard it pulls, as a share of the overworld's pull
 * @param breathable  whether its air can be breathed
 * @param temperature how hot it is, in degrees Celsius
 * @param weather     what its weather does
 * @param pressure    how thick its atmosphere is, in kilopascals: the overworld's is 101.3
 * @param clock       its own clock, if it keeps one; never the overworld's, whose day no rule touches
 * @param wind        how hard its wind blows, as a share of the overworld's
 */
public record DimensionRules(double gravity, boolean breathable, double temperature, Weather weather,
                             double pressure, Optional<DimensionClock> clock, double wind) {

    /** Hotter than this, a player not made for it burns. */
    public static final double SCORCHING = 60.0;
    /** Colder than this, a player not made for it freezes. */
    public static final double FREEZING = -30.0;

    /** The overworld's rules, which every dimension without a file of its own keeps. */
    public static final DimensionRules OVERWORLD = new DimensionRules(1.0, true, 15.0, Weather.NATURAL, 101.3,
            Optional.empty(), 1.0);
    /** The hardest a dimension's wind may blow, as a share of the overworld's. */
    public static final double MOST_WIND = 20.0;

    public static final Codec<DimensionRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0, 10.0).optionalFieldOf("gravity", OVERWORLD.gravity())
                    .forGetter(DimensionRules::gravity),
            Codec.BOOL.optionalFieldOf("breathable", OVERWORLD.breathable()).forGetter(DimensionRules::breathable),
            Codec.doubleRange(-273.15, 10_000.0).optionalFieldOf("temperature", OVERWORLD.temperature())
                    .forGetter(DimensionRules::temperature),
            StableCodecs.byName(Weather.class).optionalFieldOf("weather", OVERWORLD.weather())
                    .forGetter(DimensionRules::weather),
            Codec.doubleRange(0.0, 100_000.0).optionalFieldOf("pressure", OVERWORLD.pressure())
                    .forGetter(DimensionRules::pressure),
            DimensionClock.CODEC.optionalFieldOf("clock").forGetter(DimensionRules::clock),
            Codec.doubleRange(0.0, MOST_WIND).optionalFieldOf("wind", OVERWORLD.wind())
                    .forGetter(DimensionRules::wind)
    ).apply(instance, DimensionRules::new));

    /** Whether a player not made for it burns here. */
    public boolean scorching() {
        return temperature > SCORCHING;
    }

    /** Whether a player not made for it freezes here. */
    public boolean freezing() {
        return temperature < FREEZING;
    }

    /** What a dimension's weather does. */
    public enum Weather implements IStableName {
        /** Rain and thunder come and go as the overworld's do. */
        NATURAL("natural"),
        /** It never rains. */
        CLEAR("clear"),
        /** It always rains. */
        RAIN("rain"),
        /** It always storms. */
        THUNDER("thunder");

        private final String serializedName;

        Weather(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
