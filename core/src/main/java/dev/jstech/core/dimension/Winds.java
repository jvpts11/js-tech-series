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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The wind at any place: what a mod's source says ({@link IWindSource}), else the Core's own ({@link WindMath}) for a
 * dimension with an atmosphere, stronger in its rain and storms; none in a vacuum. Smoke drifts with it, and what else
 * the wind moves.
 */
public final class Winds {

    private static final List<IWindSource> SOURCES = new CopyOnWriteArrayList<>();

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
        return WindMath.at(level.getGameTime(), at.y, weather, rules.pressure() > 0);
    }
}
