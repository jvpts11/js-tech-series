/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The place around a block as heat meets it: how hot it is, and how thick its air is, which says how much the air
 * carries off and whether, in a vacuum, only radiation does. Read from the dimension's rules; the biome's climate, the
 * altitude and a sealed room's air come to it as each is built.
 */
public final class HeatClimate {

    /** Below this pressure, in kilopascals, a place is a vacuum: nothing carries heat off but radiation. */
    public static final double VACUUM_KPA = 0.1;

    private HeatClimate() {
    }

    /** How hot the place around {@code pos} is, in kelvin. */
    public static double ambient(final Level level, final BlockPos pos) {
        return HeatMath.kelvin(rules(level).temperature());
    }

    /** How thick the air around {@code pos} is, in kilopascals. */
    public static double pressure(final Level level, final BlockPos pos) {
        return rules(level).pressure();
    }

    /** Whether the place around {@code pos} is a vacuum. */
    public static boolean vacuum(final Level level, final BlockPos pos) {
        return pressure(level, pos) < VACUUM_KPA;
    }

    private static DimensionRules rules(final Level level) {
        return DimensionRulesData.of(level);
    }
}
