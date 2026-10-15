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
 * What a part of the Core's kit is, as its block declares it: a shaft, which turns its neighbours along its axis; a
 * gear on a shaft, which also meshes by its teeth with a gear beside it on a parallel axis; or a gearbox, whose bevel
 * gears turn the four faces around its axis, opposite faces opposite ways. Each with what it weighs in turning and
 * holds back by friction, in real units; an iron shaft a metre long and five centimetres thick weighs about 0.005
 * kilogram square metres and holds back about 0.1 newton-metre in its bearings.
 *
 * @param kind     what it is
 * @param teeth    a gear's teeth; none for the rest
 * @param inertia  what it weighs in turning, in kilogram square metres
 * @param friction what it holds back by friction, in newton-metres
 */
public record MechanicalPartSpec(Kind kind, int teeth, double inertia, double friction) {

    public MechanicalPartSpec {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.GEAR && teeth <= 0 || !(inertia >= 0.0) || !(friction >= 0.0)) {
            throw new IllegalArgumentException("a gear has teeth, and a part an inertia and a friction no less than "
                    + "none");
        }
    }

    /** A shaft. */
    public static MechanicalPartSpec shaft(final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.SHAFT, 0, inertia, friction);
    }

    /** A gear of {@code teeth} on a shaft. */
    public static MechanicalPartSpec gear(final int teeth, final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.GEAR, teeth, inertia, friction);
    }

    /** A gearbox. */
    public static MechanicalPartSpec gearbox(final double inertia, final double friction) {
        return new MechanicalPartSpec(Kind.GEARBOX, 0, inertia, friction);
    }

    /** How a part of it with its axis along {@code axis} meets the neighbour on {@code face}. */
    public ShaftContact contact(final Direction.Axis axis, final Direction face) {
        final boolean along = face.getAxis() == axis;
        return switch (this.kind) {
            case SHAFT -> along ? ShaftContact.axle(axis) : ShaftContact.NONE;
            case GEAR -> along ? ShaftContact.axle(axis) : ShaftContact.teeth(axis, this.teeth);
            case GEARBOX -> {
                if (along) {
                    yield ShaftContact.NONE;
                }
                // Opposite faces turn opposite ways: the face towards the positive end of its axis with the box.
                final ShaftContact axle = ShaftContact.axle(face.getAxis());
                yield face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? axle : axle.reversed();
            }
        };
    }

    /** What a part of the kit is. */
    public enum Kind {
        /** A shaft. */
        SHAFT,
        /** A gear on a shaft. */
        GEAR,
        /** A gearbox. */
        GEARBOX
    }
}
