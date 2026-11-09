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

import java.util.List;

/**
 * Geometry of the WAN Gateway Computer, a big block a block wider than the Mainframe: 4 wide, 2 tall, 2 deep (16
 * blocks). Its four ports of the long-distance line are on its back, one in the middle of each lower block.
 *
 * <p>The canonical NORTH-facing layout (x = clockwise from the origin, z = depth):
 * <pre>
 *   y=0  z=0 (front): P P # P
 *        z=1 (back):  1 2 3 4   (the ports, numbered from the left as seen from behind)
 *   y=1  z=0 (front): P P P P
 *        z=1 (back):  P P P P
 * </pre>
 * The origin is the power button's block, the second from the left of the bottom front row seen from the front, as
 * the Mainframe's is its middle one; the line bay with its four line modules stands over the two blocks to its right.
 */
public final class WanGatewayStructure {

    public static final int WIDTH = 4;
    public static final int HEIGHT = 2;
    public static final int DEPTH = 2;
    public static final int BLOCK_COUNT = WIDTH * HEIGHT * DEPTH;
    /** How many ports of the long-distance line it has, and so how many WAN Interface Cards it seats. */
    public static final int PORTS = 4;

    public static final MultiblockPattern PATTERN = MultiblockPattern.builder("wan_gateway")
            .layer("PP#P", "PPPP")
            .layer("PPPP", "PPPP")
            .where('P', IBlockMatcher.any())
            .build();
    public static final BigBlockFootprint FOOTPRINT = new BigBlockFootprint(PATTERN);
    /* The pattern's column of the origin, from which a part's column is counted. */
    private static final int ORIGIN_COLUMN = 2;

    private WanGatewayStructure() {
    }

    public static List<BlockPos> allPositions(final BlockPos origin, final Direction facing) {
        return FOOTPRINT.allPositions(origin, facing);
    }

    /** The block whose back face is port {@code port}, 1 to 4. */
    public static BlockPos portBlock(final BlockPos origin, final Direction facing, final int port) {
        return origin.relative(facing.getClockWise(), port - 1 - ORIGIN_COLUMN)
                .relative(facing.getOpposite(), DEPTH - 1);
    }

    /**
     * The port at {@code part}, 1 to 4, or 0 when that block holds none: a port is the back face of a block of the
     * bottom back row, numbered from the left as seen from behind, where whoever lays the lines stands.
     */
    public static int portAt(final BlockPos origin, final Direction facing, final BlockPos part) {
        final Direction right = facing.getClockWise();
        final Direction back = facing.getOpposite();
        final int x = (part.getX() - origin.getX()) * right.getStepX()
                + (part.getZ() - origin.getZ()) * right.getStepZ();
        final int z = (part.getX() - origin.getX()) * back.getStepX()
                + (part.getZ() - origin.getZ()) * back.getStepZ();
        final int column = x + ORIGIN_COLUMN;
        if (part.getY() != origin.getY() || z != DEPTH - 1 || column < 0 || column >= WIDTH) {
            return 0;
        }
        // Seen from behind, the pattern's first column (the most anticlockwise) is on the left.
        return column + 1;
    }
}
