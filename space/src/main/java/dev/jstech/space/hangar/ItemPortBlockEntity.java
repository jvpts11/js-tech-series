/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.multiblock.MultiblockPortBlockEntity;
import dev.jstech.core.multiblock.PortKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Item Port set in a wall of a Rocket Hangar: as the hangar forms it is bound to the hangar's terminal, and what a
 * chest, a hopper, a pipe or a network puts into it goes to the crane, which takes only the blocks of the rocket it
 * builds that it still waits for; what the crane no longer wants comes back out of it.
 */
public final class ItemPortBlockEntity extends MultiblockPortBlockEntity {

    public ItemPortBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.ITEM_PORT_ENTITY.get(), pos, state, PortKind.ITEM_INPUT);
    }
}
