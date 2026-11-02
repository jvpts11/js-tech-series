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
 * A block of the Rocket Hangar's loading dock, in the control room's outer wall: as the hangar forms it is bound to the
 * hangar's terminal, and what a chest, a hopper, a pipe or a network puts into it goes to the crane, which takes only
 * the blocks of the rocket it builds that it still waits for.
 */
public final class LoadingDockBlockEntity extends MultiblockPortBlockEntity {

    public LoadingDockBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.LOADING_DOCK_ENTITY.get(), pos, state, PortKind.ITEM_INPUT);
    }
}
