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
 * dimension, and the server's budget across every owner together. Kept apart from {@link ChunkLoaders} so the settings
 * can be read without the game's classes.
 */
public final class ChunkLoadingBalance {

    /** How many chunks an owner keeps loaded unless the balance file says otherwise. */
    public static final int DEFAULT_LIMIT = 25;
    /** How many chunks every owner together keeps loaded unless the balance file says otherwise. */
    public static final int DEFAULT_SERVER_BUDGET = 400;

    private static volatile int limit = DEFAULT_LIMIT;
    private static volatile int serverBudget = DEFAULT_SERVER_BUDGET;

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
}
