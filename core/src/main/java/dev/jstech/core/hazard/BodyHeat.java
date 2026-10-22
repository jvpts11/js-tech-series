/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

/**
 * How warm a body's core is kept, in real values, pure. A body of seventy kilograms holds about 243 kJ in each kelvin
 * of its core, and its heat goes out through three layers, one after the other: the tissue under its skin, which the
 * body closes in the cold and opens in the heat; what it wears, whose insulation the wind presses through unless it is
 * kept out and water soaks unless it is kept out; and the film of air on it, thinner in the wind, which takes heat by
 * convection and gives and takes it by radiation with what is around, or the water it is in instead. The body makes
 * heat by working, shivers to make more as it cools past 36.8 °C and sweats to lose more as it warms past 37.2 °C, and
 * the sun on it warms it through the same layers. The numbers are estimates, after the physiology of real bodies.
 */
public final class BodyHeat {

    /** A body's core when nothing troubles it, in kelvin: 37 °C. */
    public static final double NORMAL = 310.15;
    /** The water a body is in is never colder than this, in kelvin: liquid water's freezing point. */
    public static final double FREEZING = 273.15;
    /** How much heat a kelvin of a body's core holds, in joules: seventy kilograms at 3,470 J/(kg·K). */
    public static final double HEAT_CAPACITY = 70 * 3_470.0;
    /** A body's skin, in square metres. */
    public static final double AREA = 1.8;
    /* How the tissue under the skin holds heat in, in m²K/W: closed in the cold, open in the heat. */
    private static final double TISSUE_CLOSED = 0.10;
    private static final double TISSUE_OPEN = 0.03;
    private static final double TISSUE_SPAN = 0.2;
    /* The air's film: radiation's part, convection's in still air and as the wind rises (8.3 by the root of its
       speed), all in W/(m²K), and still water's in its place. */
    private static final double RADIATIVE = 4.6;
    private static final double STILL_AIR = 3.0;
    private static final double WIND_CONVECTION = 8.3;
    private static final double WATER = 250.0;
    private static final double SEA_LEVEL_KPA = 101.325;
    /* The sun: what share of it a body takes, and how much of the body it falls on, in square metres. */
    private static final double SUN_TAKEN = 0.7;
    private static final double SUN_AREA = 0.3;
    /* Shivering from 36.8 °C, its most by 35.5 °C; sweating from 37.2 °C, its most by 38.2 °C, both in watts. */
    private static final double SHIVER_FROM = 309.95;
    private static final double SHIVER_FULL = 308.65;
    private static final double SHIVER_MOST = 250.0;
    private static final double SWEAT_FROM = 310.35;
    private static final double SWEAT_FULL = 311.35;
    private static final double SWEAT_MOST = 600.0;
    /* The wind presses through clothes that do not keep it out, taking up to half their insulation by 10 m/s; wet
       clothes that do not keep water out keep a third of theirs. */
    private static final double WIND_PRESS = 0.5;
    private static final double WIND_PRESS_FULL = 10.0;
    private static final double WET_KEEPS = 0.3;

    private BodyHeat() {
    }

    /**
     * A body whose core is {@code coreKelvin}, at {@code place}, wearing {@code clothes} and making {@code work} watts
     * by what it does, after {@code seconds}.
     */
    public static Balance step(final double coreKelvin, final Place place, final Clothes clothes, final double work,
                               final double seconds) {
        final boolean wet = place.inWater() || place.rainedOn();
        double insulation = clothes.resistance();
        if (!place.inWater()) {
            insulation *= 1 - WIND_PRESS * Math.min(1, Math.max(0, place.wind()) / WIND_PRESS_FULL)
                    * (1 - clothes.windproof());
        }
        if (wet) {
            insulation *= WET_KEEPS + (1 - WET_KEEPS) * clothes.waterproof();
        }
        final double air = Math.clamp(place.kiloPascals() / SEA_LEVEL_KPA, 0, 1);
        final double film;
        final double around;
        if (place.inWater()) {
            film = 1 / WATER;
            around = Math.max(FREEZING, place.waterKelvin());
        } else {
            final double convection = air <= 0 ? 0
                    : Math.max(STILL_AIR, WIND_CONVECTION * Math.sqrt(Math.max(0, place.wind()))) * Math.sqrt(air);
            film = 1 / (convection + RADIATIVE);
            around = (convection * place.airKelvin() + RADIATIVE * place.radiantKelvin()) / (convection + RADIATIVE);
        }
        final double through = tissue(coreKelvin) + insulation + film;
        final double toPlace = AREA * (coreKelvin - around) / through;
        // The sun warms the body's outside, and the share that comes in is what its film holds against the rest.
        final double sun = place.inWater() ? 0 : SUN_TAKEN * SUN_AREA * Math.max(0, place.sun()) * film / through;
        final double shiver = SHIVER_MOST * ramp(SHIVER_FROM - coreKelvin, SHIVER_FROM - SHIVER_FULL);
        final double sweat = place.inWater() || air <= 0 ? 0
                : SWEAT_MOST * ramp(coreKelvin - SWEAT_FROM, SWEAT_FULL - SWEAT_FROM);
        final double made = Math.max(0, work) + shiver;
        final double lost = toPlace + sweat - sun;
        return new Balance(coreKelvin + (made - lost) * seconds / HEAT_CAPACITY, made, lost, wet, 1 / insulation);
    }

    /** How fast a body's core warms, in kelvin a minute, while it makes {@code watts} more than it loses. */
    public static double perMinute(final double watts) {
        return watts * 60 / HEAT_CAPACITY;
    }

    /* The tissue's hold: closed below the normal core, open above it, between the two near it. */
    private static double tissue(final double coreKelvin) {
        final double open = ramp(coreKelvin - (NORMAL - TISSUE_SPAN), 2 * TISSUE_SPAN);
        return TISSUE_CLOSED + (TISSUE_OPEN - TISSUE_CLOSED) * open;
    }

    /* How far {@code past} is along {@code span}, from none to all. */
    private static double ramp(final double past, final double span) {
        return Math.clamp(past / span, 0, 1);
    }

    /**
     * Where a body is, as heat meets it.
     *
     * @param airKelvin     the air's temperature
     * @param kiloPascals   how thick the air is: none in a vacuum, where only radiation carries heat
     * @param wind          the wind, in metres a second
     * @param sun           the sunlight falling on it, in watts a square metre
     * @param radiantKelvin the temperature what is around it gives and takes radiation as, warmer by a fire
     * @param inWater       whether it is in water
     * @param waterKelvin   that water's temperature
     * @param rainedOn      whether rain falls on it
     */
    public record Place(double airKelvin, double kiloPascals, double wind, double sun, double radiantKelvin,
                        boolean inWater, double waterKelvin, boolean rainedOn) {
    }

    /**
     * What a body wears against the weather.
     *
     * @param resistance the insulation of its clothes over the whole body, in m²K/W: the shares of the body each piece
     *                   covers by how well it holds heat in; none for a bare body
     * @param windproof  the share of the body whose clothes keep the wind out
     * @param waterproof the share of the body whose clothes keep water out
     */
    public record Clothes(double resistance, double windproof, double waterproof) {

        /** Nothing worn. */
        public static final Clothes NONE = new Clothes(0, 0, 0);
    }

    /**
     * A body's balance over a step.
     *
     * @param coreKelvin its core after it
     * @param made       what it made, working and shivering, in watts
     * @param lost       what it lost, to the place and by sweat, less what the sun gave it, in watts; less than none
     *                   when the place warms it
     * @param wet        whether its clothes are wet
     * @param letsOut    how much heat its clothes let out, as worn now, in W/(m²K); infinite for a bare body
     */
    public record Balance(double coreKelvin, double made, double lost, boolean wet, double letsOut) {

        /** How fast its core changes, in kelvin a minute. */
        public double perMinute() {
            return BodyHeat.perMinute(this.made - this.lost);
        }
    }
}
