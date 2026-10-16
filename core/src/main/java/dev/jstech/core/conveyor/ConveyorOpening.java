/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where items cross from one piece of conveyor into the next: named by the block the next piece's start lies in and
 * the way the items cross. A piece carries on its run into the piece whose entry is its exit. A rising belt's exit is
 * in the block ahead of it and above; a falling belt's entry is in the block above it, where its high end is.
 *
 * @param cell the block the start of a piece coming on here lies in
 * @param way  the way the items cross
 */
public record ConveyorOpening(BlockPos cell, Direction way) {

    public ConveyorOpening {
        cell = Objects.requireNonNull(cell, "cell").immutable();
        Objects.requireNonNull(way, "way");
    }
}
