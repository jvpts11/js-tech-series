/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A block of a plan that keeps something of its own, as a computer keeps its state or a piece keeps a formed look of
 * its own, so does not merge into its part's formed model, with a block entity or without: told as its part forms and
 * comes apart, it may change how it looks, as one drawn by the model once its part is formed stops drawing itself, or
 * one takes its own formed shape.
 */
public interface IPlanMember {

    /** The part of a plan the block at {@code pos} belongs to has formed, or come apart. */
    void partFormed(ServerLevel level, BlockPos pos, boolean formed);
}
