/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatHazards;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.heat.LevelHeat;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/**
 * The heat of a structure going fast through the air, a re-entry's: each face it heads into the air with takes heat by
 * the root of the air's density and the speed cubed (Sutton and Graves), as much as it faces the way it goes. A block
 * on such a face that holds heat takes it in; one that burns catches fire once the face is as hot as that heat, given
 * off again as it glows, keeps it, so a ship coming down too fast without a heat shield burns.
 */
final class StructureHeating {

    private final Vector3d way = new Vector3d();
    private final Vector3d middle = new Vector3d();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    /* Less heat than this a square metre, in watts, does nothing worth working out. */
    private static final double NOTICED = 1_000;
    /* How well a face gives off its heat as it glows, and Stefan and Boltzmann's constant. */
    private static final double EMISSIVITY = 0.8;
    private static final double STEFAN_BOLTZMANN = 5.670_374e-8;

    /** Heats the faces of the structure standing at {@code frame}, moving as {@code body}, over {@code seconds}. */
    void heat(final ServerLevel level, final StructureFrame frame, final StructureParts parts, final RigidBody body,
              final double seconds) {
        final double speed = body.velocity().length();
        if (Aerodynamics.heating(PhysicsWorld.airDensity(level, body.position().y), speed) < NOTICED) {
            return;
        }
        for (final Direction face : Direction.values()) {
            if (parts.faceArea(face) <= 0) {
                continue;
            }
            FrameMath.turned(face, frame, way);
            final double facing = way.dot(body.velocity()) / speed;
            if (facing <= 0) {
                continue;
            }
            parts.faceMiddle(face, middle);
            final double height = FrameMath.world(frame, middle, new Vector3d()).y;
            final double flux = Aerodynamics.heating(PhysicsWorld.airDensity(level, height), speed) * facing;
            if (flux < NOTICED) {
                continue;
            }
            final double air = PhysicsWorld.airTemperature(level, height);
            final double kelvin = Math.pow(flux / (EMISSIVITY * STEFAN_BOLTZMANN) + Math.pow(air, 4), 0.25);
            heatFace(level, parts, face, flux, kelvin, seconds);
        }
    }

    /* Each block of the structure whose face looking {@code face} meets the air takes the heat, or burns. */
    private void heatFace(final ServerLevel level, final StructureParts parts, final Direction face,
                          final double flux, final double kelvin, final double seconds) {
        for (final long key : parts.surface()) {
            if (parts.filled(BlockPos.offset(key, face))) {
                continue;
            }
            pos.set(key);
            if (!Loaded.at(level, pos)) {
                continue;
            }
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof IHeatHolder holder) {
                final HeatBody heat = holder.heatBody();
                if (heat != null) {
                    heat.exchange(flux * seconds);
                    LevelHeat.wake(level, pos);
                    continue;
                }
            }
            if (kelvin > HeatHazards.IGNITION) {
                final BlockState state = level.getBlockState(pos);
                HeatHazards.ignite(level, pos.immutable(), state, face, seconds);
            }
        }
    }
}
