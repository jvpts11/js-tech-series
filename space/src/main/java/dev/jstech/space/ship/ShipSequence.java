/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

/**
 * The fixed way a small ship on feet comes down onto the land and goes up off it, as the starships of No Man's Sky do:
 * hovering near the land, its pilot holding DOWN, it comes down by itself onto the land under it, slowing till it
 * touches, and settles there on its feet; standing, its pilot holding UP, it goes straight up by itself and hovers,
 * the helm theirs again. Its pilot's keys do nothing meanwhile. How far it has gone is a cubic of the time: coming down
 * it goes on at the speed it was coming down at, and slows to rest where the land is; going up it gathers speed and
 * slows to rest at its height. The figures are estimates, to be weighed in play. Pure: no game types.
 *
 * @param landing whether it comes down; else it goes up
 * @param began   the game's tick it began at
 * @param ticks   how long it takes, in ticks
 * @param depth   how far it goes, in metres
 * @param start   how fast it was going that way as it began, in metres a second
 */
public record ShipSequence(boolean landing, long began, int ticks, double depth, double start) {

    /** How near over the land a ship on feet hovers for DOWN to set it down, in metres. */
    public static final double LANDS_FROM = 30;
    /** How high it goes up off the land, in metres: past where its gear folds away. */
    public static final double RISES = 12;
    /** How fast it comes on down, in metres a second, once its time is up and it has not met the land. */
    public static final double CREEPS = 0.5;
    /* How long it takes to come down from {@link #LANDS_FROM}, less for less, and at the least; and going up. */
    private static final double LANDING_SECONDS = 4;
    private static final double LEAST_SECONDS = 1;
    private static final double RISING_SECONDS = 3;
    private static final double TICKS_A_SECOND = 20;
    /* The most a cubic leaving at the speed it was going may lead by, as a share of its depth, never to overshoot. */
    private static final double MOST_LEAD = 3;

    public ShipSequence {
        if (ticks <= 0 || depth < 0 || start < 0) {
            throw new IllegalArgumentException("a ship's sequence takes some time, and goes no way back");
        }
    }

    /**
     * Coming down from {@code now} the {@code depth} metres onto the land under it, going down {@code descending}
     * metres a second as it begins: four seconds from {@link #LANDS_FROM}, less for less, a second at the least.
     */
    public static ShipSequence landing(final long now, final double depth, final double descending) {
        final double seconds = Math.max(LEAST_SECONDS, LANDING_SECONDS * Math.min(1, depth / LANDS_FROM));
        return new ShipSequence(true, now, (int) Math.round(seconds * TICKS_A_SECOND), Math.max(0, depth),
                Math.max(0, descending));
    }

    /** Going up from {@code now} the {@link #RISES} metres off the land it stands on, from rest. */
    public static ShipSequence takingOff(final long now) {
        return new ShipSequence(false, now, (int) Math.round(RISING_SECONDS * TICKS_A_SECOND), RISES, 0);
    }

    /** How far it has gone at the tick {@code tick}, in metres: nought at its beginning, its depth from its end on. */
    public double gone(final long tick) {
        final double t = Math.clamp((double) (tick - this.began) / this.ticks, 0.0, 1.0);
        final double lead = Math.min(this.start * this.ticks / TICKS_A_SECOND, MOST_LEAD * this.depth);
        return this.depth * (3 * t * t - 2 * t * t * t) + lead * (t * t * t - 2 * t * t + t);
    }

    /** Whether its time is up at the tick {@code tick}. */
    public boolean over(final long tick) {
        return tick - this.began >= this.ticks;
    }
}
