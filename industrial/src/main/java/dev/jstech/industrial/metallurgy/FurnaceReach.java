/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

/**
 * What a furnace can melt: the dust of a metal that melts no hotter than iron, the hottest a furnace of the start
 * reaches. Silver, tin, copper and manganese dust go back into ingots in it; titanium and vanadium wait for the
 * electric arc and the vacuum of later furnaces.
 *
 * <p>Pure: no game types.
 */
public final class FurnaceReach {

    /** The hottest a furnace of the start melts, in kelvin: iron's melting point. */
    public static final double HOTTEST = 1_811.0;

    private FurnaceReach() {
    }

    /** Whether a furnace melts the dust of a metal that melts at {@code meltingKelvin}. */
    public static boolean meltsDust(final double meltingKelvin) {
        return meltingKelvin > 0.0 && meltingKelvin <= HOTTEST;
    }
}
