/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleBiFunction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The wind at any place: what a mod's source says ({@link IWindSource}), else the Core's own ({@link WindMath}) for a
 * dimension with an atmosphere, stronger with height, in its rain and storms, as hard as the dimension's rules say and
 * as its weather blows; none in a vacuum. Smoke drifts with it, and what else the wind moves.
 */
public final class Winds {

    private static final List<IWindSource> SOURCES = new CopyOnWriteArrayList<>();
    private static final List<ToDoubleBiFunction<Level, Vec3>> FACTORS = new CopyOnWriteArrayList<>();

    private Winds() {
    }

    /** Has {@code source} say the wind where it does, before every source handed in before it. */
    public static void source(final IWindSource source) {
        SOURCES.addFirst(source);
    }

    /** The wind at {@code at} in {@code level} now. */
    public static Wind at(final Level level, final Vec3 at) {
        for (final IWindSource source : SOURCES) {
            final Optional<Wind> said = source.at(level, at);
            if (said.isPresent()) {
                return said.get();
            }
        }
        final DimensionRules rules = DimensionRulesData.of(level);
        final double weather = level.isThundering() ? WindMath.STORM : level.isRaining() ? WindMath.RAIN : 1.0;
        double strength = weather * rules.wind();
        for (final ToDoubleBiFunction<Level, Vec3> factor : FACTORS) {
            strength *= factor.applyAsDouble(level, at);
        }
        return WindMath.at(level.getGameTime(), at.y, strength, rules.pressure() > 0);
    }

    /**
     * Has {@code factor} make the Core's own wind blow harder or softer where it says, as a weather blowing a dust
     * storm does; 1 leaves it as it is. A mod's own source of wind is its own to scale.
     */
    public static void factor(final ToDoubleBiFunction<Level, Vec3> factor) {
        FACTORS.add(factor);
    }
}
