/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.bigblock.BigBlockFootprint;
import dev.jstech.core.multiblock.IBlockMatcher;
import dev.jstech.core.multiblock.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry of the Server Rack, a big block: a 2-wide, 3-tall, 2-deep cabinet (12 blocks), shared with the
 * Supercomputer Node cabinet which uses the same physical footprint.
 *
 * <p>The canonical NORTH-facing layout (x = clockwise from origin, z = depth):
 * <pre>
 *   y=0  z=0 (front): # P
 *        z=1 (back):  P P
 *   y=1  z=0 (front): P P   (and the same for y=2)
 *        z=1 (back):  P P
 * </pre>
 * The origin is at the left column (x=0), ground floor (y=0), front row (z=0).
 */
public final class ServerRackStructure {

    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;
    public static final int DEPTH = 2;
    public static final int BLOCK_COUNT = WIDTH * HEIGHT * DEPTH; // 12

    /**
     * Declarative description of the Server Rack / Supercomputer Node footprint, used by
     * {@link BigBlockFootprint} to compute world positions for any of the four horizontal
     * orientations.
     */
    public static final MultiblockPattern PATTERN = MultiblockPattern.builder("server_rack")
            .layer("#P", "PP")
            .layer("PP", "PP")
            .layer("PP", "PP")
            .where('P', IBlockMatcher.any())
            .build();

    private ServerRackStructure() {
    }

    public static List<BlockPos> allPositions(final BlockPos origin, final Direction facing) {
        final Direction right = facing.getClockWise();
        final Direction back = facing.getOpposite();
        final List<BlockPos> positions = new ArrayList<>(BLOCK_COUNT);
        for (int w = 0; w < WIDTH; w++) {
            for (int h = 0; h < HEIGHT; h++) {
                for (int d = 0; d < DEPTH; d++) {
                    positions.add(origin.relative(right, w).above(h).relative(back, d));
                }
            }
        }
        return positions;
    }

    public static List<BlockPos> partPositions(final BlockPos origin, final Direction facing) {
        final List<BlockPos> positions = allPositions(origin, facing);
        positions.removeIf(pos -> pos.equals(origin));
        return positions;
    }

    public static boolean isTopLayer(final BlockPos origin, final BlockPos part) {
        return part.getY() - origin.getY() == HEIGHT - 1;
    }

    public static boolean isFrontBayBlock(final BlockPos origin, final Direction facing,
                                          final BlockPos part) {
        final int h = part.getY() - origin.getY();
        if (h < 0 || h >= HEIGHT - 1) {
            return false;
        }
        /*
         * The front layer is the origin's own layer along the facing axis: the
         * origin column and the column one step clockwise of it.
         */
        final BlockPos flat = part.below(h);
        return flat.equals(origin) || flat.equals(origin.relative(facing.getClockWise()));
    }

    public static BlockPos bayBlockPos(final BlockPos origin, final Direction facing,
                                       final int w, final int h) {
        return origin.relative(facing.getClockWise(), w).above(h);
    }
}
