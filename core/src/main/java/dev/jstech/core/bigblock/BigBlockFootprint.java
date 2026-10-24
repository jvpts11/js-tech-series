/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A big block's footprint drawn in letters, as a {@link MultiblockPattern} is: the pattern's {@code #} place is the
 * big block's origin, the cell the player places, and every other letter a part. Places left free
 * ({@link MultiblockPattern#IGNORE_CHAR}) are no cells at all.
 *
 * <p>The drawing faces NORTH, its x running to the right of the origin and its z away from it; placed facing another
 * way, each offset is turned by {@link Rotation#transform}, so the footprint is the same in all four horizontal turns.
 */
public final class BigBlockFootprint implements IBigBlockGeometry {

    private final MultiblockPattern pattern;
    private final int cachedBlockCount;

    public BigBlockFootprint(final MultiblockPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.cachedBlockCount = countNonIgnore(pattern);
    }

    @Override
    public List<BlockPos> allPositions(final BlockPos origin, final Direction facing) {
        return collect(origin, facing, false);
    }

    @Override
    public List<BlockPos> partPositions(final BlockPos origin, final Direction facing) {
        return collect(origin, facing, true);
    }

    @Override
    public int blockCount() {
        return cachedBlockCount;
    }

    private List<BlockPos> collect(final BlockPos origin, final Direction facing, final boolean partsOnly) {
        final Rotation rotation = fromDirection(facing);
        final int cx = pattern.terminalX();
        final int cy = pattern.terminalY();
        final int cz = pattern.terminalZ();
        final List<BlockPos> result = new ArrayList<>(cachedBlockCount);
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    if (pattern.charAt(x, y, z) == MultiblockPattern.IGNORE_CHAR) {
                        continue;
                    }
                    if (partsOnly && x == cx && y == cy && z == cz) {
                        continue;
                    }
                    final int[] rotated = rotation.transform(x - cx, z - cz);
                    result.add(origin.offset(rotated[0], y - cy, rotated[1]));
                }
            }
        }
        return result;
    }

    private static int countNonIgnore(final MultiblockPattern p) {
        int n = 0;
        for (int y = 0; y < p.sizeY(); y++) {
            for (int z = 0; z < p.sizeZ(); z++) {
                for (int x = 0; x < p.sizeX(); x++) {
                    if (p.charAt(x, y, z) != MultiblockPattern.IGNORE_CHAR) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static Rotation fromDirection(final Direction facing) {
        return switch (facing) {
            case NORTH -> Rotation.NORTH;
            case EAST  -> Rotation.EAST;
            case SOUTH -> Rotation.SOUTH;
            case WEST  -> Rotation.WEST;
            default    -> throw new IllegalArgumentException(
                    "a big block's footprint stands on a horizontal direction; got " + facing);
        };
    }
}
