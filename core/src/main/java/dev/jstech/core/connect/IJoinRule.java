/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks a block's faces join with, so a texture runs across them as one surface: a wall of monitors, the
 * casing of a structure.
 */
@FunctionalInterface
public interface IJoinRule {

    /** Whether a block in {@code self} joins its neighbour in {@code other}. */
    boolean joins(BlockState self, BlockState other);

    /** Blocks of the same block join, whatever their states. */
    static IJoinRule sameBlock() {
        return (self, other) -> self.getBlock() == other.getBlock();
    }
}
