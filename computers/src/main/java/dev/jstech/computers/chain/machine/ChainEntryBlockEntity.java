/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An entry of a chain machine: the block through which items, a liquid, a gas or energy come in or go out, or the
 * peripheral port a station is linked by. Its machine's terminal binds it while the machine stands, and pipes and
 * cables against it reach the terminal through J's Core's ports.
 */
public class ChainEntryBlockEntity extends MultiblockPartBlockEntity {

    public ChainEntryBlockEntity(final BlockPos pos, final BlockState state) {
        super(ChainMachineBlocks.ENTRY_BE.get(), pos, state);
    }

    /** Lets go of its terminal, and draws itself again: the machine came apart. */
    @Override
    public void release() {
        super.release();
        final BlockState state = getBlockState();
        if (this.level != null && !this.level.isClientSide() && state.getBlock() instanceof ChainFormedBlock
                && state.getValue(ChainFormedBlock.FORMED)) {
            this.level.setBlock(this.worldPosition, state.setValue(ChainFormedBlock.FORMED, false), Block.UPDATE_ALL);
        }
    }
}
