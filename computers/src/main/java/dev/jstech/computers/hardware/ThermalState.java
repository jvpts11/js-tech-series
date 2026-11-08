/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * How a computer's heat stands, as its screens show it (computers/docs/design/temperature.md): what it reads now, the
 * heat it makes against what it takes away, its surroundings, its fans, and whether it is still cooling from a trip.
 *
 * @param known          whether it has a temperature at all: one cooled from outside it, as a server, has none
 * @param celsius        how hot it is now, in whole °C
 * @param clockPercent   the share of its clock it runs at, in percent
 * @param throttled      whether it is cutting its clock to hold 90 °C
 * @param heatWatts      the heat its parts make, in watts
 * @param budgetWatts    what its case, coolers and fans take away, in watts
 * @param ambient        its surroundings, in whole °C
 * @param fans           the fans turning in its case
 * @param fanSeats       the seats its case has for them, less those radiators take
 * @param bareProcessors the processors seated with no cooler over them
 * @param cooling        whether it tripped and is still too hot to start
 * @param lastTrip       how hot it was at the trip that last shut it down, in whole °C; 0 when none did
 */
public record ThermalState(boolean known, int celsius, int clockPercent, boolean throttled, int heatWatts,
                           int budgetWatts, int ambient, int fans, int fanSeats, int bareProcessors, boolean cooling,
                           int lastTrip) {

    /** The most degrees a screen shows, however hot the sum comes out. */
    public static final int MOST_SHOWN = 999;
    /** A machine with no temperature of its own. */
    public static final ThermalState NONE = new ThermalState(false, 0, 100, false, 0, 0, 0, 0, 0, 0, false, 0);

    /** A machine that has a temperature, read as the arguments say. */
    public ThermalState(final int celsius, final int clockPercent, final boolean throttled, final int heatWatts,
                        final int budgetWatts, final int ambient, final int fans, final int fanSeats,
                        final int bareProcessors, final boolean cooling, final int lastTrip) {
        this(true, celsius, clockPercent, throttled, heatWatts, budgetWatts, ambient, fans, fanSeats, bareProcessors,
                cooling, lastTrip);
    }

    /** What it settles at once it has run long enough with what it holds: how its cooling suits its parts. */
    public ThermalModel.Reading settled() {
        return ThermalModel.read(ambient, ThermalModel.raw(ambient, heatWatts, budgetWatts));
    }

    /**
     * How short its cooling falls of what it holds, which its cooling tab's lamp shows: {@link Lack#NONE} when it
     * settles under 80 °C, {@link Lack#SLOWS} when it settles slowed, {@link Lack#BARE} when a processor has no cooler
     * or it would trip.
     */
    public Lack lack() {
        if (!known) {
            return Lack.NONE;
        }
        final ThermalModel.Reading settled = settled();
        if (bareProcessors > 0 || settled.trips()) {
            return Lack.BARE;
        }
        return settled.slowed() ? Lack.SLOWS : Lack.NONE;
    }

    /** How short a machine's cooling falls of what it holds. */
    public enum Lack {
        NONE,
        SLOWS,
        BARE
    }
}
