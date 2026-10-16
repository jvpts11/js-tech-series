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
 * A place the end of a run hands its items to: the block, and the face of it they go in by.
 *
 * @param cell the block that takes them
 * @param side the face of it they go in by
 */
public record ConveyorOutlet(BlockPos cell, Direction side) {

    public ConveyorOutlet {
        cell = Objects.requireNonNull(cell, "cell").immutable();
        Objects.requireNonNull(side, "side");
    }
}
