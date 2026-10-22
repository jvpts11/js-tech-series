/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

/**
 * How many chunks the series' machines may keep loaded, as the balance file sets it: each owner's number, across every
 * dimension, the server's budget across every owner together, and whether, and for how long, an owner's chunks stay
 * loaded while they are away ({@link AwayOwners}). Kept apart from {@link ChunkLoaders} so the settings can be read
 * without the game's classes.
 */
public final class ChunkLoadingBalance {

    /** How many chunks an owner keeps loaded unless the balance file says otherwise. */
    public static final int DEFAULT_LIMIT = 25;
    /** How many chunks every owner together keeps loaded unless the balance file says otherwise. */
    public static final int DEFAULT_SERVER_BUDGET = 400;
    /** How many ticks an hour of the server's running is. */
    public static final long TICKS_AN_HOUR = 72_000L;

    private static volatile int limit = DEFAULT_LIMIT;
    private static volatile int serverBudget = DEFAULT_SERVER_BUDGET;
    private static volatile boolean keepWhileAway = true;
    private static volatile int awayHours;

    private ChunkLoadingBalance() {
    }

    public static int limit() {
        return limit;
    }

    /** Sets the limit; a negative one is taken as none at all. */
    public static void setLimit(final int value) {
        limit = Math.max(0, value);
    }

    /** How many chunks every owner together may keep loaded on the server. */
    public static int serverBudget() {
        return serverBudget;
    }

    /** Sets the server's budget; a negative one is taken as none at all. */
    public static void setServerBudget(final int value) {
        serverBudget = Math.max(0, value);
    }

    /** Whether machines keep their chunks loaded while their owner is away from the server. */
    public static boolean keepWhileAway() {
        return keepWhileAway;
    }

    public static void setKeepWhileAway(final boolean value) {
        keepWhileAway = value;
    }

    /** For how many hours of the server's running an owner's chunks stay loaded once they leave; 0 for no end. */
    public static int awayHours() {
        return awayHours;
    }

    /** Sets the hours; a negative number is taken as no end. */
    public static void setAwayHours(final int hours) {
        awayHours = Math.max(0, hours);
    }

    /**
     * Whether an owner away for {@code ticksAway} ticks of the server's running has their chunks let go of: at once
     * when machines keep nothing while their owner is away, else once they have been away the hours allowed, never
     * when no end is set.
     */
    public static boolean letsGoAfter(final long ticksAway) {
        if (!keepWhileAway) {
            return true;
        }
        return awayHours > 0 && ticksAway >= awayHours * TICKS_AN_HOUR;
    }
}
