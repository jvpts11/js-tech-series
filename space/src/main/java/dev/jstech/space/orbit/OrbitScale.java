/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

/**
 * The scale J's Space squeezes the Solar System to. Distances are squeezed far more than sizes, so worlds look grand
 * and journeys stay short enough to fly: an astronomical unit is about 10,000 blocks of a system's space. Time keeps
 * the Overworld's pace: its day of 24,000 ticks is the Earth's 24 hours, so a tick stands for 3.6 real seconds and an
 * orbit of the International Space Station's 92 minutes takes a little over a minute; and the Earth's year lasts seven
 * of those days, a week, the pace the planets and the launch windows keep.
 */
public final class OrbitScale {

    /** How many blocks of a system's space an astronomical unit is. */
    public static final double BLOCKS_PER_AU = 10_000;
    /** The Overworld's day, the Earth's 24 hours, in ticks. */
    public static final int DAY_TICKS = 24_000;
    /** The Earth's year, seven of the Overworld's days, in ticks. */
    public static final long EARTH_YEAR_TICKS = 7L * DAY_TICKS;
    /** How many real seconds a tick of the game stands for, at the Overworld's day. */
    public static final double SECONDS_PER_TICK = 86_400.0 / DAY_TICKS;
    /** The ticks of a real hour of play, at twenty ticks a second. */
    public static final long TICKS_PER_REAL_HOUR = 20L * 60 * 60;
    /** The pull at the Earth's surface, in metres per second squared. */
    public static final double STANDARD_GRAVITY = 9.80665;

    private OrbitScale() {
    }

    /** {@code realSeconds} of real time, as the game counts it, in ticks. */
    public static double ticksOf(final double realSeconds) {
        return realSeconds / SECONDS_PER_TICK;
    }
}
