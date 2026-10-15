/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * The world's rules for the consequences of heat, as its settings file gives them: whether a pipe of a liquid freezes
 * where the place is colder than the liquid's melting point, unless it is insulated, and whether the cold liquids kept
 * in tanks boil off as the heat of the place leaks in.
 */
public final class HeatBalance {

    private static volatile boolean pipesFreeze = true;
    private static volatile boolean boilOff = true;

    private HeatBalance() {
    }

    /** Whether a pipe of a liquid freezes in a place colder than the liquid's melting point, unless insulated. */
    public static boolean pipesFreeze() {
        return pipesFreeze;
    }

    public static void setPipesFreeze(final boolean value) {
        pipesFreeze = value;
    }

    /** Whether a cold liquid kept in a tank boils off as the heat of the place leaks in. */
    public static boolean boilOff() {
        return boilOff;
    }

    public static void setBoilOff(final boolean value) {
        boilOff = value;
    }
}
