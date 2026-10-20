/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Blocks moved together, as a structure is made, set down or taken to another dimension: where each block went, and
 * how they were turned on the way. A block entity that keeps the place of another block, a link, a controller, a
 * network's end, asks this where that block went, and keeps it unchanged when that block did not move with it.
 *
 * @param from     the level the blocks left
 * @param to       the level they came to, the same for a structure made or set down in its dimension
 * @param moved    where each block that moved went, by where it was
 * @param rotation how they were turned on the way, about the up axis
 */
public record BlockMove(Level from, Level to, Map<BlockPos, BlockPos> moved, Rotation rotation) {

    public BlockMove {
        moved = Map.copyOf(moved);
    }

    /** Where the block that was at {@code was} is now: where it went if it moved, else where it was. */
    public BlockPos where(final BlockPos was) {
        return moved.getOrDefault(was, was);
    }

    /** Whether the block that was at {@code was} moved with the others. */
    public boolean movedHere(final BlockPos was) {
        return moved.containsKey(was);
    }
}
