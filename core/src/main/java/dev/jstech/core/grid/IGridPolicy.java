/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * How what a grid carries is shared along a run, and what its wires plug into: energy, by the share each machine
 * asks; fluids, from outputs to inputs and tanks; nothing, for a grid that only joins. The Core's energy and fluid
 * policies ({@link GridPolicies}) are the defaults; a mod whose line carries something of its own gives its own.
 */
public interface IGridPolicy {

    /** The work that shares what a grid of {@code kind} carries in one dimension, or none for a grid that only joins. */
    Optional<IGridMover> mover(GridKind kind);

    /**
     * Whether a wire of a grid of this policy plugs into the block at {@code pos}, met on its {@code face}: a block that
     * takes or gives what the grid carries, though it is no device of the line.
     */
    default boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
        return false;
    }
}
