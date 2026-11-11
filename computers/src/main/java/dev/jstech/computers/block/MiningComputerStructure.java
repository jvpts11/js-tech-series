/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.core.bigblock.BigBlockFootprint;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.multiblock.IBlockMatcher;
import dev.jstech.core.multiblock.MultiblockPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The blocks a Mining Computer stands on (computers/docs/design/teracoin.md): the Transition's rig in one block, the
 * Standard's and the Advanced's two blocks wide, since six and eight cards standing on their risers, at the scale of
 * the mod's parts, do not fit in one. The origin is the block the player places; the second stands east of it when the
 * rig faces north, on the left of a player facing its front.
 */
public final class MiningComputerStructure {

    public static final MultiblockPattern SINGLE = MultiblockPattern.builder("mining_computer")
            .layer("#")
            .build();
    public static final MultiblockPattern WIDE = MultiblockPattern.builder("mining_computer_wide")
            .layer("#P")
            .where('P', IBlockMatcher.any())
            .build();
    public static final BigBlockFootprint SINGLE_FOOTPRINT = new BigBlockFootprint(SINGLE);
    public static final BigBlockFootprint WIDE_FOOTPRINT = new BigBlockFootprint(WIDE);

    private MiningComputerStructure() {
    }

    /** The footprint of the rig of {@code era}: one block for the Transition's, two for the later ones. */
    public static BigBlockFootprint of(final Era era) {
        return era.isAtMost(HardwareEra.TRANSITION) ? SINGLE_FOOTPRINT : WIDE_FOOTPRINT;
    }

    /** Every block of a rig of {@code era} at {@code origin}, facing {@code facing}, the origin first. */
    public static List<BlockPos> allPositions(final BlockPos origin, final Direction facing, final Era era) {
        return of(era).allPositions(origin, facing);
    }

    /**
     * The origin of the wide rig whose second block stands at {@code part}, the rig facing {@code facing}: the block
     * west of it when the rig faces north. Read from the block alone, as the player's game does, which is not told
     * where a part's origin is.
     */
    public static BlockPos originOf(final BlockPos part, final Direction facing) {
        return part.relative(facing.getCounterClockWise());
    }

    /** How many risers the rig of {@code era} carries: four, six or eight. */
    public static int risers(final Era era) {
        return RigGeometry.of(era).risers();
    }
}
