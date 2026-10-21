/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.dimension.DimensionRulesData;
import java.util.Optional;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Trailers: one whose hitch is brought near another vehicle's tow point is hitched to it, and follows that point
 * wherever it goes, its axle trailing its own length behind, falling and standing on the ground as it would alone. A
 * trailer pulled too far from it is let go. It follows on the server, and on the game of whoever drives the vehicle
 * ahead, so they see it follow at once.
 */
final class VehicleHitch {

    /* How near a hitch has to be to a tow point to be hitched, and past how far it comes off, in blocks. */
    static final double REACH = 1.5;
    private static final double LETS_GO = 6;
    private static final double LOOK = 6;
    /* What the overworld pulls a vehicle down by each tick, as it pulls a falling block. */
    private static final double PULL = 0.04;

    private VehicleHitch() {
    }

    /** Hitches {@code trailer} to the vehicle whose tow point is nearest its hitch, within reach; whether one was. */
    static boolean hitchNearest(final VehicleEntity trailer, final VehicleDef def) {
        final Optional<Vec3> hitch = def.hitching().hitch();
        if (hitch.isEmpty()) {
            return false;
        }
        final Vec3 at = onVehicle(trailer, hitch.get());
        VehicleEntity nearest = null;
        double best = REACH;
        for (final VehicleEntity other : trailer.level().getEntitiesOfClass(VehicleEntity.class,
                new AABB(at, at).inflate(LOOK), other -> other != trailer && other.towedBy() != trailer)) {
            final VehicleDef kind = other.def();
            if (kind == null || kind.hitching().tow().isEmpty()) {
                continue;
            }
            final double distance = onVehicle(other, kind.hitching().tow().get()).distanceTo(at);
            if (distance <= best) {
                best = distance;
                nearest = other;
            }
        }
        if (nearest == null) {
            return false;
        }
        trailer.hitchTo(nearest);
        return true;
    }

    /** One tick of {@code trailer} following the vehicle it is hitched behind. */
    static void follow(final VehicleEntity trailer, final VehicleDef def, final VehicleEntity leader) {
        final VehicleDef ahead = leader.def();
        final Optional<Vec3> hitch = def.hitching().hitch();
        if (ahead == null || hitch.isEmpty() || ahead.hitching().tow().isEmpty()) {
            letGo(trailer);
            return;
        }
        final Vec3 tow = onVehicle(leader, ahead.hitching().tow().get());
        final double length = Math.max(0.1, hitch.get().z);
        final double[] placed = VehicleMath.follow(tow.x, tow.z, trailer.getX(), trailer.getZ(), length);
        final double dx = placed[0] - trailer.getX();
        final double dz = placed[1] - trailer.getZ();
        if (Math.sqrt(dx * dx + dz * dz) > LETS_GO) {
            letGo(trailer);
            return;
        }
        trailer.setYRot((float) placed[2]);
        final double fall = trailer.getDeltaMovement().y - PULL * DimensionRulesData.of(trailer.level()).gravity();
        trailer.setDeltaMovement(dx, fall, dz);
        trailer.move(MoverType.SELF, trailer.getDeltaMovement());
    }

    /** Where the place {@code at} of {@code vehicle} is in the world, as it is turned now. */
    static Vec3 onVehicle(final VehicleEntity vehicle, final Vec3 at) {
        final Vec3 ahead = Vec3.directionFromRotation(0, vehicle.getYRot());
        final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
        return vehicle.position().add(left.scale(at.x)).add(0, at.y, 0).add(ahead.scale(at.z));
    }

    /** The trailer hitched behind {@code leader}, or none. */
    static @Nullable VehicleEntity trailerOf(final VehicleEntity leader) {
        for (final VehicleEntity other : leader.level().getEntitiesOfClass(VehicleEntity.class,
                leader.getBoundingBox().inflate(LOOK + LETS_GO), other -> other.towedBy() == leader)) {
            return other;
        }
        return null;
    }

    /* Lets the trailer go, on the server; its own game waits to be told. */
    private static void letGo(final VehicleEntity trailer) {
        if (!trailer.level().isClientSide()) {
            trailer.hitchTo(null);
        }
    }
}
