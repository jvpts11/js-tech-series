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
 * where the place is colder than the liquid's melting point, unless it is insulated; whether the cold liquids kept in
 * tanks boil off as the heat of the place leaks in; whether a machine past its heat limit is damaged; whether a block
 * too hot sets alight what burns against it; and whether it burns whoever stands against it.
 */
public final class HeatBalance {

    private static volatile boolean pipesFreeze = true;
    private static volatile boolean boilOff = true;
    private static volatile boolean overheatingDamages = true;
    private static volatile boolean ignites = true;
    private static volatile boolean hotSurfacesBurn = true;

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

    /** Whether a machine past its heat limit loses health; it stops to cool either way. */
    public static boolean overheatingDamages() {
        return overheatingDamages;
    }

    public static void setOverheatingDamages(final boolean value) {
        overheatingDamages = value;
    }

    /** Whether a block past 300 °C sets alight what burns against it. */
    public static boolean ignites() {
        return ignites;
    }

    public static void setIgnites(final boolean value) {
        ignites = value;
    }

    /** Whether a block past 150 °C burns whoever stands against it. */
    public static boolean hotSurfacesBurn() {
        return hotSurfacesBurn;
    }

    public static void setHotSurfacesBurn(final boolean value) {
        hotSurfacesBurn = value;
    }
}
