/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

/** The server's settings for fire: whether what burned leaves its remains, whether the wind spreads fire, and smoke. */
public final class FireBalance {

    private static volatile boolean remains = true;
    private static volatile boolean wind = true;
    private static volatile boolean smoke = true;

    private FireBalance() {
    }

    /** Whether what burned stays burned: charred wood and ash instead of nothing. */
    public static boolean remains() {
        return remains;
    }

    public static void setRemains(final boolean value) {
        remains = value;
    }

    /** Whether the wind spreads a fire faster downwind and makes it leap ahead. */
    public static boolean wind() {
        return wind;
    }

    public static void setWind(final boolean value) {
        wind = value;
    }

    /** Whether fires give off smoke that rises and drifts with the wind. */
    public static boolean smoke() {
        return smoke;
    }

    public static void setSmoke(final boolean value) {
        smoke = value;
    }
}
