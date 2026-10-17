/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRulesData;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The place around a block as heat meets it: how hot it is, and how thick its air is, which says how much the air
 * carries off and whether, in a vacuum, only radiation does. A place's own air comes from the sources declared for it
 * (a sealed room's, a biome's climate), and otherwise from the dimension's rules.
 */
public final class HeatClimate {

    /** Below this pressure, in kilopascals, a place is a vacuum: nothing carries heat off but radiation. */
    public static final double VACUUM_KPA = 0.1;

    private static final List<IClimateSource> SOURCES = new CopyOnWriteArrayList<>();

    private HeatClimate() {
    }

    /** Declares {@code source} of places' own air, after those declared before it. */
    public static void source(final IClimateSource source) {
        SOURCES.add(Objects.requireNonNull(source, "source"));
    }

    /** How hot the place around {@code pos} is, in kelvin. */
    public static double ambient(final Level level, final BlockPos pos) {
        return air(level, pos).kelvin();
    }

    /** How thick the air around {@code pos} is, in kilopascals. */
    public static double pressure(final Level level, final BlockPos pos) {
        return air(level, pos).kiloPascals();
    }

    /** Whether the place around {@code pos} is a vacuum. */
    public static boolean vacuum(final Level level, final BlockPos pos) {
        return pressure(level, pos) < VACUUM_KPA;
    }

    /** The air around {@code pos}: its own, where a source gives it, or its dimension's. */
    public static Air air(final Level level, final BlockPos pos) {
        for (final IClimateSource source : SOURCES) {
            final Air own = source.at(level, pos);
            if (own != null) {
                return own;
            }
        }
        final Climate here = DimensionRulesData.of(level).at(pos.getY() + 0.5);
        return new Air(HeatMath.kelvin(here.temperature()), here.pressure());
    }

    /**
     * The air of a place.
     *
     * @param kelvin      how hot it is
     * @param kiloPascals how thick it is
     */
    public record Air(double kelvin, double kiloPascals) {
    }
}
