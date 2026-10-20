/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import net.minecraft.core.BlockPos;

/**
 * A block entity that keeps the places of other blocks, told when it moves with a structure: any mod's block works
 * aboard as it is, and the few that remember where something else is put that right here. A mod whose block entity it
 * cannot change listens for {@link StructureBlocksMovedEvent} instead.
 *
 * <pre>{@code
 * public void moved(BlockPos was, BlockMove move) {
 *     linked = move.where(linked);            // follows the block it is linked to, if that moved too
 * }
 * }</pre>
 */
public interface IMovesWithStructures {

    /** This block entity was moved, from {@code was} to where it is now, with the other blocks of {@code move}. */
    void moved(BlockPos was, BlockMove move);
}
