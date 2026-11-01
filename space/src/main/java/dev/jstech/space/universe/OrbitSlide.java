/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

/**
 * How far a world slides by below one in its low orbit, in radians about its poles, as they go round it: a turn a
 * game day, as the Overworld turns once a day under one who watches it from there. Leaving the orbit downwards, it
 * stops sliding, and the way down to the floor of the orbits carries it on, never back, to the next whole turn, so it
 * comes down over the place below as it was when they came into the orbit.
 */
public final class OrbitSlide {

    /** How long a world takes to slide a whole turn by below one in its low orbit, in ticks: a game day. */
    public static final double TURN_TICKS = 24_000;
    private static final double TURN = 2 * Math.PI;

    private OrbitSlide() {
    }

    /** How far the world has slid by in {@code ticks} since one came into its orbit, in radians. */
    public static double slid(final double ticks) {
        return TURN * ticks / TURN_TICKS;
    }

    /**
     * How far the world is drawn slid by on the way down: {@code slide} as it was when one left the orbit, carried on
     * to the next whole turn as {@code down} runs from 0, at the orbit, to 1, at the floor of the orbits. The further
     * down, the further on, and never back.
     */
    public static double downTo(final double slide, final double down) {
        final double share = Math.clamp(down, 0, 1);
        final double whole = Math.ceil(slide / TURN) * TURN;
        return slide + (whole - slide) * share;
    }
}
