/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block and the six around it: which face of a block another block touches, and telling the six that what a block
 * takes on its faces changed, so a cable beside it shows or hides its connection at once.
 */
public final class Neighbours {

    private Neighbours() {
    }

    /** The face of the block at {@code pos} that touches {@code other}, or null when they do not touch. */
    public static @Nullable Direction faceTowards(final BlockPos pos, final BlockPos other) {
        // The game's own lookup goes by the signs alone, so a block two away would read as touching.
        if (pos.distManhattan(other) != 1) {
            return null;
        }
        return Direction.fromDelta(other.getX() - pos.getX(), other.getY() - pos.getY(), other.getZ() - pos.getZ());
    }

    /**
     * Tells the six blocks around {@code pos} that the block there changed what it takes on its faces without its
     * state changing (turning a block changes its state, and the game tells them by itself), so each works out its
     * shape and its connections again.
     */
    public static void portsChanged(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
    }
}
