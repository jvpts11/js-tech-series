/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A block with parts that stand out of its cube, such as a tray slid out of a drive or a stick in a port, which a
 * player looks at and works though they are outside the block's own space.
 *
 * <p>The game aims only at a block's cube, so a look at one of these parts went past it to whatever was behind. With
 * this, a look that crosses a part nearer than anything else lands on the block the part belongs to, at the point it
 * meets the part: the block's own outline and its own click handling then find the part by that point.
 */
public interface IStandingParts {

    /**
     * The parts standing out of the block as it is now, each a box in the block's own coordinates (a block spans 0 to
     * 1; a part out of it reaches past that). Asked on the client every frame a look passes near the block, so it is
     * kept cheap: what the block already holds, and no allocation when nothing stands out.
     */
    List<AABB> standingParts(BlockState state, BlockGetter level, BlockPos pos);
}
