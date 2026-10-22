/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * How what rides a leaning vehicle leans with it: a seat's place turned as the vehicle's model is drawn (by its
 * heading, then its nose up by its pitch, then leaning left by its roll), and the same turn about where it sits for
 * whoever sits there, so an aircraft banking into a turn banks its pilot with it, as every player sees. Pure; angles
 * in degrees, as the game's are.
 */
public final class VehicleLean {

    /* A model faces north as it is drawn; an entity looking at yaw 0 faces south. */
    private static final double HALF_TURN = 180.0;

    private VehicleLean() {
    }

    /**
     * Where the seat {@code left, up, ahead} of a vehicle's origin is, in the world's axes, the vehicle heading
     * {@code yaw}, its nose up by {@code pitch} and leaning left by {@code roll}.
     */
    public static Vector3d seat(final double left, final double up, final double ahead, final float yaw,
                                final float pitch, final float roll) {
        // In the model's own axes the vehicle faces north, -Z, with its left to the west, -X.
        return body(yaw, pitch, roll).transform(new Vector3d(-left, up, -ahead));
    }

    /**
     * The turn of whoever sits in a vehicle heading {@code yaw} and leaning so, about where they sit: none for a
     * vehicle standing level.
     */
    public static Quaterniond tilt(final float yaw, final float pitch, final float roll) {
        final double heading = Math.toRadians(HALF_TURN - yaw);
        return new Quaterniond().rotationY(heading).rotateX(Math.toRadians(pitch)).rotateZ(Math.toRadians(roll))
                .rotateY(-heading);
    }

    /** Whether a vehicle leaning so leans at all. */
    public static boolean leans(final float pitch, final float roll) {
        return pitch != 0.0F || roll != 0.0F;
    }

    /* The turn the vehicle's model is drawn with. */
    private static Quaterniond body(final float yaw, final float pitch, final float roll) {
        return new Quaterniond().rotationY(Math.toRadians(HALF_TURN - yaw)).rotateX(Math.toRadians(pitch))
                .rotateZ(Math.toRadians(roll));
    }
}
