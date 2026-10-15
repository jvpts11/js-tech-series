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
import org.jetbrains.annotations.Nullable;

/**
 * How a part meets its neighbour through one face: by an axle along that face's axis, which turns the neighbour's
 * axle with it, or by the teeth of a gear, which mesh with a gear beside it on a parallel axis and turn it the other
 * way, slower by as many times as it has more teeth; or not at all. A part that turns the other way through a face, as
 * one side of a gearbox does, says so with its sense; one whose face turns faster or slower than the part, as a speed
 * controller's output does, says so with its gain.
 *
 * @param kind  how it meets the neighbour
 * @param axis  the axis it turns about at that face
 * @param teeth how many teeth it has there, for a gear
 * @param sense +1, or -1 where the face turns the other way to the part
 * @param gain  how many turns the face makes for each turn of the part, above none
 */
public record ShaftContact(Kind kind, Direction.Axis axis, int teeth, int sense, double gain) {

    /** A face that meets nothing. */
    public static final ShaftContact NONE = new ShaftContact(Kind.NONE, Direction.Axis.Y, 0, 1, 1.0);

    public ShaftContact {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(axis, "axis");
        if (kind == Kind.TEETH && teeth <= 0 || sense != 1 && sense != -1 || !(gain > 0.0) || !Double.isFinite(gain)) {
            throw new IllegalArgumentException("a gear has teeth, a face turns with its part or against it, and its "
                    + "gain is above none");
        }
    }

    /** An axle along {@code axis}. */
    public static ShaftContact axle(final Direction.Axis axis) {
        return new ShaftContact(Kind.AXLE, axis, 0, 1, 1.0);
    }

    /** The teeth of a gear of {@code teeth} turning about {@code axis}. */
    public static ShaftContact teeth(final Direction.Axis axis, final int teeth) {
        return new ShaftContact(Kind.TEETH, axis, teeth, 1, 1.0);
    }

    /** The same, turning the other way to its part. */
    public ShaftContact reversed() {
        return new ShaftContact(this.kind, this.axis, this.teeth, -this.sense, this.gain);
    }

    /** The same, turning {@code times} as fast as its part. */
    public ShaftContact geared(final double times) {
        return new ShaftContact(this.kind, this.axis, this.teeth, this.sense, times);
    }

    /**
     * How fast the neighbour that meets it with {@code other} turns for each turn of this part: the same with axles on
     * one axis, the other way slower by the teeth with gears on parallel axes, each times the faces' gains; none when
     * they do not meet.
     */
    public @Nullable Double ratioTo(final ShaftContact other) {
        if (this.kind == Kind.NONE || other.kind != this.kind || other.axis != this.axis) {
            return null;
        }
        final double turn = this.kind == Kind.AXLE ? 1.0 : RotationMath.mesh(this.teeth, other.teeth);
        return turn * this.sense * other.sense * this.gain / other.gain;
    }

    /** How a face meets its neighbour. */
    public enum Kind {
        /** It does not. */
        NONE,
        /** By an axle along the face's axis. */
        AXLE,
        /** By the teeth of a gear. */
        TEETH
    }
}
