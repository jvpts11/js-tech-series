/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

/**
 * The sums a vehicle moves by: how well its tyres hold on a ground, how fast it turns for its steering and how far its
 * grip lets it, how a slide dies away, what the air and the road take from it, how it leans over its wheels, how a
 * trailer follows the point it is hitched to, how a blow shares out over its parts, and how its engine sounds. Speeds
 * in metres a second, forces in newtons, angles in degrees. Pure: no part of the game.
 */
public final class VehicleMath {

    /** The pull of the overworld, in metres a second squared. */
    public static final double G = 9.81;
    /** The slipperiness of most blocks, as the game gives it: stone, dirt, planks. */
    public static final double PLAIN_SLIPPERINESS = 0.6;
    /* What a tyre loses rolling, as a share of what bears on it. */
    private static final double ROLLING = 0.015;
    /* How far a car's wheels turn at most at walking pace, and how fast that falls off with speed. */
    private static final double MOST_STEER = 35;
    private static final double STEER_FALLS = 15;
    private static final double NONE = 1.0e-9;
    /* A blow is shared over the parts on its side: one at the far side gets this much of what one at the near does. */
    private static final double FAR_SHARE = 0.1;
    /* The engine's sound runs from this pitch at its idle to the second at its top speed. */
    private static final double LOW_PITCH = 0.6;
    private static final double HIGH_PITCH = 1.8;
    private static final double QUIET = 0.35;

    private VehicleMath() {
    }

    /**
     * How well a tyre of {@code grip} holds on a ground of the game's {@code slipperiness}: as it does on stone for
     * most blocks, a tenth as well on ice.
     */
    public static double grip(final double grip, final double slipperiness) {
        final double ground = Math.max(0, 1 - slipperiness) / (1 - PLAIN_SLIPPERINESS);
        return grip * Math.min(1, ground);
    }

    /** How far the wheels turn for a full steer at {@code speed}: less as it goes faster, as a driver steers. */
    public static double steerAngle(final double steer, final double speed) {
        return Math.clamp(steer, -1, 1) * MOST_STEER / (1 + Math.abs(speed) / STEER_FALLS);
    }

    /**
     * How fast it turns, in degrees a second, its front wheels turned {@code angle} degrees on a wheelbase of
     * {@code wheelbase} metres at {@code speed}, as fast as a grip of {@code grip} lets it before it slides wide.
     */
    public static double yawRate(final double speed, final double angle, final double wheelbase, final double grip) {
        if (wheelbase <= NONE) {
            return 0;
        }
        final double rate = speed / wheelbase * Math.tan(Math.toRadians(angle));
        final double most = Math.abs(speed) <= NONE ? Double.MAX_VALUE : grip * G / Math.abs(speed);
        return Math.toDegrees(Math.clamp(rate, -most, most));
    }

    /** What is left of a sideways slide of {@code sideways} after {@code seconds}, held by a grip of {@code grip}. */
    public static double slide(final double sideways, final double grip, final double seconds) {
        final double taken = grip * G * seconds;
        return Math.abs(sideways) <= taken ? 0 : sideways - Math.signum(sideways) * taken;
    }

    /** What holds it back at {@code speed}: the air, by its drag area and the air's density, and its tyres rolling. */
    public static double resistance(final double speed, final double dragArea, final double density,
                                    final double mass, final boolean rolling) {
        final double air = 0.5 * density * dragArea * speed * speed;
        final double road = rolling ? ROLLING * mass * G : 0;
        return (air + road) * Math.signum(speed);
    }

    /**
     * How far it leans, in degrees, from how high its wheels stand: nose up when the front wheels stand higher by
     * {@code rise} over {@code span} metres between them.
     */
    public static double lean(final double rise, final double span) {
        return span <= NONE ? 0 : Math.toDegrees(Math.atan2(rise, span));
    }

    /**
     * Where a trailer stands following the point it is hitched to: its hitch at {@code towX, towZ}, its axle
     * {@code length} behind, pulled round toward where it was. Gives the axle's place and the way it faces, in degrees
     * as the game gives yaw.
     */
    public static double[] follow(final double towX, final double towZ, final double axleX, final double axleZ,
                                  final double length) {
        double dx = towX - axleX;
        double dz = towZ - axleZ;
        final double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= NONE) {
            dx = 0;
            dz = 1;
        } else {
            dx /= distance;
            dz /= distance;
        }
        final double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return new double[] {towX - dx * length, towZ - dz * length, yaw};
    }

    /**
     * How much of a blow each part takes: the parts nearest the side it struck take most, those at the far side a
     * tenth as much; the shares add to 1. {@code along} gives how far along the blow's way each part stands from the
     * vehicle's middle, in metres.
     */
    public static double[] shares(final double[] along) {
        if (along.length == 0) {
            return new double[0];
        }
        double least = Double.MAX_VALUE;
        double most = -Double.MAX_VALUE;
        for (final double each : along) {
            least = Math.min(least, each);
            most = Math.max(most, each);
        }
        final double[] weights = new double[along.length];
        double sum = 0;
        for (int i = 0; i < along.length; i++) {
            final double near = most - least <= NONE ? 1 : (along[i] - least) / (most - least);
            weights[i] = FAR_SHARE + (1 - FAR_SHARE) * near;
            sum += weights[i];
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }
        return weights;
    }

    /** The pitch of the engine's sound turning at {@code rpm}, between its idle and its top speed. */
    public static float enginePitch(final double rpm, final double idle, final double max) {
        final double x = max - idle <= NONE ? 0 : Math.clamp((rpm - idle) / (max - idle), 0, 1);
        return (float) (LOW_PITCH + (HIGH_PITCH - LOW_PITCH) * x);
    }

    /** How loud the engine sounds working at {@code load}: quiet at its idle, full under a full load. */
    public static float engineVolume(final double load) {
        return (float) (QUIET + (1 - QUIET) * Math.clamp(load, 0, 1));
    }
}
