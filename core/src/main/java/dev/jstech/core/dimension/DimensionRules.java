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
import dev.jstech.core.air.AirMath;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.physics.Laws;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * What a dimension is like to stand in: how hard it pulls, whether its air can be breathed, how hot or cold it is,
 * what its weather does, how thick its atmosphere is, its own clock and how hard its wind blows, what its air is made
 * of, how all that changes with height in bands, and laws of its own. A dimension's rules are a datapack file named
 * after it, {@code data/<namespace>/dimension_rules/<path>.json} for the dimension {@code <namespace>:<path>}; a
 * dimension without one is like the overworld. What it is like at a height is {@link #at}.
 *
 * <pre>{@code
 * {"gravity": 0.38, "temperature": -60, "weather": "clear", "pressure": 0.6, "clock": {"day": 24660, "year": 669},
 *  "wind": 2.5, "air": {"jscore:carbon_dioxide": 0.95, "jscore:nitrogen": 0.03, "jscore:argon": 0.02}}
 * {"gravity": 0.9, "pressure": 9300, "temperature": 464,                                     // Venus: hell below,
 *  "bands": [{"at": 0, "pressure": 9300, "temperature": 464}, {"at": 220, "pressure": 101, "temperature": 30}]}
 * }</pre>
 *
 * @param gravity     how hard it pulls, as a share of the overworld's pull
 * @param breathable  whether its air can be breathed, when its air is not said
 * @param temperature how hot it is, in degrees Celsius
 * @param weather     what its weather does
 * @param pressure    how thick its atmosphere is, in kilopascals: the overworld's is 101.3
 * @param clock       its own clock, if it keeps one; never the overworld's, whose day no rule touches
 * @param wind        how hard its wind blows, as a share of the overworld's
 * @param air         what its air is made of, if said: then whether it can be breathed is read from its oxygen
 * @param bands       how it changes with height, the lowest first
 * @param laws        laws of its own over the whole of it, if any
 */
public record DimensionRules(double gravity, boolean breathable, double temperature, Weather weather,
                             double pressure, Optional<DimensionClock> clock, double wind, Optional<AirMix> air,
                             List<AltitudeBand> bands, Optional<Laws> laws) {

    /** Hotter than this, a player not made for it burns. */
    public static final double SCORCHING = 60.0;
    /** Colder than this, a player not made for it freezes. */
    public static final double FREEZING = -30.0;

    /** The overworld's rules, which every dimension without a file of its own keeps. */
    public static final DimensionRules OVERWORLD = new DimensionRules(1.0, true, 15.0, Weather.NATURAL, 101.3,
            Optional.empty(), 1.0, Optional.empty(), List.of(), Optional.empty());
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
                    .forGetter(DimensionRules::wind),
            AirMix.CODEC.optionalFieldOf("air").forGetter(DimensionRules::air),
            AltitudeBand.CODEC.listOf().optionalFieldOf("bands", List.of()).forGetter(DimensionRules::bands),
            Laws.CODEC.optionalFieldOf("laws").forGetter(DimensionRules::laws)
    ).apply(instance, DimensionRules::new));

    public DimensionRules {
        final List<AltitudeBand> ordered = new ArrayList<>(bands);
        ordered.sort(Comparator.comparingDouble(AltitudeBand::at));
        bands = List.copyOf(ordered);
    }

    /** Whether a player not made for it burns here, at its ground. */
    public boolean scorching() {
        return temperature > SCORCHING;
    }

    /** Whether a player not made for it freezes here, at its ground. */
    public boolean freezing() {
        return temperature < FREEZING;
    }

    /**
     * What it is like at the height {@code y}: its pressure and temperature run straight between the bands either side,
     * the nearest band's beyond them, its own where no band says; its air is the band's below, else its own; and it can
     * be breathed by the oxygen's partial pressure there when its air is said, else as its rules say.
     */
    public Climate at(final double y) {
        AltitudeBand low = null;
        AltitudeBand high = null;
        for (final AltitudeBand band : this.bands) {
            if (band.at() <= y) {
                low = band;
            } else {
                high = band;
                break;
            }
        }
        final double kpa = between(y, low, high, AltitudeBand::pressure, this.pressure);
        final double celsius = between(y, low, high, AltitudeBand::temperature, this.temperature);
        final AltitudeBand below = low != null ? low : high;
        final Optional<AirMix> mix = below == null ? this.air : below.air().or(() -> this.air);
        return new Climate(kpa, celsius, mix, mix.map(made -> made.breathable(kpa)).orElse(this.breathable));
    }

    /**
     * How hard it pulls at the height {@code y}, as a share of the overworld's pull: its own pull, by the bands' share
     * of it there, running straight between the bands either side, the nearest band's beyond them.
     */
    public double gravityAt(final double y) {
        AltitudeBand low = null;
        AltitudeBand high = null;
        for (final AltitudeBand band : this.bands) {
            if (band.at() <= y) {
                low = band;
            } else {
                high = band;
                break;
            }
        }
        return gravity * between(y, low, high, AltitudeBand::gravity, 1.0);
    }

    /* A band's value at {@code y}: straight between the bands either side, the nearer's beyond, its own unsaid. */
    private static double between(final double y, final AltitudeBand low, final AltitudeBand high,
                                  final Function<AltitudeBand, Optional<Double>> value, final double own) {
        if (low == null && high == null) {
            return own;
        }
        if (low == null) {
            return value.apply(high).orElse(own);
        }
        if (high == null) {
            return value.apply(low).orElse(own);
        }
        return AirMath.between(y, low.at(), value.apply(low).orElse(own), high.at(), value.apply(high).orElse(own));
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
