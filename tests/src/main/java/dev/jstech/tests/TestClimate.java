/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatMath;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * A cold store of the test mod's own, as a source of places' own air: the places a test chills stand at 30 degrees
 * below zero in the overworld's air, so a test sees what deep cold does without a cold dimension.
 */
public final class TestClimate {

    /** How cold a chilled place is, in kelvin. */
    public static final double COLD = HeatMath.kelvin(-30.0);

    private static final Set<Long> CHILLED = ConcurrentHashMap.newKeySet();

    static {
        HeatClimate.source((level, pos) -> CHILLED.contains(pos.asLong())
                ? new HeatClimate.Air(COLD, HeatMath.SEA_LEVEL_KPA) : null);
    }

    private TestClimate() {
    }

    /** Declares the source, from the test mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Chills the place at {@code pos}, absolute. */
    public static void chill(final BlockPos pos) {
        CHILLED.add(pos.asLong());
    }

    /** Lets the place at {@code pos}, absolute, warm back to the dimension's air. */
    public static void warm(final BlockPos pos) {
        CHILLED.remove(pos.asLong());
    }
}
