/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * How hot a computer runs and what that costs it (computers/docs/design/temperature.md), as a real processor's
 * thresholds: the heat its parts make against the heat it can take away raises it over its surroundings, full speed up
 * to 80 °C, a boost that fades down to 85% of its clock by 90 °C, a clock cut to hold 90 °C there, down to a quarter of
 * it, and a trip at 100 °C when not even a quarter holds the heat.
 *
 * <p>The temperature the load leaves a machine at is worked out as if it ran at full clock, its "raw" temperature: the
 * surroundings plus 60 °C at a load equal to the budget. What the machine really reads is that raw temperature under
 * those thresholds ({@link #read}). Plain arithmetic, tested without the game; the numbers are estimates.
 */
public final class ThermalModel {

    /** Up to this a machine runs at its full clock. */
    public static final double FULL_UP_TO = 80.0;
    /** Here it cuts its clock to hold the temperature. */
    public static final double THROTTLE_AT = 90.0;
    /** Here it trips and shuts down. */
    public static final double TRIP_AT = 100.0;
    /** However hot it gets, a machine that has not tripped keeps this share of its clock. */
    public static final double FLOOR = 0.25;
    /** The rise over the surroundings at a load equal to the budget, in °C. */
    public static final double RISE_AT_BUDGET = 60.0;

    /* The share of its clock a machine keeps at 90 °C before it throttles: its boost gone, 1.5% a degree past 80. */
    private static final double BOOSTLESS = 0.85;
    private static final double BOOST_LOSS = (1.0 - BOOSTLESS) / (THROTTLE_AT - FULL_UP_TO);

    private ThermalModel() {
    }

    /**
     * The surroundings' temperature in a biome of the game's own temperature {@code biome}: {@code base} at 0.8, as in
     * the plains, {@code factor} more for every point warmer, and {@code netherExtra} more in the Nether.
     */
    public static double ambient(final double base, final double factor, final double biome,
                                 final double netherExtra, final boolean nether) {
        return base + factor * (biome - 0.8) + (nether ? netherExtra : 0.0);
    }

    /**
     * The temperature {@code heatWatts} leaves a machine at over {@code ambient} when it takes away
     * {@code budgetWatts}, running at full clock: infinite for heat with nothing to take it away.
     */
    public static double raw(final double ambient, final double heatWatts, final double budgetWatts) {
        if (heatWatts <= 0.0) {
            return ambient;
        }
        return budgetWatts <= 0.0 ? Double.POSITIVE_INFINITY : ambient + RISE_AT_BUDGET * heatWatts / budgetWatts;
    }

    /** What a machine whose raw temperature is {@code raw}, over {@code ambient}, reads and how fast it runs. */
    public static Reading read(final double ambient, final double raw) {
        if (raw <= FULL_UP_TO) {
            return new Reading(raw, 1.0, false, false);
        }
        if (raw <= THROTTLE_AT) {
            return new Reading(raw, 1.0 - BOOST_LOSS * (raw - FULL_UP_TO), false, false);
        }
        // Held at 90 °C: the clock falls to whatever share of the heat that leaves, and no lower than the floor.
        final double rise = raw - ambient;
        final double clock = ambient >= THROTTLE_AT || rise <= 0.0 ? 0.0 : BOOSTLESS * (THROTTLE_AT - ambient) / rise;
        if (clock >= FLOOR) {
            return new Reading(THROTTLE_AT, clock, true, false);
        }
        final double floored = ambient + FLOOR / BOOSTLESS * Math.max(0.0, rise);
        return new Reading(floored, FLOOR, true, floored >= TRIP_AT);
    }

    /** The degrees a reading shows, whole: rounded, and nothing past {@code most} however hot the sum comes out. */
    public static int shown(final double celsius, final int most) {
        return Double.isNaN(celsius) ? 0 : (int) Math.min(most, Math.round(celsius));
    }

    /**
     * What a machine reads.
     *
     * @param celsius   how hot it is, in °C
     * @param clock     the share of its clock it runs at, from {@link #FLOOR} to 1
     * @param throttled whether it is cutting its clock to hold 90 °C
     * @param trips     whether not even the floor holds the heat, so it shuts down
     */
    public record Reading(double celsius, double clock, boolean throttled, boolean trips) {

        /** A machine at rest in surroundings at {@code ambient}. */
        public static Reading resting(final double ambient) {
            return new Reading(ambient, 1.0, false, false);
        }

        /** Whether it runs below its full clock, its boost fading or its clock cut. */
        public boolean slowed() {
            return clock < 1.0;
        }

        /** Its clock in whole percent, rounded: what a screen shows. */
        public int clockPercent() {
            return (int) Math.round(clock * 100.0);
        }
    }
}
