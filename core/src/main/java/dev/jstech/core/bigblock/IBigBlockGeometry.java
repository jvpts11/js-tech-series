/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * The footprint of one big block as a value, apart from the block itself: a big block hands its geometry to the shared
 * lifecycle, so raising and taking down its parts never has to know which big block it is.
 */
public interface IBigBlockGeometry {

    /** Every cell it occupies, the origin included, when its origin is at {@code origin}. */
    List<BlockPos> allPositions(BlockPos origin, Direction facing);

    /** Every cell but the origin's own: the parts it raises and later takes down. */
    List<BlockPos> partPositions(BlockPos origin, Direction facing);

    /** How many cells it takes, the origin and its parts. */
    int blockCount();
}
