/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

/**
 * The world's rules for naming what is discovered, as its settings file gives them: whether a discoverer names it at
 * all, whether a name must end in "-ium" as the elements' names do, and how long a name may be.
 */
public final class NamingBalance {

    /** How many letters a name may have, unless the world says otherwise. */
    public static final int DEFAULT_LONGEST = 20;

    private static volatile boolean discoverersName = true;
    private static volatile boolean requireIum;
    private static volatile int longest = DEFAULT_LONGEST;

    private NamingBalance() {
    }

    /** Whether the discoverer of an element beyond the known table names it; when not, the series' names are used. */
    public static boolean discoverersName() {
        return discoverersName;
    }

    public static void setDiscoverersName(final boolean value) {
        discoverersName = value;
    }

    /** Whether a name must end in "-ium". */
    public static boolean requireIum() {
        return requireIum;
    }

    public static void setRequireIum(final boolean value) {
        requireIum = value;
    }

    /** How many letters a name may have. */
    public static int longest() {
        return longest;
    }

    public static void setLongest(final int value) {
        longest = value;
    }
}
