/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.space.JsSpace;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SpaceClimbs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * A rocket's engines seen from outside as they burn, a second at a time, as the approved looks draw them: in the air,
 * a bright flame with smoke pouring off it and a white trail left hanging, thinner as the air thins on the way up;
 * where the air is nearly gone, the short hot core and the wide pale plume a nozzle gives in a vacuum, whatever engine
 * it is, as the ambient pressure shapes a plume and not the engine.
 */
public final class ClimbEffects {

    /** An engine burning in air, and in a near vacuum; the trail of a climb. */
    public static final ResourceLocation IN_AIR = id("plume_sea_level");
    public static final ResourceLocation IN_VACUUM = id("plume_vacuum");
    public static final ResourceLocation TRAIL = id("climb_trail");
    /* Thinner than this share of the sea level's air, a plume opens wide as in a vacuum; the least trail drawn. */
    private static final double THIN = 0.25;
    private static final double FAINTEST = 0.05;
    /* How far the plumes and the trail are seen, in blocks. */
    private static final int SEEN = 256;

    private ClimbEffects() {
    }

    /** {@code body}'s engines burn this second at its foot: their plume, and in air their trail. */
    public static void burning(final ServerLevel level, final MovingStructure body) {
        final Vec3 foot = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        final double air = airAt(level, foot.y);
        Effects.at(level, plumeIn(air), foot).facing(new Vec3(0, -1, 0)).following(body.entity()).reach(SEEN)
                .send();
        if (air > FAINTEST) {
            Effects.at(level, TRAIL, foot).facing(new Vec3(0, -1, 0)).scale((float) Math.min(1, air)).reach(SEEN)
                    .send();
        }
    }

    /** The plume of an engine burning in {@code air}, the share of the sea level's air round it. */
    public static ResourceLocation plumeIn(final double air) {
        return air < THIN ? IN_VACUUM : IN_AIR;
    }

    /**
     * The share of the sea level's air at the height {@code y} of {@code level}: the world's own air, thinning as one
     * climbs from its sea to where it becomes its space.
     */
    public static double airAt(final ServerLevel level, final double y) {
        final double sea = FarMaps.seaOf(level);
        final double up = Math.clamp((y - sea) / Math.max(1, SpaceClimbs.ceiling(level) - sea), 0, 1);
        return StageMath.air(DimensionRulesData.of(level).pressure()) * (1 - up);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
