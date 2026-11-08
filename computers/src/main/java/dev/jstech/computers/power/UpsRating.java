/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.power;

/**
 * What a UPS holds and carries (computers/docs/design/power.md): the joules its battery holds, and the most watts it
 * carries before it goes into bypass, as a real one does. An empty battery charges in ten game minutes, drawn on top of
 * the load. Estimates, as every number of the series is.
 *
 * <p>Pure: no game types.
 *
 * @param batteryJoules what a full battery holds
 * @param carryWatts    the most it carries; above it the load runs straight from the line, unprotected
 */
public record UpsRating(long batteryJoules, int carryWatts) {

    /** The MF PowerGuard 300, of the Vintage and of the Legacy: five minutes at 300 W. */
    public static final UpsRating POWERGUARD_300 = new UpsRating(90_000L, 300);
    /** The MF PowerGuard 500, of the Transition: eight minutes at 500 W. */
    public static final UpsRating POWERGUARD_500 = new UpsRating(240_000L, 500);
    /** The MF PowerGuard 800, of the Standard: ten minutes at 800 W. */
    public static final UpsRating POWERGUARD_800 = new UpsRating(480_000L, 800);
    /** The MF PowerGuard 1500, of the Advanced: fifteen minutes at 1,500 W. */
    public static final UpsRating POWERGUARD_1500 = new UpsRating(1_350_000L, 1_500);
    /** The Rack UPS: fifteen minutes at 3 kW, for the whole rack. */
    public static final UpsRating RACK = new UpsRating(2_700_000L, 3_000);
    /** What every UPS Battery Expansion in the rack adds to its Rack UPS. */
    public static final long EXPANSION_JOULES = 2_700_000L;
    /** The share of a full battery at which the system shuts itself down cleanly, in thousandths. */
    public static final int SHUTDOWN_PERMILLE = 100;

    /* Ten game minutes to charge an empty battery, a tick being a twentieth of a second. */
    private static final long CHARGE_TICKS = 12_000L;
    private static final double TICKS_A_SECOND = 20.0;
    private static final double SECONDS_A_MINUTE = 60.0;
    private static final long PERMILLE = 1_000L;

    public UpsRating {
        if (batteryJoules < 0L || carryWatts < 0) {
            throw new IllegalArgumentException("a UPS holds and carries no less than nothing");
        }
    }

    /** The same rating with {@code extra} joules of battery on top, as expansions add to a Rack UPS. */
    public UpsRating withBattery(final long extra) {
        return new UpsRating(this.batteryJoules + Math.max(0L, extra), this.carryWatts);
    }

    /** What charging takes in a tick, on top of the load: a full battery in ten game minutes. */
    public long chargePerTick() {
        return this.batteryJoules <= 0L ? 0L : Math.max(1L, this.batteryJoules / CHARGE_TICKS);
    }

    /** Whether a load drawing {@code watts} from the line is more than it carries, which puts it into bypass. */
    public boolean bypassedBy(final double watts) {
        return watts > this.carryWatts;
    }

    /** How full {@code charge} joules leave its battery, in thousandths. */
    public int permille(final long charge) {
        return this.batteryJoules <= 0L ? 0
                : (int) Math.max(0L, Math.min(PERMILLE, Math.max(0L, charge) * PERMILLE / this.batteryJoules));
    }

    /** Whether {@code charge} has fallen to where the system shuts itself down cleanly. */
    public boolean shutsDownAt(final long charge) {
        return permille(charge) <= SHUTDOWN_PERMILLE;
    }

    /** How long {@code charge} joules carry a load drawing {@code watts}, in whole minutes; none for no load. */
    public static long minutesLeft(final long charge, final double watts) {
        return watts <= 0.0 ? 0L : (long) (Math.max(0L, charge) / watts / SECONDS_A_MINUTE);
    }

    /** What a tick of a load drawing {@code watts} takes from a battery, in whole joules. */
    public static long tickOf(final double watts) {
        return (long) Math.ceil(Math.max(0.0, watts) / TICKS_A_SECOND);
    }
}
