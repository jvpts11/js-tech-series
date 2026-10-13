/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

/**
 * How many chunks one owner may keep loaded through the series' machines, across every dimension, as the balance
 * file sets it. Kept apart from {@link ChunkLoaders} so the settings can be read without the game's classes.
 */
public final class ChunkLoadingBalance {

    /** How many chunks an owner keeps loaded unless the balance file says otherwise. */
    public static final int DEFAULT_LIMIT = 25;

    private static volatile int limit = DEFAULT_LIMIT;

    private ChunkLoadingBalance() {
    }

    public static int limit() {
        return limit;
    }

    /** Sets the limit; a negative one is taken as none at all. */
    public static void setLimit(final int value) {
        limit = Math.max(0, value);
    }
}
