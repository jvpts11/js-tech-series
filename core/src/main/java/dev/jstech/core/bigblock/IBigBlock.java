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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A big block: one block that, when placed, takes a fixed footprint bigger than a block (its origin, the cell the
 * player placed, and its parts). Implementing this gives every such block a uniform way to report the cells it
 * occupies and whether it fits at a spot, so placement checks, a placement preview and highlighting work on any big
 * block without knowing which one it is.
 */
public interface IBigBlock {

    /** Every world position it occupies, the origin included, when placed at {@code origin} facing {@code facing}. */
    List<BlockPos> footprint(BlockPos origin, Direction facing);

    /**
     * The footprint cells that are obstructed at {@code origin} facing {@code facing}: every cell other than the
     * origin that is not currently replaceable. The origin is left out because the placement has already checked it.
     * An empty result means the whole footprint can be placed.
     */
    default List<BlockPos> getObstructedBlocks(final BlockGetter level, final BlockPos origin, final Direction facing) {
        final ArrayList<BlockPos> obstructedBlocks = new ArrayList<>();
        for (final BlockPos cell : footprint(origin, facing)) {
            if (!cell.equals(origin) && !level.getBlockState(cell).canBeReplaced()) {
                obstructedBlocks.add(cell);
            }
        }
        return obstructedBlocks;
    }

    /**
     * Outlines each obstructing cell with flame particles, giving the player visual feedback for why a
     * placement was refused. Only the edges of every obstructed cube are traced.
     */
    default void spawnMisplaceParticles(final Level level, final List<BlockPos> obstructedBlocks) {
        for (final BlockPos part : obstructedBlocks) {
            final double x = part.getX();
            final double y = part.getY();
            final double z = part.getZ();
            final double[] offsets = {0F, 0.25, 0.5, 0.75};
            for (final double offset : offsets) {
                final double[][] positions = {
                        {offset, 0, 0}, {0, offset, 0}, {0, 0, offset},
                        {offset, 0, 1}, {1, offset, 0}, {0, 1, offset},
                        {offset, 1, 0}, {0, offset, 1}, {1, 0, offset},
                        {offset, 1, 1}, {1, offset, 1}, {1, 1, offset},
                };
                for (final double[] p : positions) {
                    spawnMisplaceParticle(level, x + p[0], y + p[1], z + p[2]);
                }
            }
        }
    }

    private void spawnMisplaceParticle(final Level level, final double x, final double y, final double z) {
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0D, 0D, 0D);
    }
}
