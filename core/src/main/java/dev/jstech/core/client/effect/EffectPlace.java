/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.effect.EffectPhysics;
import dev.jstech.core.heat.HeatClimate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What acts on an effect's particles where it plays, worked out once a tick: the dimension's pull, the air's density
 * from the place's pressure and warmth, the wind, and how much wider a plume grows in that air.
 *
 * @param physics  the place, as the physics reads it
 * @param wind     the wind there
 * @param widening how much wider a plume grows there than at sea level
 */
record EffectPlace(EffectPhysics.Place physics, Wind wind, double widening) {

    /** Still air at sea level. */
    static final EffectPlace STILL = new EffectPlace(EffectPhysics.Place.SEA_LEVEL, Wind.STILL, 1.0);

    /** The place at {@code at} in {@code level} now. */
    static EffectPlace of(final Level level, final Vec3 at) {
        final HeatClimate.Air air = HeatClimate.air(level, BlockPos.containing(at));
        final Wind wind = Winds.at(level, at);
        final EffectPhysics.Place physics = new EffectPhysics.Place(EffectPhysics.EARTH_GRAVITY
                * DimensionRulesData.of(level).gravity(), EffectPhysics.airDensity(air.kiloPascals(), air.kelvin()),
                wind.x(), wind.z());
        return new EffectPlace(physics, wind, EffectPhysics.widening(physics));
    }

    /** The same place, but in water, where nothing blows. */
    EffectPhysics.Place underwater() {
        return new EffectPhysics.Place(this.physics.gravity(), EffectPhysics.WATER, 0, 0);
    }
}
