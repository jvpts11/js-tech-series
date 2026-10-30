/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.space.universe.SystemLayout;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Solar storms in a system's space: the Sun throws out a storm of particles, the crew is warned, and while it passes
 * the radiation in the space is many times what it is, coming from the Sun. A body aboard a structure is shielded by
 * the blocks between it and the Sun, its water and its cargo as on Orion's shelter; one out in the open takes it all.
 */
public final class SolarStorms {

    /* The storm's radiation in the open, in millisieverts an hour: tens of millisieverts over its half minute. */
    private static final double OPEN = 3_000;
    /* How far towards the Sun the blocks between a body and it are looked over, in blocks. */
    private static final double LOOKED = 24;
    /* The storm coming or passing in each space: the game's ticks it reaches it at and is gone at. */
    private static final Map<ResourceKey<Level>, long[]> PASSING = new ConcurrentHashMap<>();

    private SolarStorms() {
    }

    /** Lets the storms add to the radiation of every place, once, from the mod's constructor. */
    public static void listen() {
        IonizingRadiation.RAYS.addAmbient(SolarStorms::at);
    }

    /** A storm reaches {@code level}'s space {@code ahead} ticks from now and passes through it for {@code lasts}. */
    public static void coming(final ServerLevel level, final long ahead, final long lasts) {
        final long reaches = level.getGameTime() + ahead;
        PASSING.put(level.dimension(), new long[] {reaches, reaches + lasts});
    }

    /** Whether a storm is coming to {@code level}'s space or passing through it now. */
    public static boolean passing(final Level level) {
        final long[] storm = PASSING.get(level.dimension());
        return storm != null && level.getGameTime() < storm[1];
    }

    /** Whether a storm is passing through {@code level}'s space now, its radiation there. */
    public static boolean raging(final Level level) {
        final long[] storm = PASSING.get(level.dimension());
        return storm != null && level.getGameTime() >= storm[0] && level.getGameTime() < storm[1];
    }

    /* The storm's radiation at {@code at}, shielded by what stands between it and the Sun; none with no storm. */
    private static double at(final ServerLevel level, final Vec3 at) {
        if (!raging(level)) {
            return 0;
        }
        final double[] sun = SystemLayout.sun();
        final Vec3 sunward = new Vec3(sun[0] - at.x, sun[1] - at.y, sun[2] - at.z).normalize();
        for (final IStructureBody body : MovingStructures.near(level, new AABB(at, at).inflate(1))) {
            if (body.worldBox().inflate(1).contains(at)) {
                // Its blocks stand in its yard: the body's place there, and the way to the Sun from it.
                final Vec3 from = MovingStructures.toYard(body, at);
                final Vec3 to = MovingStructures.toYard(body, at.add(sunward.scale(LOOKED)));
                return OPEN * Radiation.transmission(level, from, to, IonizingRadiation.RAYS);
            }
        }
        return OPEN;
    }
}
