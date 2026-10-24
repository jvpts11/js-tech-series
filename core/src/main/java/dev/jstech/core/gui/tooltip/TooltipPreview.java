/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A turning 3D picture of a block or of a whole structure in a tooltip, as a multiblock's terminal shows the
 * furnace it builds. It turns slowly by itself, and stands still for a player who asked for less movement.
 *
 * @param blocks the blocks, by where each stands
 * @param size   how wide and tall the picture is, in the GUI's pixels
 */
public record TooltipPreview(Map<BlockPos, BlockState> blocks, int size) implements TooltipComponent {

    /** The smallest and the largest a preview is drawn. */
    public static final int LEAST = 16;
    public static final int MOST = 128;

    public TooltipPreview {
        blocks = Map.copyOf(blocks);
        if (blocks.isEmpty() || size < LEAST || size > MOST) {
            throw new IllegalArgumentException("A tooltip's preview needs a block and is " + LEAST + " to " + MOST
                    + " pixels a side, not " + size);
        }
    }

    /** One block, {@code size} pixels a side. */
    public static TooltipPreview block(final BlockState state, final int size) {
        return new TooltipPreview(Map.of(BlockPos.ZERO, state), size);
    }
}
