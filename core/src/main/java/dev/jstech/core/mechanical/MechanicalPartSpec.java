/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.Objects;
import net.minecraft.core.Direction;

/**
 * What a part of the Core's kit is, as its block declares it, in real units:
 *
 * <ul>
 *   <li>a <b>shaft</b>, which turns its neighbours along its axis, and a <b>flywheel</b>, a shaft that weighs much in
 *       turning, so it smooths its network and keeps it turning after the power is cut;</li>
 *   <li>a <b>gear</b> on a shaft, which also meshes by its teeth with a gear beside it on a parallel axis;</li>
 *   <li>a <b>gearbox</b>, whose bevel gears turn the four faces around its axis, opposite faces opposite ways;</li>
 *   <li>a <b>clutch</b>, a shaft that lets go while redstone powers it;</li>
 *   <li>a <b>brake</b>, a shaft that holds back as much as the redstone it gets says, up to its braking torque;</li>
 *   <li>a <b>speed controller</b>, a shaft whose end towards the positive end of its axis turns up to twice as fast as
 *       the other, as the redstone it gets says, and lets go with none;</li>
 *   <li>a <b>shear pin</b>, a shaft that shears when the torque through it passes its rating, stopping the line and
 *       sparing everything else, as a fuse does;</li>
 *   <li>a <b>pulley</b>, a shaft that a belt or a chain joins to another pulley across a distance.</li>
 * </ul>
 *
 * <p>Each with what it weighs in turning and holds back by friction, and the most torque it carries before it breaks,
 * or none; an iron shaft a metre long and five centimetres thick weighs about 0.005 kilogram square metres and holds
 * back about 0.1 newton-metre in its bearings.
 *
 * @param kind     what it is
 * @param teeth    a gear's teeth; none for the rest
 * @param inertia  what it weighs in turning, in kilogram square metres
 * @param friction what it holds back by friction, in newton-metres
 * @param rating   the most torque it carries before it breaks, in newton-metres; none for no limit
 * @param brake    a brake's torque at full redstone, in newton-metres
 * @param radius   a pulley's radius, in metres
 */
public record MechanicalPartSpec(Kind kind, int teeth, double inertia, double friction, double rating, double brake,
                                 double radius) {

    /** The most a speed controller's end turns for each turn of the other, at full redstone. */
    public static final double MOST_GAIN = 2.0;
    /* The strongest redstone signal. */
    private static final double FULL_SIGNAL = 15.0;

    public MechanicalPartSpec {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.GEAR && teeth <= 0 || kind == Kind.PULLEY && !(radius > 0.0)
                || kind == Kind.SHEAR_PIN && !(rating > 0.0)) {
            throw new IllegalArgumentException("a gear has teeth, a pulley a radius and a shear pin a rating");
        }
        if (!(inertia >= 0.0) || !(friction >= 0.0) || !(rating >= 0.0) || !(brake >= 0.0)) {
            throw new IllegalArgumentException("a part's inertia, friction, rating and brake are no less than none");
        }
    }

    /** A shaft. */
    public static MechanicalPartSpec shaft(final double inertia, final double friction) {
        return of(Kind.SHAFT, inertia, friction);
    }

    /** A flywheel: a shaft of {@code inertia}, much more than a shaft's. */
    public static MechanicalPartSpec flywheel(final double inertia, final double friction) {
        return of(Kind.FLYWHEEL, inertia, friction);
    }

    /** A gear of {@code teeth} on a shaft. */
    public static MechanicalPartSpec gear(final int teeth, final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.GEAR, teeth, inertia, friction, 0.0, 0.0, 0.0);
    }

    /** A gearbox. */
    public static MechanicalPartSpec gearbox(final double inertia, final double friction) {
        return of(Kind.GEARBOX, inertia, friction);
    }

    /** A clutch, which lets go while redstone powers it. */
    public static MechanicalPartSpec clutch(final double inertia, final double friction) {
        return of(Kind.CLUTCH, inertia, friction);
    }

    /** A brake of {@code newtonMetres} at full redstone. */
    public static MechanicalPartSpec brake(final double newtonMetres, final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.BRAKE, 0, inertia, friction, 0.0, newtonMetres, 0.0);
    }

    /** A speed controller, its faster end set by redstone. */
    public static MechanicalPartSpec controller(final double inertia, final double friction) {
        return of(Kind.CONTROLLER, inertia, friction);
    }

    /** A shear pin that shears past {@code newtonMetres}. */
    public static MechanicalPartSpec shearPin(final double newtonMetres, final double inertia,
                                              final double friction) {
        return new MechanicalPartSpec(Kind.SHEAR_PIN, 0, inertia, friction, newtonMetres, 0.0, 0.0);
    }

    /** A pulley of {@code metres} of radius, for a belt or a chain. */
    public static MechanicalPartSpec pulley(final double metres, final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.PULLEY, 0, inertia, friction, 0.0, 0.0, metres);
    }

    /** The same, breaking past {@code newtonMetres} through it, where the server lets parts break. */
    public MechanicalPartSpec rated(final double newtonMetres) {
        return new MechanicalPartSpec(this.kind, this.teeth, this.inertia, this.friction, newtonMetres, this.brake,
                this.radius);
    }

    /**
     * How a part of it with its axis along {@code axis} meets the neighbour on {@code face}, getting {@code signal}
     * of redstone, from 0 to 15.
     */
    public ShaftContact contact(final Direction.Axis axis, final Direction face, final int signal) {
        final boolean along = face.getAxis() == axis;
        return switch (this.kind) {
            case SHAFT, FLYWHEEL, BRAKE, SHEAR_PIN, PULLEY -> along ? ShaftContact.axle(axis) : ShaftContact.NONE;
            case GEAR -> along ? ShaftContact.axle(axis) : ShaftContact.teeth(axis, this.teeth);
            case GEARBOX -> {
                if (along) {
                    yield ShaftContact.NONE;
                }
                // Opposite faces turn opposite ways: the face towards the positive end of its axis with the box.
                final ShaftContact axle = ShaftContact.axle(face.getAxis());
                yield face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? axle : axle.reversed();
            }
            case CLUTCH -> along && signal <= 0 ? ShaftContact.axle(axis) : ShaftContact.NONE;
            case CONTROLLER -> {
                if (!along) {
                    yield ShaftContact.NONE;
                }
                if (face.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    yield ShaftContact.axle(axis);
                }
                yield signal <= 0 ? ShaftContact.NONE
                        : ShaftContact.axle(axis).geared(MOST_GAIN * Math.min(signal, FULL_SIGNAL) / FULL_SIGNAL);
            }
        };
    }

    /** What a brake of it asks at {@code signal} of redstone, in newton-metres; none for any other part. */
    public double braking(final int signal) {
        return this.kind == Kind.BRAKE ? this.brake * Math.min(Math.max(0, signal), FULL_SIGNAL) / FULL_SIGNAL : 0.0;
    }

    private static MechanicalPartSpec of(final Kind kind, final double inertia, final double friction) {
        return new MechanicalPartSpec(kind, 0, inertia, friction, 0.0, 0.0, 0.0);
    }

    /** What a part of the kit is. */
    public enum Kind {
        /** A shaft. */
        SHAFT,
        /** A shaft that weighs much in turning. */
        FLYWHEEL,
        /** A gear on a shaft. */
        GEAR,
        /** A gearbox. */
        GEARBOX,
        /** A shaft that lets go while powered. */
        CLUTCH,
        /** A shaft that holds back as powered. */
        BRAKE,
        /** A shaft whose one end turns faster or slower, as powered. */
        CONTROLLER,
        /** A shaft that shears past its rating. */
        SHEAR_PIN,
        /** A shaft a belt or a chain joins to another. */
        PULLEY
    }
}
