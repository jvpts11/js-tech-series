/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

/**
 * The wind of a world with air, worked out from the time alone, so the server and every player's game agree without
 * sending it: a few metres a second, its way turning slowly over the days, gusting, stronger in rain and storms and
 * higher up, where the ground holds it back less. Pure.
 */
public final class WindMath {

    /** A plain day's wind at the ground, metres a second. */
    public static final double PLAIN = 3.0;
    /** How much harder it blows in rain, and in a storm. */
    public static final double RAIN = 1.8;
    public static final double STORM = 3.0;
    /** The height the ground no longer holds the wind back above, and how many blocks higher doubles it. */
    public static final int GROUND = 63;
    public static final double DOUBLES_EVERY = 80.0;

    private static final double TICKS_A_DAY = 24_000.0;
    private static final double TURN = 2 * Math.PI;
    /* The slow turn of its way over days, and its gusts over tens of seconds and seconds. */
    private static final double WAY_DAYS = 2.7;
    private static final double WAY_SWING = 0.6;
    private static final double GUST_TICKS = 600.0;
    private static final double SHORT_GUST_TICKS = 170.0;
    private static final double GUST = 0.25;
    private static final double SHORT_GUST = 0.15;
    private static final double PHASE = 1.3;

    private WindMath() {
    }

    /**
     * The wind at {@code gameTime} and height {@code y}, in a world with air ({@code air}), {@code weather} times as
     * strong as a plain day's (1, {@link #RAIN} or {@link #STORM}).
     */
    public static Wind at(final long gameTime, final double y, final double weather, final boolean air) {
        if (!air) {
            return Wind.STILL;
        }
        final double t = gameTime;
        final double way = TURN * (t / (TICKS_A_DAY * WAY_DAYS)) + WAY_SWING * Math.sin(t / TICKS_A_DAY * TURN);
        final double gusts = 1 + GUST * Math.sin(t / GUST_TICKS * TURN) + SHORT_GUST * Math.sin(t / SHORT_GUST_TICKS
                * TURN + PHASE);
        final double height = 1 + Math.max(0, y - GROUND) / DOUBLES_EVERY;
        final double speed = PLAIN * weather * gusts * height;
        return new Wind(Math.sin(way) * speed, -Math.cos(way) * speed);
    }
}
