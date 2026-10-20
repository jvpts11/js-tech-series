/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.StructureEffectsPayload;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * What a structure moving by its physics shows of itself going, worked out at the end of its tick for its players'
 * games to draw: its engines burning and how hard, its faces glowing hot from the air they head into, and where the
 * air is cold enough, the trails behind its engines and its wingtips. A contrail forms where the water of an
 * engine's exhaust, and of the air spun off a wingtip, freezes: in air below about 40 degrees under freezing, at a
 * speed of an aircraft's, as Appleman found.
 */
final class StructureShowing {

    /*
     * Air colder than this, in kelvin, freezes the water of a trail; a structure going slower than this, in metres a
     * second, leaves none.
     */
    private static final double TRAIL_COLD = 233;
    private static final double TRAIL_SPEED = 40;
    /* Air thinner than this, in kilograms a cubic metre, has too little water for a trail. */
    private static final double TRAIL_AIR = 0.05;
    /* How finely a face's heat is told, in kelvin: finer than this changes nothing drawn. */
    private static final int KELVIN_STEP = 25;

    private StructureShowing() {
    }

    /**
     * What the structure of entity {@code entity} standing at {@code frame}, moving as {@code body}, shows now, by the
     * forces and the heat it was last worked with.
     */
    static StructureEffectsPayload of(final ServerLevel level, final int entity, final StructureFrame frame,
                                      final StructureForces forces, final StructureHeating heating,
                                      final RigidBody body) {
        final List<StructureEffectsPayload.Engine> engines = new ArrayList<>();
        for (final Long2FloatMap.Entry engine : forces.engines().long2FloatEntrySet()) {
            final int strength = Math.round(engine.getFloatValue() * StructureEffectsPayload.STEPS);
            if (strength > 0 && engines.size() < StructureEffectsPayload.MOST) {
                engines.add(new StructureEffectsPayload.Engine(engine.getLongKey(),
                        (byte) Math.min(StructureEffectsPayload.STEPS, strength),
                        look(forces.engineKind(engine.getLongKey()))));
            }
        }
        // By each face's lasting number in the game, down, up, north, south, west and east.
        final Integer[] byFace = new Integer[Direction.values().length];
        final Map<Direction, Double> hot = heating.kelvins();
        for (final Direction face : Direction.values()) {
            final Double kelvin = hot.get(face);
            byFace[face.get3DDataValue()] = kelvin == null ? 0 : (int) Math.round(kelvin / KELVIN_STEP) * KELVIN_STEP;
        }
        final List<Integer> kelvins = List.of(byFace);
        final double speed = body.velocity().length();
        final double height = body.position().y;
        final boolean contrail = speed > TRAIL_SPEED && PhysicsWorld.airTemperature(level, height) < TRAIL_COLD
                && PhysicsWorld.airDensity(level, height) > TRAIL_AIR;
        final List<Long> trails = contrail ? trails(frame, forces, engines, body) : List.of();
        return new StructureEffectsPayload(entity, engines, kelvins, contrail, trails);
    }

    /* What an engine of the kind {@code kind} looks like burning. */
    private static byte look(final Aerodynamics.ThrustKind kind) {
        return switch (kind) {
            case ROCKET -> StructureEffectsPayload.Engine.ROCKET;
            case JET -> StructureEffectsPayload.Engine.JET;
            case PROPELLER -> StructureEffectsPayload.Engine.PROPELLER;
        };
    }

    /*
     * Where its trails come from: each engine burning that burns, and the two wingtips furthest out across the way it
     * goes.
     */
    private static List<Long> trails(final StructureFrame frame, final StructureForces forces,
                                     final List<StructureEffectsPayload.Engine> engines, final RigidBody body) {
        final List<Long> out = new ArrayList<>();
        for (final StructureEffectsPayload.Engine engine : engines) {
            if (engine.look() != StructureEffectsPayload.Engine.PROPELLER) {
                out.add(engine.pos());
            }
        }
        if (forces.wings().isEmpty()) {
            return out;
        }
        final StructurePose pose = frame.pose();
        final Quaterniond turn = new Quaterniond(pose.qx(), pose.qy(), pose.qz(), pose.qw());
        final Vector3d forward = turn.transformInverse(new Vector3d(body.velocity()));
        final Vector3d across = forward.cross(turn.transformInverse(new Vector3d(0, 1, 0)), new Vector3d());
        if (across.lengthSquared() == 0) {
            return out;
        }
        long left = forces.wings().getLong(0);
        long right = left;
        double least = Double.MAX_VALUE;
        double most = -Double.MAX_VALUE;
        for (int i = 0; i < forces.wings().size(); i++) {
            final long wing = forces.wings().getLong(i);
            final double side = across.x * BlockPos.getX(wing) + across.y * BlockPos.getY(wing)
                    + across.z * BlockPos.getZ(wing);
            if (side < least) {
                least = side;
                left = wing;
            }
            if (side > most) {
                most = side;
                right = wing;
            }
        }
        out.add(left);
        if (right != left) {
            out.add(right);
        }
        return out.size() > StructureEffectsPayload.MOST ? out.subList(0, StructureEffectsPayload.MOST) : out;
    }
}
