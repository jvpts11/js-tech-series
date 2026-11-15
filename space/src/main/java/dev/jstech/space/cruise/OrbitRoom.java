/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.universe.SystemLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Where a rocket comes to rest in a world's low orbit, climbing into it or coming to the end of its cruise there: at
 * the spot it was headed for, or, where rockets rest there or are bound there already, the nearest spot round it with
 * room for it, in rings a rocket's width apart, so no two rockets come to rest one inside the other.
 */
public final class OrbitRoom {

    /*
     * How much room is kept between two rockets resting in one orbit, in blocks; how many rings of spots round the
     * spot headed for one looks over for room, and how many spots the first ring holds.
     */
    private static final double APART = 8;
    private static final int RINGS = 8;
    private static final int FIRST_RING = 6;

    private OrbitRoom() {
    }

    /**
     * Where in a system's space {@code space} the rocket {@code body} comes to rest, headed for the spot {@code headed}
     * of the low orbit of {@code world}: the spot itself, or the nearest round it with room for it.
     */
    public static double[] spotFor(final ServerLevel space, final MovingStructure body, final double[] headed,
                                   final ResourceLocation world) {
        final double room = Math.max(body.worldBox().getXsize(), body.worldBox().getZsize()) + APART;
        final List<double[]> taken = new ArrayList<>();
        for (final IStructureBody other : MovingStructures.near(space, new AABB(headed[0], SystemLayout.ORBIT_Y,
                headed[1], headed[0], SystemLayout.ORBIT_Y, headed[1]).inflate(room * (RINGS + 1)))) {
            if (other instanceof MovingStructure resting && !resting.id().equals(body.id())) {
                taken.add(new double[] {resting.pose().x(), resting.pose().z(),
                        Math.max(resting.worldBox().getXsize(), resting.worldBox().getZsize())});
            }
        }
        // The rockets told resting there, loaded or not: one whose part of space is not loaded is no less there.
        OrbitObjects.restingAround(space.getServer(), world).forEach((id, rest) -> {
            if (!id.equals(body.id().value())) {
                taken.add(new double[] {rest.x(), rest.z(), rest.width()});
            }
        });
        for (final IStructureBody other : LevelMovingStructures.of(space).all()) {
            if (other instanceof MovingStructure bound && !bound.id().equals(body.id())
                    && bound.motion() instanceof RocketCruise cruise && cruise.to().equals(world)) {
                taken.add(new double[] {cruise.toX(), cruise.toZ(), Math.max(bound.worldBox().getXsize(),
                        bound.worldBox().getZsize())});
            }
        }
        for (int ring = 0; ring <= RINGS; ring++) {
            final int spots = ring == 0 ? 1 : FIRST_RING * ring;
            for (int k = 0; k < spots; k++) {
                final double angle = 2 * Math.PI * k / spots;
                final double[] spot = {headed[0] + ring * room * Math.cos(angle),
                    headed[1] + ring * room * Math.sin(angle)};
                if (free(spot, room, taken)) {
                    return spot;
                }
            }
        }
        return headed;
    }

    /** Tells that {@code body} rests at {@code spot} of the low orbit it is in, for others to keep clear of. */
    public static void restsAt(final ServerLevel space, final MovingStructure body, final double[] spot) {
        OrbitObjects.restsAt(space.getServer(), body.id().value(), new OrbitObjects.Rest(spot[0], spot[1],
                Math.max(body.worldBox().getXsize(), body.worldBox().getZsize())));
    }

    /* Whether a rocket {@code room} wide, its room kept, rests at {@code spot} clear of each of {@code taken}. */
    private static boolean free(final double[] spot, final double room, final List<double[]> taken) {
        for (final double[] other : taken) {
            final double apart = (room + other[2] + APART) / 2;
            if ((spot[0] - other[0]) * (spot[0] - other[0]) + (spot[1] - other[1]) * (spot[1] - other[1])
                    < apart * apart) {
                return false;
            }
        }
        return true;
    }
}
