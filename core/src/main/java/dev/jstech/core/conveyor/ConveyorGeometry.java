/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The line the items on a piece of conveyor lie along, inside its block, and how they lie on it: where an item stands
 * at each point along the piece, where one lying on it is taken on, and where it is drawn.
 *
 * @param start  where the piece begins, in the block, from 0 to 1 on each axis
 * @param end    where it ends
 * @param facing the way an item on it faces, flat or upright
 * @param pitch  how far an item on it is tipped up from lying flat, front edge first, in degrees: 0 on a flat belt,
 *               45 on a rising one, -45 on a falling one, 90 standing upright
 */
public record ConveyorGeometry(Vec3 start, Vec3 end, Direction facing, float pitch) {

    public ConveyorGeometry {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(facing, "facing");
    }

    /** Where an item {@code at} along the piece stands, from 0 at its start to 1 at its end. */
    public Vec3 at(final double at) {
        return this.start.lerp(this.end, at);
    }

    /** How far along the piece {@code point}, in the block, lies when brought onto its line: from 0 to 1. */
    public double along(final Vec3 point) {
        final Vec3 line = this.end.subtract(this.start);
        final double length = line.lengthSqr();
        return length <= 0.0 ? 0.0 : Math.clamp(point.subtract(this.start).dot(line) / length, 0.0, 1.0);
    }
}
