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

/**
 * What a dimension is like to stand in: how hard it pulls, whether its air can be breathed, how hot or cold it is,
 * what its weather does and how thick its atmosphere is. A dimension's rules are a datapack file named after it,
 * {@code data/<namespace>/dimension_rules/<path>.json} for the dimension {@code <namespace>:<path>}; a dimension
 * without one is like the overworld.
 *
 * <pre>{@code
 * {"gravity": 0.16, "breathable": false, "temperature": -120, "weather": "clear", "pressure": 0}
 * }</pre>
 *
 * @param gravity     how hard it pulls, as a share of the overworld's pull
 * @param breathable  whether its air can be breathed
 * @param temperature how hot it is, in degrees Celsius
 * @param weather     what its weather does
 * @param pressure    how thick its atmosphere is, in kilopascals: the overworld's is 101.3
 */
public record DimensionRules(double gravity, boolean breathable, double temperature, Weather weather,
                             double pressure) {

    /** Hotter than this, a player not made for it burns. */
    public static final double SCORCHING = 60.0;
    /** Colder than this, a player not made for it freezes. */
    public static final double FREEZING = -30.0;

    /** The overworld's rules, which every dimension without a file of its own keeps. */
    public static final DimensionRules OVERWORLD = new DimensionRules(1.0, true, 15.0, Weather.NATURAL, 101.3);

    public static final Codec<DimensionRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0, 10.0).optionalFieldOf("gravity", OVERWORLD.gravity())
                    .forGetter(DimensionRules::gravity),
            Codec.BOOL.optionalFieldOf("breathable", OVERWORLD.breathable()).forGetter(DimensionRules::breathable),
            Codec.doubleRange(-273.15, 10_000.0).optionalFieldOf("temperature", OVERWORLD.temperature())
                    .forGetter(DimensionRules::temperature),
            StableCodecs.byName(Weather.class).optionalFieldOf("weather", OVERWORLD.weather())
                    .forGetter(DimensionRules::weather),
            Codec.doubleRange(0.0, 100_000.0).optionalFieldOf("pressure", OVERWORLD.pressure())
                    .forGetter(DimensionRules::pressure)
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
