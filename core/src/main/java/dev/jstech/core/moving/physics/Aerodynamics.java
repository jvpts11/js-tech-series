/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.id.IStableName;
import org.joml.Vector3d;

/**
 * How air, or water, pushes on what moves through it, in metres, seconds, kilograms and newtons.
 * <ul>
 *   <li>A wing lifts by its area, the air's density, the square of the speed of the air past it and its angle of
 *       attack: in a straight line up to its stall, past which the flow breaks away and it lifts as a flat plate, much
 *       less; and it drags, more as it lifts, and as a flat plate past its stall.</li>
 *   <li>A body drags by the area it shows the flow, the density and the speed squared.</li>
 *   <li>An engine that breathes air gives less as the air thins, and a propeller less again as the craft nears the
 *       speed of the air it throws back; a rocket gives the same anywhere, in a vacuum too.</li>
 *   <li>A body coming down fast through thin air heats by the speed cubed and the root of the density (Sutton and
 *       Graves), the heat of a re-entry.</li>
 * </ul>
 */
public final class Aerodynamics {

    /** The density of air at the sea of the overworld, in kilograms a cubic metre. */
    public static final double SEA_AIR = 1.225;
    /* A dynamic pressure is half the density times the speed squared. */
    private static final double HALF = 0.5;
    /* A wing's span over its chord, and how well it spreads its lift: the drag its lift costs. */
    private static final double ASPECT = 6.0;
    private static final double SPREAD = 0.8;
    /* What a wing drags at no lift; and a flat plate's most lift and drag, broadside and at a slant. */
    private static final double BARE_DRAG = 0.02;
    private static final double PLATE_DRAG = 2.0;
    /* The heat of a re-entry, in watts a square metre, by the root of the density and the speed cubed, a 1 m nose. */
    private static final double SUTTON_GRAVES = 1.74e-4;
    /* Below this the air is still: no force, no angle. */
    private static final double STILL = 1.0e-6;

    private Aerodynamics() {
    }

    /**
     * A wing's lift for every angle of attack, in radians, and its stall: in a straight line up to {@code most} at the
     * stall's angle, past it a flat plate's, a sine of twice the angle at most 1, the flow broken away.
     */
    public static double liftCoefficient(final double angle, final double stall, final double most) {
        final double size = Math.abs(angle);
        if (size <= stall) {
            return most * angle / stall;
        }
        return Math.signum(angle) * Math.min(most, Math.sin(2 * Math.min(size, Math.PI / 2)));
    }

    /** A wing's drag at {@code angle}: bare, and what its lift costs, up to its stall; a flat plate's past it. */
    public static double dragCoefficient(final double angle, final double stall, final double most) {
        final double size = Math.abs(angle);
        if (size <= stall) {
            final double lift = liftCoefficient(angle, stall, most);
            return BARE_DRAG + lift * lift / (Math.PI * ASPECT * SPREAD);
        }
        final double sine = Math.sin(Math.min(size, Math.PI / 2));
        return BARE_DRAG + PLATE_DRAG * sine * sine;
    }

    /**
     * The force of the air on a wing, added to {@code into}: {@code air} is how fast the air goes past it, in metres
     * a second, {@code normal} the way its face looks, of unit length, {@code area} its area in square metres. The
     * lift is square to the air, on the face's side the air comes from below; the drag goes with the air.
     */
    public static Vector3d wing(final Vector3d air, final Vector3d normal, final double area, final double density,
                                final double stall, final double most, final Vector3d into) {
        final double speed = air.length();
        if (speed < STILL || density <= 0 || area <= 0) {
            return into;
        }
        final double pressure = HALF * density * speed * speed;
        final Vector3d along = new Vector3d(air).div(speed);
        final double facing = along.dot(normal);
        final double angle = Math.asin(Math.max(-1, Math.min(1, facing)));
        // The lift's way: the face's look, less its part along the air, so square to the air.
        final Vector3d up = new Vector3d(normal).fma(-facing, along);
        final double upLength = up.length();
        if (upLength > STILL) {
            into.fma(pressure * area * liftCoefficient(angle, stall, most) / upLength, up);
        }
        return into.fma(pressure * area * dragCoefficient(angle, stall, most), along);
    }

    /** The drag of a body showing {@code area} to the flow, of {@code coefficient}, in newtons, along the flow. */
    public static double drag(final double speed, final double density, final double area, final double coefficient) {
        return HALF * density * speed * speed * area * coefficient;
    }

    /**
     * How much of its thrust an engine gives: a rocket all of it anywhere; one breathing air its share of the air's
     * density to the sea's; a propeller that, less again as the craft's speed along it, {@code alongSpeed}, nears
     * the speed it throws the air back at, {@code exhaust}, nothing at or past it.
     */
    public static double thrustShare(final ThrustKind kind, final double density, final double alongSpeed,
                                     final double exhaust) {
        return switch (kind) {
            case ROCKET -> 1;
            case JET -> Math.min(1, density / SEA_AIR);
            case PROPELLER -> Math.min(1, density / SEA_AIR) * (exhaust <= 0 ? 1
                    : Math.max(0, Math.min(1, 1 - alongSpeed / exhaust)));
        };
    }

    /** The heat a body's face heads into the air takes, in watts a square metre: a re-entry's. */
    public static double heating(final double density, final double speed) {
        return SUTTON_GRAVES * Math.sqrt(Math.max(0, density)) * speed * speed * speed;
    }

    /** How an engine throws its thrust. */
    public enum ThrustKind implements IStableName {
        /** Carries what it burns: the same thrust anywhere, a vacuum too. */
        ROCKET("rocket"),
        /** Burns the air it takes in: less as the air thins, none in a vacuum. */
        JET("jet"),
        /** Throws the air back: less as the air thins, and as the craft nears the speed it throws it at. */
        PROPELLER("propeller");

        private final String serializedName;

        ThrustKind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
