/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.space.orbit.Orbit;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.orbit.SolarSystemOrbits;
import dev.jstech.space.orbit.Transfers;
import dev.jstech.space.universe.SolarBody;
import java.util.Optional;

/**
 * A transfer from one world to another as the Flight Computer plans it, at a moment: between two worlds with orbits of
 * their own round the Sun, as the Overworld and Mars, the phase between them now and the one the window opens at, how
 * long until the next window, and the delta-v of the transfer now and in the window; between the Overworld and its
 * Moon, no window, a player goes when they like. With the time it takes and how the rocket arrives: braking in the air
 * where its destination has any, burning its engines where it has none.
 *
 * @param from          the world it leaves
 * @param to            the world it goes to
 * @param windowed      whether it has windows
 * @param phase         the angle the destination stands ahead of the world left, now, in degrees
 * @param lead          the angle it stands ahead at the window's best, in degrees
 * @param inWindow      whether the window is open now
 * @param toWindow      how long until the next window opens, in ticks; nought while one is open
 * @param between       how long from one window to the next, in ticks
 * @param deltaVNow     what the transfer's burn costs now, in metres a second
 * @param deltaVWindow  what it costs in the window
 * @param travel        how long it takes, in ticks
 * @param aerobraking   whether it brakes in the destination's air rather than with its engines
 */
public record TransferPlan(SolarBody from, SolarBody to, boolean windowed, double phase, double lead,
                           boolean inWindow, long toWindow, long between, double deltaVNow, double deltaVWindow,
                           long travel, boolean aerobraking) {

    /**
     * The transfer from {@code from} to {@code to} at the tick {@code tick}, if the computer plans one there: the Moon
     * and the Overworld go round the Sun as one.
     */
    public static Optional<TransferPlan> of(final SolarBody from, final SolarBody to, final long tick) {
        if (from == to) {
            return Optional.empty();
        }
        final boolean aero = to != SolarBody.MOON;
        final Orbit leaving = round(from);
        final Orbit reaching = round(to);
        if (leaving == reaching) {
            // The Overworld and its Moon: no window, the same burn whenever.
            final double burn = Rocketry.LOW_ORBIT_TO_MOON_ORBIT;
            return Optional.of(new TransferPlan(from, to, false, 0, 0, true, 0, 0, burn, burn,
                    SolarSystemOrbits.MOON_TRIP_TICKS, aero));
        }
        final double burn = Rocketry.LOW_ORBIT_TO_MARS_TRANSFER;
        return Optional.of(new TransferPlan(from, to, true, Math.toDegrees(Transfers.phaseAt(leaving, reaching, tick)),
                Math.toDegrees(Transfers.hohmannLead(leaving, reaching)), Transfers.inWindow(leaving, reaching, tick),
                Transfers.inWindow(leaving, reaching, tick) ? 0 : Transfers.ticksToWindow(leaving, reaching, tick),
                Math.round(Transfers.synodicTicks(leaving, reaching)),
                burn * Transfers.deltaVFactor(leaving, reaching, tick), burn, SolarSystemOrbits.MARS_TRIP_TICKS,
                aero));
    }

    /* The orbit round the Sun a world goes on: the Moon's is the Overworld's. */
    private static Orbit round(final SolarBody body) {
        final Orbit own = body.orbit();
        return own == null ? SolarSystemOrbits.EARTH : own;
    }
}
