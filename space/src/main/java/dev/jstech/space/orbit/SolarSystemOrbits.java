/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

/**
 * The paths of the skeleton's worlds round the Sun, at their real distances, and the usual length of a trip to each,
 * the reference the delta-v spent shortens. The Moon goes round the Overworld, so going there needs no window; its
 * trip lasts about a minute, as Apollo's three days do squeezed, and the trip to Mars about eight minutes, as its eight
 * months do. Where each begins on its path is a choice of the series'.
 */
public final class SolarSystemOrbits {

    /** The Overworld, the Earth, at one astronomical unit. */
    public static final Orbit EARTH = new Orbit(1.0, 0.0);
    /** Mars, at 1.524 astronomical units, its year 1.88 of the Earth's. */
    public static final Orbit MARS = new Orbit(1.524, 0.35);
    /** The usual trip to the Moon and to Mars, in ticks. */
    public static final long MOON_TRIP_TICKS = 1_200;
    public static final long MARS_TRIP_TICKS = 9_600;

    private SolarSystemOrbits() {
    }
}
